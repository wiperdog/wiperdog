package com.lilypepper.groovy.boot;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.IOException;

import java.net.URL;
import java.net.URLClassLoader;

/**
 * Replacement for the system classloader.
 */
public class GoshClassLoader extends URLClassLoader
{
  /**
   * Called by Java to initialize this classloader on boot.
   * @param classLoader The parent classloader (provided by Java).
   */
  public GoshClassLoader(ClassLoader classLoader)
  {
    super(new URL[0], classLoader);
  }
  
  /**
   * Adds all JAR files in the folder to the classpath.  The search is recursive.
   * @param folder The folder to search.
   * @throws IOException An IOException occurred.
   * @see com.lilypepper.groovy.boot.FileSystem
   */
  public void addJars(String folder) throws IOException
  {
    addJars(new File(folder));
  }
  
  /**
   * Adds all JAR files in the folder to the classpath.  The search is recursive.
   * @param folder The folder to search.
   * @throws IOException An IOException occurred.
   * @see com.lilypepper.groovy.boot.FileSystem
   */
  public void addJars(File folder) throws IOException
  {
    folder = folder.getCanonicalFile();
    
    if(!folder.isDirectory())
    {
      throw new FileNotFoundException(folder.getPath());
    }
    
    for(File file : folder.listFiles(
        new FileFilter()
        {
          public boolean accept(File f)
          {
            return f.isDirectory() || (f.isFile() && f.getName().toLowerCase().endsWith(".jar"));
          }
        }
    ))
    {
      if(file.isDirectory())
      {
        addJars(file);
      }
      else
      {
        addURL(file.toURI().toURL());
      }
    }
  }
  
  /**
   * Adds a single JAR file to the classpath.
   * @param jarFile JAR file to add.
   * @throws IOException An IOException occurred.
   * @see com.lilypepper.groovy.boot.FileSystem
   */
  public void addJar(String jarFile) throws IOException
  {
    addJar(new File(jarFile));
  }
  
  /**
   * Adds a single JAR file to the classpath.
   * @param jarFile JAR file to add.
   * @throws IOException An IOException occurred.
   * @see com.lilypepper.groovy.boot.FileSystem
   */
  public void addJar(File jarFile) throws IOException
  {
    jarFile = jarFile.getCanonicalFile();
    
    if(!jarFile.isFile())
    {
      throw new FileNotFoundException(jarFile.getPath());
    }
    else if(!jarFile.getName().toLowerCase().endsWith(".jar"))
    {
      throw new IOException("File '" + jarFile.getPath() + "' is not a JAR file.");
    }
    
    addURL(jarFile.getCanonicalFile().toURI().toURL());
  }
  
  /**
   * Adds a folder path to the classpath. The folder is the root for {@code .class} files, 
   * properties files, and other resources.   
   * @param classpath Path to the folder to add.
   * @throws IOException An IOException occurred.
   * @see com.lilypepper.groovy.boot.FileSystem
   */
  public void addClasspath(String classpath) throws IOException
  {
    addClasspath(new File(classpath));
  }
  
  /**
   * Adds a folder path to the classpath. The folder is the root for {@code .class} files, 
   * properties files, and other resources.
   * @param classpath Path to the folder to add.
   * @throws IOException An IOException occurred.
   * @see com.lilypepper.groovy.boot.FileSystem
   */
  public void addClasspath(File classpath) throws IOException
  {
    classpath = classpath.getCanonicalFile();
    
    if(!classpath.isDirectory())
    {
      throw new FileNotFoundException(classpath.getPath());
    }
    
    addURL(classpath.getCanonicalFile().toURI().toURL());
  }
  
}