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

1.  [WiperDog Server assembly(for linux)](http://develop.wiperdog.org/jenkins/job/wiperdog-assembly-v0.2.4/lastSuccessfulBuild/artifact/target/wiperdog-0.2.4-unix.jar).  
or  [WiperDog Server assembly(for Windows)](http://develop.wiperdog.org/jenkins/job/wiperdog-assembly-v0.2.4/lastSuccessfulBuild/artifact/target/wiperdog-0.2.4-win.jar)   
(There are no significant difference between Linux version and Windows version except line endings.)
2.  [WiperDog XWiki Application](http://develop.wiperdog.org/jenkins/job/wiperdog-xwiki-app/lastSuccessfulBuild/artifact/target/wiperdog-ui-0.2.3-SNAPSHOT.xar).  

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
Version 0.1.0 (first release)

