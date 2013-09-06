package com.lilypepper.groovy;

import com.lilypepper.groovy.boot.FileSystem;
import com.lilypepper.groovy.boot.GoshClassLoader;
import com.lilypepper.groovy.boot.Bootstrap;

import groovy.lang.Binding;
import groovy.lang.GroovyClassLoader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import java.io.PrintWriter;

import java.lang.reflect.Field;

import java.math.BigInteger;

import java.security.MessageDigest;

import java.security.NoSuchAlgorithmException;

import java.util.List;

import junit.framework.TestCase;

import junit.framework.TestSuite;

import junit.textui.TestRunner;

import org.codehaus.groovy.control.CompilationUnit;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.runtime.InvokerHelper;


/**
 * Runs groovy scripts within a groovy class hierarchy, man.<p>
 * 
 * TODO: Fix these comments.<p>
 * 
 * {@code GroovyRunner} will 
 * <ol>
 *  <li>first run your custom {@code root.config.groovy} file at the root
 *  of your groovy project, man (if it finds one), </li>
 *  <li>then it will run your script.</li>
 * </ol>
 * The {@code root.config.groovy} file allows you to programmatically define the 
 * Java class files, JARs, and resources to load prior to starting your script.<p>
 * 
 * The {@code root.config.groovy} file at the root of your project is searched for 
 * in the following manner:
 * <ol>
 *  <li>First, look in the same folder as your script.</li>
 *  <li>If not there, look in the parent folder...</li>
 *  <li>If not there, look in the parent folder...</li>
 *  <li>and so on, up to the root folder.</li>
 * </ol>
 * If a {@code root.config.groovy} is found, that will be the package root for your
 * groovy scripts, man.  So you can use packages just like in Java.
 * If no {@code root.config.groovy} is found, set the package root to the same
 * folder your groovy script is in.<p>
 * 
 * <h3>Bindings</h3>
 * <table>
 *  <tr>
 *    <td>args</td>
 *    <td>
 *      The list of arguments from the command-line.  Note that this is an actual
 *      {@code java.util.List} and can be modified at runtime.  For services,
 *      this is always an empty list.
 *    </td>
 *  </tr>
 *  <tr>
 *    <td>startupScriptFile</td>
 *    <td>A {@code java.io.File} instance referring to the application script file</td>
 *  </tr>
 *  <tr>
 *    <td>service</td>
 *    <td>
 *      A {@code ServiceRunner} instance. For services, this gives you information
 *      about the service.  For command-line scripts, this allows you to test your services 
 *      by running them from the command line.  Note that the normal mechanism for stopping
 *      the service is disabled when run from the command line.
 *    </td>
 *  </tr>
 *  <tr>
 *    <td>systemClassLoader</td>
 *    <td>
 *      An instance of {@link com.lilypepper.groovy.boot.GoshClassLoader}, which is
 *      set as the system class loader (the lowest level class loader that a Java
 *      application can control).  Typically you will access this from the configuration
 *      file to load jars and modify the classpath.
 *    </td>
 *  </tr>
 *  <tr>
 *    <td>fs</td>
 *    <td>
 *      An instance of {@link com.lilypepper.groovy.boot.FileSystem}; usually used
 *      along with the <i>systemClassLoader</i> during configuration.  Gives information
 *      about various system paths, including the root path of the project.
 *    </td>
 *  </tr>
 * </table>
 */
public class Groovy
{
  private static final String CONFIG_FOLDER_NAME = "GROOVY-INF";
  private static final String CONFIG_CLASS_NAME = "config";
  
  private static final GoshClassLoader   systemClassLoader = (GoshClassLoader)Groovy.class.getClassLoader().getSystemClassLoader();
  private static final GroovyClassLoader groovyClassLoader = new GroovyClassLoader(systemClassLoader);
  
  static
  {
    StackTraceUtils2.addClassTest(StackTraceSupport.getFilter());
  }
  
  /**
   * Entry point that allows additional bindings to be defined.
   * @param scriptFile The script file to run.
   * @param args A list of arguments from the command line.
   */
  public static void run(File scriptFile, List<String> args) throws Throwable
  {
    try
    {
      Thread.currentThread().setContextClassLoader(systemClassLoader);
  
      //System.err.println("Groovy version: " + InvokerHelper.getVersion());
  
      //OBSOLETE: This is the folder where the GOSH batch file is located.
      //File groovyLocalFolder = new File(System.getProperty("com.lilypepper.groovy.localfolder")).getCanonicalFile();
      
      File configFolder = findConfigFolder(scriptFile);
      
      //Set up the root folder.
      //If the user has defined a local configuration file, use that folder as the root.
      //Otherwise, punt, and use the folder where the target groovy script is located, man.
      File groovyRoot = scriptFile.getParentFile();
      File configFile = null;
      if(configFolder != null)
      {
        //First, find the local (root) configuration file.
        configFile = new File(configFolder, CONFIG_CLASS_NAME + ".groovy"); //findConfigFile(scriptFile);
        if(!configFile.isFile()) configFile = null;
        
        groovyRoot = configFolder.getParentFile();
        
        if(new File(configFolder, "lib").isDirectory())
        {
          systemClassLoader.addJars(new File(configFolder, "lib"));
        }
        
        if(new File(configFolder, "classes").isDirectory())
        {
          systemClassLoader.addClasspath(new File(configFolder, "classes"));
        }
        
        if(new File(configFolder, "bin").isDirectory())
        {
          FileSystem.addJavaLibraryPath(new File(configFolder, "bin"));
        }
      }
      
      
      FileSystem.rootPath = groovyRoot;
      FileSystem.startScriptPath = scriptFile.getCanonicalFile();
      
      groovyClassLoader.addURL(groovyRoot.toURI().toURL());
      Thread.currentThread().setContextClassLoader(groovyClassLoader);
      
      Binding binding = new Binding();
      
      binding.setVariable("systemClassLoader", systemClassLoader);
      binding.setVariable("groovyClassLoader", groovyClassLoader);
      binding.setVariable("scriptFile", scriptFile.getCanonicalFile());
      binding.setVariable("service", new ServiceRunner());
      binding.setVariable("args", args);
      binding.setVariable("fs", new FileSystem());
      
      if(configFile != null)
      {
        try
        {
          //This bit of magic compiles the config.groovy file to config.class as needed.  
          //If not needed, we save about a half second on startup.
          //Well, it's a work in progress.
          //This can be replaced by a call to runScript() if there is a problem with the 
          //design.
          File tempFolder = new File(new File(System.getProperty("java.io.tmpdir"), "groovy-config"), getSecureHash(/*InvokerHelper.getVersion()*/
          groovy.lang.GroovySystem.getVersion() + "|" + groovyRoot.getCanonicalPath()));
          File configClassFile = new File(tempFolder, CONFIG_CLASS_NAME + ".class");
          if(!configClassFile.isFile() || configClassFile.lastModified() != configFile.lastModified())
          {
            delete(tempFolder);
            
            CompilerConfiguration config = new CompilerConfiguration();
            config.setTargetDirectory(tempFolder);
            config.setOutput(new PrintWriter(System.err));
            config.setDebug(false);
            config.setVerbose(false);
            config.setSourceEncoding("US-ASCII");
            
            CompilationUnit unit = new CompilationUnit(config, null, groovyClassLoader);
            unit.addSource(configFile);
            unit.compile();
            
            configClassFile.setLastModified(configFile.lastModified());
          }
          
          GroovyClassLoader configClassLoader = new GroovyClassLoader(systemClassLoader);
          configClassLoader.addURL(tempFolder.toURI().toURL());
          
          Class scriptClass = configClassLoader.loadClass(CONFIG_CLASS_NAME, true, true, true);
          if(groovy.lang.Script.class.isAssignableFrom(scriptClass))
          {
            groovy.lang.Script script = (groovy.lang.Script)scriptClass.newInstance();
            script.setBinding(binding);
            
            script.run();
          }
          else
          {
            throw new NotAGroovyScriptException("Class is not a groovy script: " + scriptClass.getName());
          }
        }
        catch(Throwable e)
        {
          throw new ConfigurationException("'" + configFile.getCanonicalPath() + "'", e);
        }
      }
      
      //runScript(scriptFile, binding);
      Class scriptClass = groovyClassLoader.parseClass(scriptFile.getCanonicalFile());
      if(groovy.lang.Script.class.isAssignableFrom(scriptClass))
      {
        groovy.lang.Script script = (groovy.lang.Script)scriptClass.newInstance();
        script.setBinding(binding);
      
        script.run();
      }
      else if(TestCase.class.isAssignableFrom(scriptClass))
      {
        TestSuite testSuite = new TestSuite(scriptClass);
        TestRunner.run(testSuite);
      }
      //else if(TestSuite.class.isAssignableFrom(scriptClass))
      //{
      //  TestSuite testSuite = (TestSuite)scriptClass.newInstance();
      //  TestRunner.run(testSuite);
      //}
      else
      {
        throw new NotAGroovyScriptException("Class is not a groovy script: " + scriptClass.getName());
      }
    }
    catch(Throwable t)
    {
      throw StackTraceUtils2.deepSanitize(t);   
    }
  }
  
  /**
   * Searches from the folder of the target file upwards to the root folder until 
   * it finds a groovy folder.  
   * @param targetFile The groovy file that is going to be run.
   * @return The root.config.groovy file, or {@code null} if not found.
   * @throws IOException An IOException occurred.
   */
  private static File findConfigFolder(File targetFile) throws IOException
  {
    if(!targetFile.isFile())
    {
      throw new FileNotFoundException(targetFile.getPath());
    }
    
    targetFile = targetFile.getCanonicalFile();
    
    File parentPath = targetFile.getParentFile();   
    do
    {
      File configFolder = new File(parentPath, CONFIG_FOLDER_NAME);
      if(configFolder.isDirectory())
      {
        return configFolder;
      }
      else
      {
        parentPath = parentPath.getParentFile();
      }
    }
    while(parentPath != null);
    
    return null;
  }
  
  
  /**
   * An exception occurred in the groovy configuration script, man.
   */
  public static class ConfigurationException extends Exception
  {
    private ConfigurationException(String message, Throwable t)
    {
      super(message, t);
    }
  }
  
  
  public static class NotAGroovyScriptException extends Exception
  {
    private NotAGroovyScriptException(String message)
    {
      super(message);
    }
  }
  
  private static String getSecureHash(String text) throws NoSuchAlgorithmException
  {
    byte[] data = text.getBytes();
    MessageDigest md = MessageDigest.getInstance("SHA-1");
    md.update(data);
    BigInteger digest = new BigInteger(md.digest()).and(new BigInteger("ffffFFFFffffFFFFffffFFFFffffFFFFffffFFFF",16));
    return digest.toString(36);
  }
  
  private static void delete(File file)
  {
    if(file == null || !file.exists())
      return;
      
    for(File f : file.listFiles())
    {
      if(f.isDirectory())
        delete(f);
      else
        f.delete();
    }
    file.delete();
  }
}