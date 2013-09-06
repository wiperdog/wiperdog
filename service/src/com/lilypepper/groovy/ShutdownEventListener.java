package com.lilypepper.groovy;

import java.util.EventListener;

/**
 * Listener for the shutdown-requested event.
 */
public interface ShutdownEventListener extends EventListener
{
  /**
   * Called when the service is requested to shutdown by the OS.
   * @throws Throwable An exception occurred.
   */
  void shutdownRequested() throws Throwable;
}