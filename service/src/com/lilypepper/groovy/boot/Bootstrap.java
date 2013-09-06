package com.lilypepper.groovy.boot;

import java.io.File;

/**
 * The bootstrap class for Gosh, this is the single entry point.
 * This will dynamically load the initial Gosh and Groovy library files, then 
 * execute the defined {@link Runner}.
 * 
 * <h3>Gosh-Specific Properties</h3>
 * <table>
 *  <tr>
 *    <td>com.lilypepper.groovy.localfolder</td>
 *    <td>The folder where the {@code gosh.bat} file is executing.</td>
 *  </tr>
 *  <tr>
 *    <td>com.lilypepper.groovy.runner</td>
 *    <td>The {@link Runner} that will be dynamically loaded and run.</td>
 *  </tr>
 * </table>
 */
public class Bootstrap
{
  private static volatile boolean initialized = false;
  
  /**
   * Records the earliest time Java ran code in the project.
   */
  public static long startTime = System.currentTimeMillis();

  /**
   * The entry point for Gosh.  This method takes care of initializing the system
   * classloader (the first time), setting the context classloader, and initially 
   * loading in all the jar library files in {@code ./gosh/lib}. 
   * @param args The arguments to pass to the {@link Runner}.
   * @throws Throwable An exception occurred.
   */
  public static void main(String[] args) throws Throwable
  {
    //System.out.println("Bootstrap started. " + (System.currentTimeMillis() - startTime));
  
    File localFolder = new File(System.getProperty("com.lilypepper.groovy.localfolder")).getCanonicalFile();
    //System.err.println("com.lilypepper.groovy.localfolder=\"" + localFolder.getPath() + "\"");
    
    String targetRunner = System.getProperty("com.lilypepper.groovy.runner");
    
    GoshClassLoader classLoader = (GoshClassLoader)Bootstrap.class.getClassLoader().getSystemClassLoader();
    Thread.currentThread().setContextClassLoader(classLoader);
    
    if(!initialized)
    {
      classLoader.addJars(new File(localFolder, "lib"));
      FileSystem.addJavaLibraryPath(new File(localFolder, "bin"));
      //System.err.println("java.library.path=\"" + System.getProperty("java.library.path") + "\"");
      initialized = true;
    }

    Class runnerClass = classLoader.loadClass(targetRunner);
    Runner runner = (Runner)runnerClass.newInstance();
    runner.run(args);
  }
}