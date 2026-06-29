@echo off
chcp 65001 >nul
REM ============================================================
REM   RUN_BOT — מפעיל מרכזי ל-DTC Analyzer + בוט תרגום XTOOL
REM   הפעלה: לחיצה כפולה על הקובץ (Windows)
REM ============================================================
cd /d "%~dp0"

:menu
echo.
echo ============================================
echo    בוט DTC / תרגום XTOOL — תפריט הפעלה
echo ============================================
echo   1 - בנה והתקן את אפליקציית התרגום על הטאבלט (Android)
echo   2 - בנה APK בלבד (בלי התקנה)
echo   3 - רענן את מילון ה-DTC מבסיס הידע
echo   4 - הרץ את בוט המייל-טלגרם (garage_bot_v2.py)
echo   5 - הרץ את מנתח הדוחות (dtc_analyzer.py)
echo   0 - יציאה
echo ============================================
set /p choice=בחר מספר ואנטר:

if "%choice%"=="1" goto build_install
if "%choice%"=="2" goto build_only
if "%choice%"=="3" goto refresh_dict
if "%choice%"=="4" goto run_pybot
if "%choice%"=="5" goto run_analyzer
if "%choice%"=="0" goto end
echo בחירה לא תקינה.
goto menu

:build_install
call :refresh_dict_inline
echo בונה APK...
cd android
call gradlew.bat assembleDebug || goto err
cd ..
echo מתקין על הטאבלט (ודא ש-USB Debugging פעיל וחובר USB)...
adb install -r android\app\build\outputs\apk\debug\app-debug.apk || goto err
echo ✓ הותקן. פתח באפליקציה "תרגום XTOOL" והשלם את האונבורדינג.
goto menu

:build_only
call :refresh_dict_inline
cd android
call gradlew.bat assembleDebug || goto err
cd ..
echo ✓ נבנה: android\app\build\outputs\apk\debug\app-debug.apk
goto menu

:refresh_dict
call :refresh_dict_inline
echo ✓ המילון עודכן.
goto menu

:refresh_dict_inline
echo מרענן מילון DTC...
python tools\export_kb.py
exit /b 0

:run_pybot
python garage_bot_v2.py
goto menu

:run_analyzer
python dtc_analyzer.py
goto menu

:err
echo.
echo ❌ שגיאה. בדוק ש-Android SDK / adb / python מותקנים.
cd /d "%~dp0"
goto menu

:end
echo ביי!
