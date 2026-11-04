@echo off
setlocal
set "DIR=%~dp0"
if exist "%DIR%tools\maven\bin\mvn.cmd" (
    set "MAVEN_CMD=%DIR%tools\maven\bin\mvn.cmd"
) else (
    set "MAVEN_CMD=mvn"
)
if not defined JAVA_HOME (
    if exist "C:\Program Files\Java\jdk-21.0.10" (
        set "JAVA_HOME=C:\Program Files\Java\jdk-21.0.10"
    )
)
"%MAVEN_CMD%" %*
