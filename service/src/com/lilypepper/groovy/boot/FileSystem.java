package com.lilypepper.groovy.boot;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import java.lang.reflect.Field;

/**
 * Methods for working with paths relative to your groovy project, man.
 */
public class FileSystem
{
  /**
   * The groovy project root path, man.
   */
  public static File rootPath = null;
  
  /** 
   * The path to your groovy start script, man.
   */
  public static File startScriptPath = null;
  
  /**
   * Makes your file relative to the groovy root, man, rather than the current folder.
   * If the file you pass in is already an absolute path, it doesn't get changed.
   * If it is a relative path, you get a file that is relative to the folder
   * where the {@code root.config.groovy} file is located, which is also known as the groovy root.
   * @param file Represents a path or file, existing or not.
   * @return File relative to the groovy root, man.
   * @throws IOException An IOException occurred.
   * @see #fromPath
   */
  public File fromRootPath(File file) throws IOException
  {
    return fromPath(rootPath, file);
  }
  
  /**
   * Makes your file relative to the given start path.
   * If the file you pass in is already an absolute path, it doesn't get changed.
   * If it is a relative path, you get a file that is relative to the folder
   * you specified.
   * @param startPath An existing folder.
   * @param relativePath Represents a relative path or file, existing or not.
   * @return File relative to the given start path.
   * @throws IOException An IOException occurred.
   */
  public File fromPath(File startPath, File relativePath) throws IOException
  {
    if(relativePath.isAbsolute())
    {
      return relativePath;
    }
    else
    {
      if(!startPath.isDirectory())
      {
        throw new IOException("'" + startPath.getPath() + "' is not a valid starting path.");
      }
      return new File(startPath.getCanonicalFile(), relativePath.getPath()).getCanonicalFile();
    }
  }

  /**
   * Makes your file relative to the groovy root, man, rather than the current folder.
   * If the file you pass in is already an absolute path, it doesn't get changed.
   * If it is a relative path, you get a file that is relative to the folder
   * where the {@code root.config.groovy} file is located, which is also known as the groovy root.
   * @param filePath Represents a path or file, existing or not.
   * @return File relative to the groovy root, man.
   * @throws IOException An IOException occurred.
   * @see #fromPath
   */
  public File fromRootPath(String filePath) throws IOException
  {
    return fromRootPath(new File(filePath));
  }
  
  /**
   * Returns the absolute path to the current groovy root, man.
   * The groovy root is the folder where your custom {@code root.config.groovy}
   * file lives. 
   * @return The absolute path to the current groovy root, man.
   * @throws IOException An IOException occurred.
   */
  public File getRootPath() throws IOException
  {
    return rootPath.getCanonicalFile();
  }
  
  /**
   * Adds a path to the Java Library Path.
   * @param folder
   * @throws IOException
   */
  public static void addJavaLibraryPath(File folder) throws IOException
  {
    if(!folder.isDirectory()) throw new FileNotFoundException(folder.getPath());
    addJavaLibraryPathDirHack(folder.getCanonicalPath());
  }
  
  /**
   * http://forum.java.sun.com/thread.jspa?threadID=707176&messageID=4096872
   * @param s
   * @throws IOException
   */
  private static void addJavaLibraryPathDirHack(String s) throws IOException 
  {
    try 
    {
      Field field = ClassLoader.class.getDeclaredField("usr_paths");
      field.setAccessible(true);
      String[] paths = (String[])field.get(null);
      for (int i = 0; i < paths.length; i++) 
      {
        if (s.equals(paths[i])) 
        {
          return;
        }
      }
      String[] tmp = new String[paths.length+1];
      System.arraycopy(paths,0,tmp,1,paths.length);
      tmp[0] = s;
      field.set(null,tmp);
      
      System.setProperty("java.library.path", s + ";" + System.getProperty("java.library.path"));
    } 
    catch (IllegalAccessException e) 
    {
      throw new IOException("Failed to get permissions to set library path");
    } 
    catch (NoSuchFieldException e) 
    {
      throw new IOException("Failed to get field handle to set library path");
    }
  }
}