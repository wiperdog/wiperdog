package com.lilypepper.groovy;

import com.lilypepper.groovy.boot.Runner;

import java.io.File;
import java.io.FileNotFoundException;

import java.util.Arrays;
import java.util.LinkedList;


/**
 * Called by {@link com.lilypepper.groovy.boot.Bootstrap} to run Groovy scripts 
 * from the command-line, man.
 * @see com.lilypepper.groovy.Groovy
 */
public class CommandRunner implements Runner
{
  /**
   * Main entry point for running groovy scripts run from a command line, man.
   * 
   * @param args First param is groovy script, man; the rest are passed as arguments to the script.
   * @throws Throwable An exception occurred.
   */
  public void run(String[] args) throws Throwable
  {
      if(args.length == 0)
      {
        //Note that this can only happen if you are running GOSH.BAT directly.
        throw new FileNotFoundException("Groovy source file not specified, man.  That's not groovy.");
      }
  
      //This is the target groovy script.
      File targetFile = new File(args[0]);
      
      LinkedList<String> arguments = new LinkedList<String>(Arrays.asList(args));
      arguments.removeFirst();
  
      Groovy.run(targetFile, arguments);
  }
}