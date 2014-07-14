WiperDog
=================
WiperDog is a monitoring solution which has groovy dsl as its monitoring definition language.  
This repository is WiperDog Server which is a part of WiperDog solution.

WiperDog is organized by 3 components.

1. WiperDog Server
2. WiperDog GUI(XWiki system and its application)
3. mongodb

To use wiperdog system, you have to install those 3 components.
We provide:  
* Wiperdog server (this assembly)  
* Wiperdog GUI as XWiki addon application.

Build this package from source
------------------------------------
You need maven 2.0 or later version installed to  build this package.
  
    $ git clone https://github.com/wiperdog/wiperdog.git
    $ cd wiperdog
    $ mvn package

You will get zip packed files of WiperDog server.

About other packages, please refer to individual README files.

Downloading binary packages.
-------------------------------------
You need 2 packages to install WiperDog.  

1.  [WiperDog Server assembly(for linux)](https://develop.wiperdog.org/jenkins/job/wiperdog-assembly-v0.2.5/lastSuccessfulBuild/artifact/target/wiperdog-0.2.5-unix.jar).  
or  [WiperDog Server assembly(for Windows)](https://develop.wiperdog.org/jenkins/job/wiperdog-assembly-v0.2.5/lastSuccessfulBuild/artifact/target/wiperdog-0.2.5-win.jar)   
(There are no significant difference between Linux version and Windows version except line endings.)
2.  [WiperDog XWiki Application](https://develop.wiperdog.org/jenkins/job/wiperdog-ui-v0.2.3/lastSuccessfulBuild/artifact/target/wiperdog-ui.xar).  

Installation
-------------------------------------

See INSTALL.md

Usage
-------------------------------------
You can access top menu of WiperDog GUI at  
http://localhost:8080/xwiki/bin/view/WiperDog/WebHome

Then check/set the configuration from "WiperDog Config" menu group.

Authors
-------------------------------------
See AUTHORS

Licence
-------------------------------------
WiperDog is provided with Apache 2.0 licence.

Change history
-------------------------------------
Version 0.2.5

* New/Update
    * Add command (genschedule, gendefaultparam, genjobcls, genjobinst, genjobparam, genpolicy, deployjob) to make WiperDog more useful
    * Add db connect interface to make connection to target database more flexible
    * Add function which installe WiperDog Server to remote host to "Install wiperdog from Xwiki" of Xwiki
    * Add genjob command to bin/ directory
    * Add -ni option for Wiperdog non-interactive installation
    * Add real-time chart function to "WiperDog for view data monitoring"
    * Modify expression of Xwiki from Jetty to Netty because structure of WiperDog server is modified
    * Modify from closing gmongo to closing object of MongoDBConnection class when connect from Xwiki to MongoDB
    * Modify jobrunner.groovy to use ListBundle.csv when execute OSGi
    * Modify MONITORING_TYPE and DB_TYPE of "Create new Job or update Job" to pluggable
    * Modify structure of WiperDog-UI source tree to make taking care of source tree easier
    * Modify to be able to install WiperDog interactively without -d option
    * Modify to create install.log when execute WiperDog installation
    * Modify to error message will appear on Xwiki when WiperDog server directory is not found
    * Modify to error message will be put on log file when there is a mistake on trg file or cls file.
    * Modify to ListBundle.csv supports 2 types of writing (mvn,groupid:artifactid:version,runlevel, and wrapmvn,groupid:artifactid:version,runlevel,) to get artifact from maven
    * Modify to policy file adjust to params file
    * Modify to save temporary data on memory which was saved on disk
    * Modify to use REST framework which works on Netty because servlet is redundant
    * Modify to use ScriptService of Xwiki to keep away from memory problem
    * Modify unnecessary WARNING, which comes from maven bug, doesn't come up
    * Modify view of job tree of "WiperDog for view data monitoring"
    * Modify WiperDog function from Jetty to Netty to decrease glue code, make source code easier, and make more affinitive with scripts on JVM
    * Omit JobNet function and writing job with xml function to optimize JobManager (JobClass function is left)
    * Add tool for packaging and deploying wiperdog's resource file (job files, ruby file...etc..) as a bundle to devide job from framework
* Fixed Bug
    * Change Xwiki configuration file (conf.params) from system file to Xwiki attachment for Windows authentication problem
    * Fix "each" to "for" on groovy of "WiperDog for view data monitoring" of Xwiki otherwise "PermGen Space" error appears when real-time chart is displayed
    * Fix default values of WiperDog installer because some of them were wrong
    * Fix DELETE button of "Create new Job or Update Job" otherwise it doesn't work
    * Fix OS monitoring job files because they had mistakes about arguments
    * Fix policy files' name and params files' name which belong to policy files
    * Fix the bug that cannot choose MONITORING_TYPE of "Create new Job or Update Job" with several kinds of browsers
    * Fix the bug that cannot send alert email when job name is same with other job
    * Fix the bug that drawing MySQL.Database_Area.Top30Database job fails
    * Fix the bug that drawing OS.Pagefiles_Usage_Windows job fails
    * Fix the bug that executing MySQL.Performance.QueryCache job fails
    * Fix the bug that executing OS.IOSystems_Linux job fails
    * Fix the bug that executing OS.IOSystems_Windows job fails
    * Fix the bug that executing OS.Memory_Swap_Linux job fails
    * Fix the bug that executing OS.Network_IO_Windows job fails
    * Fix the bug that executing OS.Process_Linux job fails
    * Fix the bug that Policy data cannot be updated on "Policy evaluation information" of Xwiki
    * Fix the bug that Policy files aren't seen from Xwiki
    * Fix the bug that Postgres.Database_Area.Tablespace_Free job didn't work except that target database is PostgreSQL 9.1
    * Fix the bug that result of OS.Pagefiles_Usage_Windows job becomes null
    * Fix the bug that result of OS.Process_Windows job becomes null
    * Fix the bug that some part of data cannot be seen on "WiperDog for view data monitoring" of Xwiki when SENDTYPE is Subtyped
    * Fix the bug that unnecessary space will be included when save jobs from "Create new Job or update Job" of Xwiki
    * Fix the bug that updating data in real time fails with no chart job on "WiperDog for view data monitoring" of Xwiki
    * Fix the bug that WiperDog installation fails when modify jar file's name before execute installation
    * Fix the function of "Create, update policy for job and run test them" of Xwiki otherwise too many options will be displayed
    * Fix the function to check jre/jdk otherwise WiperDog installation to a machine which only has jre fails
    * Fix the function to set WiperDog directory otherwise executing start/stop/status from Xwiki is unable
    * Fix the WiperDog-UI to be able to show OS monitoring jobs
    * Fix to be able to execute bundle_packaging.sh with relative path otherwise it fails
    * Fix to reuse connection to MongoDB otherwise "Java heap space" error occurs and job becomes not to work
    * Fix WiperDog installer otherwise error appears when execute installation to the path including space
    * Fix WiperDog installer’s registering service function for Windows machine otherwise WiperDog doesn’t start on Windows environment
    * Fix WiperDog installer's registering service function for Windows otherwise WiperDog cannot connect to target databases
* Known Problems
    * Need to set JAVA_HOME as system variables when execute installation to Windows environment

Version 0.1.0 (first release)

