@echo off
chcp 65001 >nul
rem ---------------------------------------------------------------------------
rem SCRIPT DE PRUEBAS AUTOMATIZADAS CON CURL - VILLA
rem Facturacion (8084), Notificaciones (8085) y Acceso via ESB (8280)
rem
rem Requisitos:
rem   - Servicios corriendo: Facturacion (:8084), Notificaciones (:8085), ESB (:8280)
rem ---------------------------------------------------------------------------

setlocal enabledelayedexpansion

rem Cargar .env centralizado si existe
if exist "%~dp0.env" (
    for /f "usebackq tokens=1,* delims==" %%a in ("%~dp0.env") do (
        set "line=%%a"
        if "!line:~0,1!" neq "#" if "%%a" neq "" (
            set "%%a=%%b"
        )
    )
)

if "%FACTURACION_PORT%"=="" set "FACTURACION_PORT=8084"
if "%NOTIFICACIONES_PORT%"=="" set "NOTIFICACIONES_PORT=8085"
if "%ESB_URL%"=="" set "ESB_URL=http://localhost:8280"

set "FACT_DIRECT=http://localhost:!FACTURACION_PORT!/api/facturas"
set "NOTIF_DIRECT=http://localhost:!NOTIFICACIONES_PORT!/api/notificaciones"
set "FACT_ESB=!ESB_URL!/api/facturas"
set "NOTIF_ESB=!ESB_URL!/api/notificaciones"

echo ===============================================================================
echo          PRUEBAS DEL SERVICIO DE FACTURACION (8084) Y NOTIFICACIONES (8085)
echo ===============================================================================
echo.

rem ===========================================================================
rem SECCION 1: PRUEBAS DE FACTURACION (:8084)
rem ===========================================================================
echo ===== 1. PRUEBAS DE FACTURACION =====
echo.

echo [PF-01] Generar factura exitosa para Pedido 101, Cliente 1, Monto $245,000 (Esperado: 201)
curl -s -w " -> HTTP %%{http_code}\n" -X POST "%FACT_DIRECT%" -H "Content-Type: application/json" -d "{\"pedidoId\":101,\"clienteId\":1,\"montoTotal\":245000.00}"
echo.

echo [PF-02] Generar factura duplicada para el mismo Pedido 101 (Esperado: 409 Conflict)
curl -s -w " -> HTTP %%{http_code}\n" -X POST "%FACT_DIRECT%" -H "Content-Type: application/json" -d "{\"pedidoId\":101,\"clienteId\":1,\"montoTotal\":245000.00}"
echo.

echo [PF-03] Generar factura con monto invalido 0 (Esperado: 400 Bad Request)
curl -s -w " -> HTTP %%{http_code}\n" -X POST "%FACT_DIRECT%" -H "Content-Type: application/json" -d "{\"pedidoId\":102,\"clienteId\":1,\"montoTotal\":0}"
echo.

echo [PF-04] Consultar factura por ID existente (ID 1) (Esperado: 200 OK)
curl -s -w " -> HTTP %%{http_code}\n" "%FACT_DIRECT%/1"
echo.

echo [PF-05] Consultar factura por ID inexistente (ID 9999) (Esperado: 404 Not Found)
curl -s -w " -> HTTP %%{http_code}\n" "%FACT_DIRECT%/9999"
echo.

echo [PF-06] Consultar factura asociada al Pedido 101 (Esperado: 200 OK)
curl -s -w " -> HTTP %%{http_code}\n" "%FACT_DIRECT%/pedido/101"
echo.

echo [PF-07] Listar todas las facturas registradas (Esperado: 200 OK)
curl -s -w " -> HTTP %%{http_code}\n" "%FACT_DIRECT%"
echo.

pause
cls

rem ===========================================================================
rem SECCION 2: PRUEBAS DE NOTIFICACIONES (:8085)
rem ===========================================================================
echo ===== 2. PRUEBAS DE NOTIFICACIONES =====
echo.

echo [PN-01] Enviar notificacion de Pedido Confirmado (Esperado: 200 OK con codigo de seguimiento)
curl -s -w " -> HTTP %%{http_code}\n" -X POST "%NOTIF_DIRECT%/enviar" -H "Content-Type: application/json" -d "{\"clienteId\":1,\"email\":\"laura.gomez@correo.com\",\"tipo\":\"PEDIDO_CONFIRMADO\",\"mensaje\":\"Su pedido #101 ha sido confirmado exitosamente. Factura #1 generada por $245,000.\"}"
echo.

echo [PN-02] Enviar notificacion con email invalido (Esperado: 400 Bad Request)
curl -s -w " -> HTTP %%{http_code}\n" -X POST "%NOTIF_DIRECT%/enviar" -H "Content-Type: application/json" -d "{\"clienteId\":1,\"email\":\"correo-invalido-sin-arroba\",\"tipo\":\"PEDIDO_CONFIRMADO\",\"mensaje\":\"Prueba error\"}"
echo.

echo [PN-03] Enviar notificacion de Pedido Rechazado con motivo (Esperado: 200 OK)
curl -s -w " -> HTTP %%{http_code}\n" -X POST "%NOTIF_DIRECT%/enviar" -H "Content-Type: application/json" -d "{\"clienteId\":2,\"email\":\"carlos.perez@correo.com\",\"tipo\":\"PEDIDO_RECHAZADO\",\"mensaje\":\"Su pedido #102 fue rechazado. Motivo: Stock insuficiente para Monitor 24 pulgadas.\"}"
echo.

echo [PN-04] Consultar bitacora de auditoria de notificaciones (Esperado: 200 OK)
curl -s -w " -> HTTP %%{http_code}\n" "%NOTIF_DIRECT%"
echo.

pause
cls

rem ===========================================================================
rem SECCION 3: PRUEBAS DE ACCESO VIA ESB (:8280) - CUMPLIMIENTO RF-07
rem ===========================================================================
echo ===== 3. PRUEBAS DE ACCESO VIA ESB (:8280) =====
echo.

echo [PE-01] Consultar facturas a traves del ESB (GET :8280/api/facturas) (Esperado: 200 OK)
curl -s -w " -> HTTP %%{http_code}\n" "%FACT_ESB%"
echo.

echo [PE-02] Consultar notificaciones a traves del ESB (GET :8280/api/notificaciones) (Esperado: 200 OK)
curl -s -w " -> HTTP %%{http_code}\n" "%NOTIF_ESB%"
echo.

echo [PE-03] Enviar notificacion a traves del ESB (POST :8280/api/notificaciones/enviar) (Esperado: 200 OK)
curl -s -w " -> HTTP %%{http_code}\n" -X POST "%NOTIF_ESB%/enviar" -H "Content-Type: application/json" -d "{\"clienteId\":3,\"email\":\"ana.lopez@correo.com\",\"tipo\":\"PEDIDO_CONFIRMADO\",\"mensaje\":\"Notificacion de prueba enviada a traves del ESB :8280\"}"
echo.

echo ===============================================================================
echo                FIN DE LA SUITE DE PRUEBAS DE VILLA
echo ===============================================================================
pause

