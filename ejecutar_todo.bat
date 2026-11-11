@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

set "ROOT=%~dp0"

echo ===============================================================================
echo        SISTEMA DE INTEGRACION ENTERPRISE MEDIANTE ESB (:8280)
echo               Entregables de Villa - Facturacion y Notificaciones
echo ===============================================================================
echo.

rem 1. Cargar archivo centralizado .env si existe
if exist "%ROOT%.env" (
    echo [INFO] Cargando variables centralizadas desde .env...
    for /f "usebackq tokens=1,* delims==" %%a in ("%ROOT%.env") do (
        set "line=%%a"
        if "!line:~0,1!" neq "#" if "%%a" neq "" (
            set "%%a=%%b"
        )
    )
)

rem Valores por defecto de respaldo
if "%DB_USER%"=="" set "DB_USER=root"
if "%DB_PASSWORD%"=="" set "DB_PASSWORD=root"
if "%DB_HOST%"=="" set "DB_HOST=localhost"
if "%DB_PORT%"=="" set "DB_PORT=3306"
if "%ESB_URL%"=="" set "ESB_URL=http://localhost:8280"

echo [CONFIG] Servidor BD:     !DB_HOST!:!DB_PORT!
echo [CONFIG] Usuario MySQL:   !DB_USER!
echo [CONFIG] ESB URL:         !ESB_URL!
echo.
echo Iniciando todos los microservicios en ventanas de terminal independientes...
echo.

rem 2. Levantar cada microservicio con start en su propia ventana

rem Servicio 1: Clientes (Puerto 8081)
echo [1/6] Iniciando Servicio Clientes en puerto %CLIENTES_PORT%...
start "1. Clientes (:8081)" cmd /k "cd /d %ROOT% && title Clientes (:8081) && mvn spring-boot:run"
timeout /t 2 /nobreak >nul

rem Servicio 2: Inventarios (Puerto 8082)
echo [2/6] Iniciando Servicio Inventarios en puerto %INVENTARIOS_PORT%...
start "2. Inventarios (:8082)" cmd /k "cd /d %ROOT%inventario-service && title Inventarios (:8082) && mvn spring-boot:run"
timeout /t 2 /nobreak >nul

rem ESB Local (Puerto 8280)
echo [3/6] Iniciando ESB Local Orquestador en puerto %ESB_PORT%...
start "3. ESB Local (:8280)" cmd /k "cd /d %ROOT% && title ESB Local (:8280) && java pedidos-service/herramientas/EsbLocal.java"
timeout /t 2 /nobreak >nul

rem Servicio 3: Pedidos (Puerto 8083)
echo [4/6] Iniciando Servicio Pedidos en puerto %PEDIDOS_PORT%...
start "4. Pedidos (:8083)" cmd /k "cd /d %ROOT%pedidos-service && title Pedidos (:8083) && mvn spring-boot:run"
timeout /t 2 /nobreak >nul

rem Servicio 4: Facturacion (Puerto 8084)
echo [5/6] Iniciando Servicio Facturacion en puerto %FACTURACION_PORT%...
start "5. Facturacion (:8084)" cmd /k "cd /d %ROOT%facturacion-service && title Facturacion (:8084) && mvn spring-boot:run"
timeout /t 2 /nobreak >nul

rem Servicio 5: Notificaciones (Puerto 8085)
echo [6/6] Iniciando Servicio Notificaciones en puerto %NOTIFICACIONES_PORT%...
start "6. Notificaciones (:8085)" cmd /k "cd /d %ROOT%notificaciones-service && title Notificaciones (:8085) && mvn spring-boot:run"
timeout /t 3 /nobreak >nul

echo.
echo ===============================================================================
echo  TODOS LOS SERVICIOS HAN SIDO LANZADOS EXITOSAMENTE
echo ===============================================================================
echo  * Clientes:       http://localhost:8081/api/clientes
echo  * Inventarios:    http://localhost:8082/api/inventario/productos
echo  * Pedidos:        http://localhost:8083/api/pedidos
echo  * Facturacion:    http://localhost:8084/api/facturas
echo  * Notificaciones: http://localhost:8085/api/notificaciones
echo  * ESB Router:     %ESB_URL%
echo ===============================================================================
echo.
echo Abriendo Dashboard Frontend en el navegador web...
start "" "%ROOT%frontend\index.html"

echo.
echo Presione cualquier tecla cuando desee salir de este lanzador (las ventanas seguiran abiertas).
pause >nul
