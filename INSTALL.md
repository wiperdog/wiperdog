WiperDog is not monolithic software, it needs some required software installed.

Before Installing WiperDog solution, check the prerequisites:  
1.  Because we are using MongoDB, You need using 64 bit Operating systems.  
2.  Java runtime is required to be installed.  
    Valid versions are:  
     - jre 1.6_30 or greater  
     - jre 7 may works also  
3.  Be aware about firewall settings, if you want to access WiperDog's Web GUI from another machine, port 13111 should not be blocked by firewall.

On the Windows OS, JRE must be 32bits version(this restriction is from the service controlling software we are using).

Download and Install MongoDB
---------------------------------
Visit [mongodb site](http://www.mongodb.org/), then install mongodb to your system(if you dont have it yet)  
We require version 2.2 or above.  

You may be able to use 'yum', 'apt-get' or some other package manager to install mongodb if you are using such Linux distributions.

Download and Install XWiki
---------------------------------
Visit [XWiki site](http://www.xwiki.org), then go to "Download&play" -> "XWiki Enterprise".  
Then download XWiki package.  
We recommend you to download "Standalone installer" package.  

If you choose "Standalone installer", You just need to unpack zipped package.  
After then, XWiki can be started with "startup scripts" placed on the top of unzipped directory.  

XWiki uses IP port 8080 by default, If you are already using that port, please change it by modifying port number parameter written in configuration file(jetty/etc/jetty.xml if you choose "Standalone installer").  

Try start up XWiki, then access http://localhost:8080/xwiki, you will see the XWiki top screen.
 
Now you successfully started up XWiki, I recommend you to do something more.
Let's tune JavaVM which hosting XWiki, otherwise your WiperDog GUI may have some minor problem(ex; WiperDog image can't be displayed).

Please add JVM option:  
  
        -Djava.awt.headless=true -XX:+CMSClassUnloadingEnabled -XX:+UseConcMarkSweepGC

 to your JVM settings.  
 more concretely, if you choose "Standalone installer", JVM Settings are located in the start_xwiki.sh.


Download and Install WiperDog server
---------------------------------
WiperDog server is the control center of WiperDog solution.
You just need to download and unpack to where you like.  
Package(WiperDog server assembly) is located [here](http://develop.wiperdog.org/jenkins/job/wiperdog-assembly-v0.1.0/lastSuccessfulBuild/artifact/target/wiperdog-0.1.0-unix.zip)  
or [here(for Windows)](http://develop.wiperdog.org/jenkins/job/wiperdog-assembly-v0.1.0/lastSuccessfulBuild/artifact/target/wiperdog-0.1.0-win.zip)  
After unpacking the WiperDog assembly, You need to do some 'dirty work'.  
1.  You need  to edit bin/wiperdog file

    ...
    . /etc/rc.d/init.d/functions  
    
    WIPERDOGHOME=/home/luvina/wiperdog
    ...
Change the value of WIPERDOGHOME above to the directory you unpacked WiperDog assembly.  
2.  You need to edit var/conf/default.params like:

     ...
      ],
     dest: [ [ file: "stdout" ] ],
     datadirectory: [
     ...
The value of "dest: " is what you need to change, for this version, just change like this:  

    dest: [ [ http: "localhost:13111/spool/tfm_test" ] ],  

Now you can try to start WiperDog server as:   

    $ bin/startWiperdog.sh

or(for Windows)

    extracteddir> bin\startWiperdog

Please check you don't see kinda errors.

You can finish startup test by pressing CTRL-C.
### For Linux OS(Redhat/CentOS), registering Wiperdog as service
bin/wiperdog file you modified above is used by WiperDog GUI, and also can be used as service run command file.  
After entering root account do as following  

    # cd bin
    # chkconfig --add wiperdog
    # chkconfig --level 345 wiperdog on

### For Windows OS, registering Windows Service
This is just for Windows OS user.
If you choosed Windows OS to install WiperDog, You should register WiperDog server as Windows Service or you may not able to control WiperDog Server from XWiki GUI.
You can register WiperDog Server as Windows Service after startup test successflly finished.  
1. Open command prompt, then set JAVA_HOME environment variable to where your jre installed.  
   Be careful, never quote the path.  
2. Go to the directory service\javaservice then invoke   

    service\javaservice> create_wiperdog_service DIRECTORY_WHERE_WIPERDOG_SERVER_PLACED
Please replace DIRECTORY\_WHERE\_WIPERDOG\_SERVER\_PLACED to the directory path where the WiperDog server placed.  
Again, You need to using 32bit version of jre to successfully register Windows Service.


Download and Install WiperDog XWiki app
---------------------------------
You need to setup WiperDog GUI to use WiperDog.  

1.  Download [wiperdog-xwiki-app](http://develop.wiperdog.org/jenkins/job/wiperdog-xwiki-app-v0.1.0/lastSuccessfulBuild/artifact/target/wiperdog-xwiki-app-0.1.0.xar).
2.  Start up XWiki, then log in to XWiki using Admin account.  
    If you havn't changed the password yet, it is 'admin'.
3.  Go to the 'Administer wiki' menu from the top 'Wiki' menu.
4.  Go to 'Import' page then upload the file you just downloaded at step 1.  
    You don't need to touch any import options, just import.
5.  You will see "WiperDog" & "WiperDogLib" space on the top page of XWiki, Please enter to "WiperDog" space.
6.  You need to do additional configuration by hand.  
    Go to webapp/xwiki/resources directory under the directory where XWiki files have been extracted, then edit(create) conf.params file as:  
    (change YOUR\_IP\_ADDRESS to your real IP address where the WiperDog server placed,  
     and also, replace THE\_PATH\_WHERE\_WIPERDOG\_SERVER\_PLACED to the full path where the WiperDog server is placed)

        [
          "servlet_config": [
            "dbinfo": [
              "servlet": "http://localhost:13111/DBConfigServlet",
              "desc": "Using for WiperDog/DBConfiguration page"
            ],
            "dbcommon": [
              "servlet": "http://localhost:13111/DBCommonConfig",
              "desc": "Using for WiperDog/JobDBConnectionConfiguration page "
            ],
            "jobdoc": [
              "servlet": "http://localhost:13111/JobDocInfoServlet",
              "desc": "Using for WiperDog/JobDoc page"
            ],
            "console": [
              "servlet": "http://localhost:13111/wiperdog/echo",
              "desc": "Using for WiperDog/ConsoleManager page"
            ],
            "logfileinfo": [
              "servlet": "http://localhost:13111/LogFileInfoServlet",
              "desc": "Using for WiperDog/LogFileInfo page"
            ],
            "JobDeclared": [
              "servlet": "http://localhost:13111/JobDeclared",
              "desc": "Using for WiperDog/JobConfiguration page"
            ],
            "testjob": [
              "servlet": "http://localhost:13111/TestJobServlet",
              "desc": "Using for Using for WiperDog/TestJob  page"
            ]
          ],
          "wiperdog_path":"THE_PATH_WHERE_WIPERDOG_SERVER_PLACED"
        ]


That's all.
You cat re-visit WiperDog space, then start your tour around WiperDog.  
Enjoy.

