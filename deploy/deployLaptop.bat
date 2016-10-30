REM @echo off
set WEBAPPS_DIR=D:\code\apache-tomcat-8.0.36\webapps
set APP_DIR=%WEBAPPS_DIR%\sqrlexample
RMDIR  /S /Q %APP_DIR%

exit /b 0

:error 
echo Failed with error #%errorlevel%.
exit /b %errorlevel%