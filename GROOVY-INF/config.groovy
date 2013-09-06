/**
 * This groovy script runs before the groovy script you specified, man.
 * Like, its purpose is to allow you to load up all the jars and classes
 * your script is going to need in order to run, man.
 *
 * Oh, wow, you have to use the 'classpath' 
 * (instance of com.lilypepper.groovy.ClassPath) to add all your jars
 * and stuff, and that gets added to the ClassLoader before your script 
 * runs.  Fixes all the bad mojo with ClassLoaders and stuff, man.
 * Speaking of jars, are you hungry?  
 *
 * How groovy it is to use groovy to configure groovy, man?
 *
 * Oh, I almost forgot...you have to include this script in the root
 * of your groovy class hierarchy, man.  That way, gosh knows where
 * to start looking for related classes (since groovy uses the same type
 * of file-based package hierarchy as Java).  Programming by convention, man.
 */

//THE NEXT LINE WILL RECURSIVELY FIND ALL JARS IN THE SPECIFIED PATH AND LOAD THEM.
//classpath.loadJars "C:/my-java-libraries"

println "****************************"
println "* config.groovy            *"
println "* Custom config is GROOVY! *"
println "****************************"
println()