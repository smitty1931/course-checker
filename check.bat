@echo off
setlocal enabledelayedexpansion

set "sem=%1"
set "courses="
for /f "delims=" %%i in (tocheck.txt) do set "courses=%%i"

mkdir search
cd search

echo %courses%

set "codes="
for %%i in (%courses%) do (
    set "code=%%i"
    set "code=!code:~0,4!"
    if "!codes!" == "" (
        set "codes=!code!"
    ) else (
        if "!codes!" neq "!codes:!code!=!" (
            set "codes=!codes! !code!"
        )
    )
)

for %%i in (%codes%) do (
    set "command=https://api.easi.utoronto.ca/ttb/getOptimizedMatchingCourseTitles?term=%%i&divisions=ARTSC&sessions=%sem%&lowerThreshold=50&upperThreshold=200"
    curl --location "!command!" > "%%i.xml"
)

for %%i in (%courses%) do (
    set "code=%%i"
    set "code=!code:~0,4!"
    set "title=%%i"
    set "title=!title:~4!"
    findstr /c:"!title!" "!code!.xml" >nul
    if !errorlevel! == 0 (
        echo Found %%i
    )
)

cd ..
rd /s /q search