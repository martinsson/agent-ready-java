@echo off
rem Windows (cmd, PowerShell): .\verify runs this file, as .\mvnw runs mvnw.cmd. Without it Windows
rem cannot open the extensionless bash script ./verify and asks which application should.
rem Git for Windows' bash, not WSL's (C:\Windows\System32\bash.exe), which sees neither the Windows
rem Maven nor the Windows JDK.
setlocal
set "GITBASH="
for /f "delims=" %%g in ('where git 2^>nul') do (
  if not defined GITBASH if exist "%%~dpg..\bin\bash.exe" set "GITBASH=%%~dpg..\bin\bash.exe"
)
if not defined GITBASH if exist "%ProgramFiles%\Git\bin\bash.exe" set "GITBASH=%ProgramFiles%\Git\bin\bash.exe"
if not defined GITBASH (
  echo verify: Git Bash not found. Install Git for Windows, or run ./verify from Git Bash. 1>&2
  exit /b 2
)
pushd "%~dp0"
"%GITBASH%" ./verify %*
set "rc=%ERRORLEVEL%"
popd
exit /b %rc%
