@echo off
setlocal

set "ROOT_DIR=%~dp0"
set "BACKEND_PORT=8080"
set "FRONTEND_PORT=3000"

echo.
echo ========================================
echo           QuantEdge Startup
echo ========================================
echo.

REM --- Backend ---------------------------------------------------------------

netstat -ano | findstr /R /C:":%BACKEND_PORT% .*LISTENING" >nul 2>&1
if %ERRORLEVEL% EQU 0 goto :backend_running

echo Backend: opening terminal on :%BACKEND_PORT%

if not exist "%ROOT_DIR%.run" mkdir "%ROOT_DIR%.run"
set "BACKEND_LAUNCHER=%ROOT_DIR%.run\backend_launch.ps1"

REM Env vars (DATABASE_URL etc.) are loaded via PowerShell rather than a
REM plain `set` in this .bat, since values like DATABASE_URL contain
REM unescaped `&`, which cmd.exe would otherwise parse as a command
REM separator and truncate.
> "%BACKEND_LAUNCHER%" echo function Import-DotEnv {
>> "%BACKEND_LAUNCHER%" echo     param([string]$Path)
>> "%BACKEND_LAUNCHER%" echo     if (-not (Test-Path $Path)) { return }
>> "%BACKEND_LAUNCHER%" echo     Get-Content $Path ^| ForEach-Object {
>> "%BACKEND_LAUNCHER%" echo         $line = $_.Trim()
>> "%BACKEND_LAUNCHER%" echo         if ($line -eq '' -or $line.StartsWith('#')) { return }
>> "%BACKEND_LAUNCHER%" echo         $idx = $line.IndexOf('=')
>> "%BACKEND_LAUNCHER%" echo         if ($idx -lt 1) { return }
>> "%BACKEND_LAUNCHER%" echo         $key = $line.Substring(0, $idx).Trim()
>> "%BACKEND_LAUNCHER%" echo         $value = $line.Substring($idx + 1).Trim()
>> "%BACKEND_LAUNCHER%" echo         Set-Item -Path "Env:$key" -Value $value
>> "%BACKEND_LAUNCHER%" echo     }
>> "%BACKEND_LAUNCHER%" echo }
>> "%BACKEND_LAUNCHER%" echo Import-DotEnv -Path '%ROOT_DIR%.env'
>> "%BACKEND_LAUNCHER%" echo Import-DotEnv -Path '%ROOT_DIR%backend\.env'
>> "%BACKEND_LAUNCHER%" echo Set-Location '%ROOT_DIR%backend'
>> "%BACKEND_LAUNCHER%" echo ^& .\mvnw.cmd spring-boot:run
>> "%BACKEND_LAUNCHER%" echo Write-Host ''
>> "%BACKEND_LAUNCHER%" echo Read-Host 'Backend exited. Press Enter to close'

start "QuantEdge Backend" cmd /k powershell -NoExit -NoProfile -ExecutionPolicy Bypass -File "%BACKEND_LAUNCHER%"
goto :backend_done

:backend_running
echo Backend: port %BACKEND_PORT% already in use - skipping.

:backend_done

REM --- Frontend --------------------------------------------------------------

netstat -ano | findstr /R /C:":%FRONTEND_PORT% .*LISTENING" >nul 2>&1
if %ERRORLEVEL% EQU 0 goto :frontend_running

echo Frontend: opening terminal on :%FRONTEND_PORT%
start "QuantEdge Frontend" cmd /k "cd /d "%ROOT_DIR%frontend" && set NEXT_TELEMETRY_DISABLED=1 && npm run dev"
goto :frontend_done

:frontend_running
echo Frontend: port %FRONTEND_PORT% already in use - skipping.

:frontend_done

echo.
echo QuantEdge startup commands launched.
echo.
pause
