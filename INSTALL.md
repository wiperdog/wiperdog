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
After downloaded packages above, you can simply invoke it as installer, the usage is:

    Usage:
                 java -jar [Installer Jar] -d [INSTALL_PATH>]
                 or
                 java -jar [Installer Jar] -d [INSTALL_PATH] -j [nettyport] -m [mongodb host] -p [mongodb port] -n [mongodb database name] -u [mongodb user name] -pw [mongodb password] -mp [mail policy] -s [yes/no install as OS service]

the "-d" option can be omitted.  
The installation process is like as following...   

     $ java -jar wiperdog-0.2.4-unix.jar
     You omitted to specify WIPERDOG HOME.
     Are you sure to install wiperdog at [current working directory]/wiperdog-0.2.4 ? [y/n] :
     y <--- you type "y" if it is OK to install at the directory specified above.
     Wiperdog will be install to directory: /home/kurohara/work/wiperdog_test/wiperdog/target/wiperdog-0.2.4
     Wiperdog installer, unzip to file : /home/kurohara/work/wiperdog_test/wiperdog/target/wiperdog-0.2.4/lib/java/bundle.wrap/ojdbc5.jar
     ...
     ...
     ...
     Wiperdog installer, unzip to file : /home/kurohara/work/wiperdog_test/wiperdog/target/wiperdog-0.2.4/lib/java/ext/org.apache.felix.framework-4.2.1.jar
     Self-extracting done!
     ----------------------------------------------------------------
     ------------------    INSTALLATION     -------------------------
     ----------------------------------------------------------------
     Welcome to the installation of Wiperdog multi purposes monitoring system - Version: 1.0.1
     
     Wiperdog is multi purpose monitoring DBMS system writing in Groovy.
     Wiperdog has some major components as followings:
     1. MongoDB is used as a storage DBMS for storing monitoring result data.
     Mongodb is good for large scale system with high concurrence access, it also provide a very good performance
     2. XWiki is used as a front-end user GUI. It is easy customizable and configurable
     3. Groovy monitoring framework, has built by Insight-tech, is well-structure, easy for maintenance, scalable and configurable
     Thank you for using wiperdog, please follow screen construction to continue the installation
     
     Getting input parameters for pre-configured wiperdog, press any key to continue...
     Please input Netty Port(default set to 13111):
     <---- you can type just ENTER.
     Please input database server (Mongodb) IP address (default set to 127.0.0.1):
     <---- you can type just ENTER.
     Please input database server port, Mongodb port (default set to 27017):
     <---- you can type just ENTER.
     Please input database name (default set to wiperdog):
     <---- you can type just ENTER.
     Please input database server user name, (Mongodb user name, default set to empty):
     <---- you can type just ENTER.
     Please input database server password, (Mongodb password, default set to empty):
     <---- you can type just ENTER.
     Please input mail send data policy, Mail Policy (default set to testmail@gmail.com):
     <---- you can type just ENTER.
     Do you want to install wiperdog as system service, default set to 'yes' (type yes|no), enter for default value:
     no <-- type 'no' if you don't want to setup WiperDog server as System service.(or if you simply don't have the administrater permission)
     
     
     Please CONFIRM The following configuration are correct:
     Wiperdog Home:/home/kurohara/work/wiperdog_test/wiperdog/target/wiperdog-0.2.4
     Server Port:13111
     Database address:127.0.0.1
     Database port:27017
     Database name:wiperdog
     User name:
     Password:
     Mail Policy:testmail@gmail.com
     Install as OS service:no
     
     
     Your input are correct(Y|y|N|n):
     y <-- type 'y' if all is OK.

     
Now you can try to start WiperDog server as:   

    $ bin/startWiperdog.sh

or(for Windows)

    extracteddir> bin\startWiperdog

Please check you don't see kinda errors.

You can finish startup test by pressing CTRL-C.

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
You can re-visit WiperDog space, then start your tour around WiperDog.  
Enjoy.

