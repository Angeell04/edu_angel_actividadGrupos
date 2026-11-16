@echo off
chcp 65001 > nul
cd /d "%~dp0"
echo =========================================================
echo   EJECUTANDO CONVERSOR XML A MARKDOWN (.md) Y HTML (.html)
echo =========================================================
echo.

REM Comprobar si Java esta instalado y accesible en el PATH del dispositivo
java -version > nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] No se ha detectado Java instalado o configurado en el PATH de este equipo.
    echo Por favor, instala Java JRE/JDK 15 o superior para ejecutar la aplicacion.
    pause
    exit /b 1
)

REM Ejecutar el archivo .jar forzando codificacion UTF-8 en consola
if "%~1"=="" (
    java -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8 -jar "%~dp0ConversorXmlMarkdownHtml.jar"
) else (
    java -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8 -jar "%~dp0ConversorXmlMarkdownHtml.jar" "%~1"
)

echo.
echo =========================================================
echo   PROCESO FINALIZADO
echo =========================================================
if "%~2"=="" pause
