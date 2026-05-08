@echo off
REM Gradle wrapper script for Windows

SETLOCAL ENABLEDELAYEDEXPANSION

REM Locate Java
if defined JAVA_HOME (
    SET JAVA_EXE=%JAVA_HOME%\bin\java.exe
) else (
    SET JAVA_EXE=java
)

REM Verify Java
%JAVA_EXE% -version >nul 2>&1
if errorlevel 1 (
    echo Java is not installed or not in PATH.
    echo Please install Java 17 or higher (https://adoptium.net/).
    pause
    exit /b 1
)

SET WRAPPER_JAR=gradle\wrapper\gradle-wrapper.jar
if not exist "%WRAPPER_JAR%" (
    echo Gradle wrapper jar not found at %WRAPPER_JAR%
    pause
    exit /b 1
)

%JAVA_EXE% -classpath "%WRAPPER_JAR%" org.gradle.wrapper.GradleWrapperMain %*
