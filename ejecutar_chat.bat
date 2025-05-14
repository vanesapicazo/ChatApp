@echo off
echo Compilando...

REM Crear carpeta bin si no existe
if not exist bin mkdir bin

REM Compilar todos los archivos fuente y enviar los .class a la carpeta bin
javac -cp lib\flatlaf-3.2.jar -d bin src\ClienteLoginGUI.java src\ClienteChatGUI.java src\Servidor.java

if errorlevel 1 (
    echo ❌ Error en la compilación.
    pause
    exit /b
)

echo ✅ Compilación exitosa.
echo Ejecutando...

REM Ejecutar ClienteLoginGUI desde bin usando la librería
java -cp bin;lib\flatlaf-3.2.jar ClienteLoginGUI

pause
