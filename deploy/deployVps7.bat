REM @echo off

putty.exe -ssh tomcat8@vps7 -m removeoldwarcommand.txt || goto :error
pscp target\sqrlexample.war tomcat8@vps7:/opt/apache-tomcat-8.0.33/webapps

exit /b 0

:error 
echo Failed with error #%errorlevel%.
exit /b %errorlevel%