package com.lilypepper.groovy

class StackTraceSupport
{
  static final Closure filter = 
  {className ->
    if(className.startsWith('com.lilypepper.groovy'))
      return false;
    else
      return null;
  }
}