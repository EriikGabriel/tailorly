@ECHO OFF
WHERE mvn >NUL 2>NUL
IF %ERRORLEVEL% EQU 0 (
  CALL mvn %*
  EXIT /B %ERRORLEVEL%
)
ECHO Maven 3.9+ is required. Install Maven or set MAVEN_HOME before running this command.
EXIT /B 1
