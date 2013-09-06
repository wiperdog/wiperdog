package com.lilypepper.groovy.boot;

/**
 * Identifies a runnable class.  Classes in the {@code gosh.jar} file implement this interface,
 * and it allows {@link Bootstrap} to call them dynamically.
 */
public interface Runner
{
  /**
   * Execute the call to the runner.
   * @param args Arguments passed to the runner.
   * @throws Throwable An exception occurred.
   */
  void run(String[] args) throws Throwable;
}