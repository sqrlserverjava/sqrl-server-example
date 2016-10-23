REM @echo off

putty.exe -ssh tomcat7@jj -m deploy\moveOldWarJj.txt || goto :error
pscp target\sqrlexample.war tomcat7@jj:/var/lib/tomcat7/webapps

exit /b 0

:error 
echo Failed with error #%errorlevel%.
exit /b %errorlevel%