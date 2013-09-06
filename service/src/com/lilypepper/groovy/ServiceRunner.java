package com.lilypepper.groovy;

import com.lilypepper.groovy.boot.Runner;

import java.io.File;
import java.io.FileNotFoundException;

import java.util.LinkedList;

import javax.swing.event.EventListenerList;


/**
 * Called to run Groovy scripts as a WinNT service, man.
 * @see com.lilypepper.groovy.Groovy
 */
public class ServiceRunner implements Runner
{
  private static volatile boolean shutdownRequested = false;
  
  /**
   * Called by {@code JavaService} to process service events.
   * @param args Arguments passed from {@code JavaService}.
   * @throws Throwable An exception occurred.
   */
  public void run(String[] args) throws Throwable
  {
    if(args[0].equals("start"))
    {
      try
      {
        if(args.length <= 1)
        {
          throw new FileNotFoundException("Groovy source file not specified, man.  That's not groovy.");
        }
        File targetFile = new File(args[1]);
        
        Groovy.run(targetFile, new LinkedList<String>());
      }
      catch(Throwable t)
      {
        t.printStackTrace(System.err);
        Thread.sleep(5000); 
        System.exit(1);
      }
    }
    else if(args[0].equals("stop"))
    {
      shutdownRequested = true; 
      
      synchronized(shutdownEvents)
      {
        for(ShutdownEventListener listener : shutdownEvents.getListeners(ShutdownEventListener.class))
        {
          try
          {
            listener.shutdownRequested();
          }
          catch(Throwable t)
          {
            t.printStackTrace();
          }
        }
      }
    }
  }
  
  private static EventListenerList shutdownEvents = new EventListenerList();
  
  /**
   * Returns {@code true} if the service has been requested to shut down.
   * @return {@code true} if the service has been requested to shut down.
   */
  public boolean getShutdownRequested()
  {
    return shutdownRequested;
  }
  
  /**
   * Special implementation of {@code sleep} that is interruptable by a 
   * shutdown request - without throwing exceptions.
   * @param interval The interval to sleep, in milliseconds.
   * @throws InterruptedException The thread was interrupted.
   */
  public void sleep(long interval) throws InterruptedException
  {
    long nextWakeTime = System.currentTimeMillis() + interval;
    while(true)
    {
      long timeTilWake = nextWakeTime - System.currentTimeMillis();
      
      if(shutdownRequested || timeTilWake <= 0)
      {
        break;
      }
      
      long sleepTime = 100;
      if(timeTilWake < sleepTime)
      {
        sleepTime = timeTilWake;
      }
      Thread.sleep(sleepTime);      
    }
  }
  
  /**
   * Adds a listener for the shutdown-requested event.
   * @param listener Listener object.
   */
  public void addShutdownEventListener(ShutdownEventListener listener)
  {
    synchronized(shutdownEvents)
    {
      shutdownEvents.add(ShutdownEventListener.class, listener);
    }
  }
  
  /**
   * Removes a listener.
   * @param listener Listener object.
   */
  public void removeShutdownListener(ShutdownEventListener listener)
  {
    synchronized(shutdownEvents)
    {
      shutdownEvents.remove(ShutdownEventListener.class, listener);
    }
  }
}