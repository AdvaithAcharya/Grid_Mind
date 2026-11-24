@echo off
setlocal
set "JAVA_HOME=C:\Users\user\Downloads\openjdk-25.0.2_windows-x64_bin\jdk-25.0.2"
set "PATH=%JAVA_HOME%\bin;%PATH%"
set "MAVEN_CMD=D:\gridmind\backend\.mvn\apache-maven-3.9.9\bin\mvn.cmd"

if "%~1"=="" (
    call "%MAVEN_CMD%" spring-boot:run
) else (
    call "%MAVEN_CMD%" %*
)
