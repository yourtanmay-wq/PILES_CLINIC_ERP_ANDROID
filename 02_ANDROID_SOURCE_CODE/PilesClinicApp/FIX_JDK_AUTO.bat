@echo off
setlocal EnableDelayedExpansion
title TK Piles Clinic - JDK Auto-Fix
color 0A

echo ============================================================
echo   Piles Clinic App - JDK ঠিক করা হচ্ছে (নিজে থেকে খুঁজে বের করা হবে)
echo   এই উইন্ডো বন্ধ করবেন না, শেষ না হওয়া পর্যন্ত অপেক্ষা করুন...
echo ============================================================
echo.

set "GPROPS=%~dp0gradle.properties"
if not exist "%GPROPS%" (
    echo [ERROR] gradle.properties ফাইল পাওয়া যায়নি এই ফোল্ডারে:
    echo    %~dp0
    echo    এই .bat ফাইলটা অবশ্যই প্রজেক্টের একদম ভেতরে রাখতে হবে,
    echo    যেখানে gradle.properties আছে ঠিক সেই একই ফোল্ডারে।
    echo.
    pause
    exit /b 1
)

set "FOUNDJDK="

REM ---- ধাপ ১: চেনা জায়গাগুলো একে একে চেক করা ----
set "CANDIDATES[0]=C:\Program Files\Android\Android Studio\jbr"
set "CANDIDATES[1]=C:\Program Files\Android\Android Studio1\jbr"
set "CANDIDATES[2]=C:\Program Files\Android\Android Studio2\jbr"
set "CANDIDATES[3]=C:\Program Files\Android\Android Studio3\jbr"
set "CANDIDATES[4]=%LOCALAPPDATA%\Programs\Android Studio\jbr"
set "CANDIDATES[5]=%LOCALAPPDATA%\Google\AndroidStudio\jbr"

for /L %%i in (0,1,5) do (
    if not defined FOUNDJDK (
        call :checkjdk "!CANDIDATES[%%i]!"
    )
)

REM ---- ধাপ ২: %USERPROFILE%\.jdks ফোল্ডারের ভেতরের সব ফোল্ডার চেক করা ----
if not defined FOUNDJDK (
    if exist "%USERPROFILE%\.jdks" (
        for /d %%D in ("%USERPROFILE%\.jdks\*") do (
            if not defined FOUNDJDK (
                call :checkjdk "%%D"
            )
        )
    )
)

REM ---- ধাপ ৩: C:\Program Files\Android এর ভেতরের সব Android Studio* ফোল্ডার চেক করা ----
if not defined FOUNDJDK (
    if exist "C:\Program Files\Android" (
        for /d %%D in ("C:\Program Files\Android\Android Studio*") do (
            if not defined FOUNDJDK (
                call :checkjdk "%%D\jbr"
            )
        )
    )
)

if not defined FOUNDJDK (
    echo.
    echo [ERROR] এই কম্পিউটারে সঠিক JDK ( ভার্সন ৮ থেকে ২১-এর মধ্যে ) কোথাও খুঁজে পাওয়া যায়নি।
    echo    এই স্ক্রিনের পুরো লেখাটার একটা ছবি তুলে পাঠান।
    echo.
    pause
    exit /b 1
)

echo.
echo [ঠিক আছে] পাওয়া গেছে: %FOUNDJDK%
echo.

REM ---- gradle.properties থেকে পুরনো org.gradle.java.home লাইন (যদি থাকে) বাদ দেওয়া ----
set "TMPFILE=%~dp0gradle.properties.tmp"
if exist "%TMPFILE%" del "%TMPFILE%"
(for /f "usebackq delims=" %%L in ("%GPROPS%") do (
    echo %%L | findstr /b /c:"org.gradle.java.home" >nul
    if errorlevel 1 echo %%L
)) > "%TMPFILE%"

REM ---- পথটাকে gradle.properties-এর নিয়মে ঠিক করা (\ কে \\ ) ----
set "ESCAPED=%FOUNDJDK%"
set "ESCAPED=!ESCAPED:\=\\!"

echo org.gradle.java.home=!ESCAPED! >> "%TMPFILE%"

if not exist "%TMPFILE%" (
    echo [ERROR] নতুন ফাইল বানানো যায়নি। এই স্ক্রিনের ছবি তুলে পাঠান।
    pause
    exit /b 1
)
for %%Z in ("%TMPFILE%") do if %%~zZ==0 (
    echo [ERROR] নতুন ফাইল ফাঁকা এসেছে, পুরনো ফাইল অক্ষত রাখা হলো। এই স্ক্রিনের ছবি তুলে পাঠান।
    del "%TMPFILE%"
    pause
    exit /b 1
)

move /y "%TMPFILE%" "%GPROPS%" >nul

echo   এখন gradle.properties-এর শেষ লাইনে যা বসেছে:
findstr /b /c:"org.gradle.java.home" "%GPROPS%"
echo.
echo ============================================================
echo   ✅ ঠিক হয়ে গেছে!
echo   এখন এই কাজগুলো করুন:
echo   ১. এই উইন্ডো বন্ধ করুন
echo   ২. Android Studio পুরোপুরি বন্ধ করুন (যদি খোলা থাকে)
echo   ৩. Android Studio আবার খুলুন, প্রজেক্টটা খুলুন
echo   ৪. এমনি এমনি Sync শুরু হয়ে যাবে, কয়েক মিনিট অপেক্ষা করুন
echo ============================================================
echo.
pause
exit /b 0

:checkjdk
set "CAND=%~1"
if "%CAND%"=="" exit /b 0
if not exist "%CAND%\bin\java.exe" exit /b 0
for /f "tokens=*" %%V in ('"%CAND%\bin\java.exe" -version 2^>^&1') do (
    echo %%V | findstr /C:"version \"11." /C:"version \"17." /C:"version \"18." /C:"version \"19." /C:"version \"20." /C:"version \"21." >nul
    if not errorlevel 1 (
        set "FOUNDJDK=%CAND%"
    )
)
exit /b 0
