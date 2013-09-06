' This fixes a Java bug.  Java STDERR and STDOUT don't work properly from
' within a batch file if redirected.  By piping the STDOUT to this VBS
' and printing it to STDOUT, we fix the problem for STDOUT only.  There is
' no way to pipe to STDERR.  

Do While Not WScript.StdIn.AtEndOfStream 
   str = WScript.StdIn.ReadLine
   WScript.StdOut.WriteLine str 
Loop 

