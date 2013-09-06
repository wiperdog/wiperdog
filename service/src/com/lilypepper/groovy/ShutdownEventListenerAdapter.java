package com.lilypepper.groovy;

/**
 * Empty implementation of the {@link ShutdownEventListener}.
 */
public abstract class ShutdownEventListenerAdapter implements ShutdownEventListener
{
  /**
   * Called when the service is requested to shutdown by the OS.
   * @throws Throwable An exception occurred.
   */
  public void shutdownRequested() throws Throwable
  {
  }
}