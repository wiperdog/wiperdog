NOTICE:
A) WINDOWS
Copy all folders to WIPERDOG_HOME\, such as E:\wiperdog
To install Wiperdog as windows service please do the followings:
1. Stop wiperdog service if it is running
2. Run batch file create_wiperdog_service.bat <WIPERDOG_HOME>
3. Start by running command: net start Wiperdog
4. Stop by: net stop Wiperdog

B) LINUX
Please take care of the following:
1. Make sure file wiperdog exists in <WIPERDOG_HOME>/bin
2. Configure WIPERDOG_HOME in this file 
3. If the service did not exist in /etc/init.d please copy it into that folder
   *) chmod 755 /etc/init.d/wiperdog
   *) chkconfig --add wiperdog
   *) chkconfig --level 3 wiperdog on
   *) chkconfig --level 4 wiperdog on
   *) chkconfig --level 5 wiperdog on
4. Try start wiperdog by:
service wiperdog start
or try stoping:
service wiperdog stop
