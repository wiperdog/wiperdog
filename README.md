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

Download binary packages.
-------------------------------------
You need 2 packages to install WiperDog.  

1.  [WiperDog Server assembly(for linux)](http://demo.wiperdog.org/jenkins/job/wiperdog-assembly/lastSuccessfulBuild/artifact/target/wiperdog-0.1.0-SNAPSHOT-unix.zip).  
or  [WiperDog Server assembly(for Windows)](http://demo.wiperdog.org/jenkins/job/wiperdog-assembly/lastSuccessfulBuild/artifact/target/wiperdog-0.1.0-SNAPSHOT-win.zip)   
(There are no significant difference between Linux version and Windows version except line endings.)
2.  [WiperDog XWiki Application](http://develop.wiperdog.org/jenkins/job/wiperdog-xwiki-app/lastSuccessfulBuild/artifact/target/wiperdog-xwiki-app-0.1.0.xar).  

Installation
-------------------------------------
This version of WiperDog requires several steps of installation. 
### mongodb installation
Mongodb is required as main data store of Wiperdog.  
Get mongodb distribution from its [site](http://www.mongodb.org/).  
Then follow the instruction on that site.

### XWiki installation
Wiperdog is using Xwiki as its front end engine.
Please get latest version of "XWiki Enterprise" from   
[XWiki Enterprise Downloads page](http://www.xwiki.org/xwiki/bin/view/Main/Download)   
then install it into your system.

### To install WiperDog GUI application into XWiki.
1. Download [WiperDog XWiki Application](http://develop.wiperdog.org/jenkins/job/wiperdog-xwiki-app/lastSuccessfulBuild/artifact/target/wiperdog-xwiki-app-0.1.0.xar).  
2. Log in to XWiki as Admin, then go to "Administer: workspace" -> "Import" menu.
Then import previously downloaded xar file. 

### To install Wiperdog server into your system.
1. Download  [WiperDog Server assembly(for linux)](http://demo.wiperdog.org/jenkins/job/wiperdog-assembly/lastSuccessfulBuild/artifact/target/wiperdog-0.1.0-SNAPSHOT-unix.zip).  
or  [WiperDog Server assembly(for Windows)](http://demo.wiperdog.org/jenkins/job/wiperdog-assembly/lastSuccessfulBuild/artifact/target/wiperdog-0.1.0-SNAPSHOT-win.zip)   
2. just unzip the package, thats all.
3. If you wish to make WiperDog server as service, we only provide run script for Linux.   
(The following description is just about on centos, redhat, fedora distributions)
You can edit it to match your location of WiperDog server  then place it to /etc/init.d directory.
Then do  
        # service wiperdog start    
to start WiperDog server.  

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
Version 0.1.0 (first release)

