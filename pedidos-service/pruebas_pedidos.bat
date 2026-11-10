@echo off
chcp 65001 >nul
rem ---------------------------------------------------------------------------
rem Requisitos antes de ejecutar:
rem   - Corriendo: pedidos (8083), ESB (8280), clientes (8081) e inventarios (8082).
rem   - La base db_pedidos vacia (PP-11 consulta el pedido con id 1).
rem   - Existe el cliente con id 1 (registrarlo antes en clientes).
rem   - Inventarios con los 5 productos de ejemplo (data.sql de inventarios).
rem   - PP-10 pide apagar el ESB: sigue las instrucciones en pantalla.
rem ---------------------------------------------------------------------------
set "BASE=http://localhost:8083/api/pedidos"
echo ===== PRUEBAS DEL SERVICIO DE PEDIDOS =====
echo.
echo PP-01 Pedido con inventario suficiente (cliente 1, productos 1 x2 y 2 x3)
curl -s -w " -> HTTP %%{http_code}\n" -X POST "%BASE%" -H "Content-Type: application/json" -d "{\"clienteId\":1,\"items\":[{\"productoId\":1,\"cantidad\":2},{\"productoId\":2,\"cantidad\":3}]}"
echo.
echo PP-02 Pedido con stock insuficiente (producto 3, cantidad 20)
curl -s -w " -> HTTP %%{http_code}\n" -X POST "%BASE%" -H "Content-Type: application/json" -d "{\"clienteId\":1,\"items\":[{\"productoId\":3,\"cantidad\":20}]}"
echo.
echo PP-03 Cliente que no existe (9999)
curl -s -w " -> HTTP %%{http_code}\n" -X POST "%BASE%" -H "Content-Type: application/json" -d "{\"clienteId\":9999,\"items\":[{\"productoId\":1,\"cantidad\":1}]}"
echo.
echo PP-04 Lista de productos vacia
curl -s -w " -> HTTP %%{http_code}\n" -X POST "%BASE%" -H "Content-Type: application/json" -d "{\"clienteId\":1,\"items\":[]}"
echo.
echo PP-05 Cantidad invalida (producto 1, cantidad 0)
curl -s -w " -> HTTP %%{http_code}\n" -X POST "%BASE%" -H "Content-Type: application/json" -d "{\"clienteId\":1,\"items\":[{\"productoId\":1,\"cantidad\":0}]}"
echo.
echo PP-06 Pedido sin cliente
curl -s -w " -> HTTP %%{http_code}\n" -X POST "%BASE%" -H "Content-Type: application/json" -d "{\"items\":[{\"productoId\":1,\"cantidad\":1}]}"
echo.
pause
cls
echo PP-07 Producto inactivo (producto 5, cantidad 1)
curl -s -w " -> HTTP %%{http_code}\n" -X POST "%BASE%" -H "Content-Type: application/json" -d "{\"clienteId\":1,\"items\":[{\"productoId\":5,\"cantidad\":1}]}"
echo.
echo PP-08 Producto que no existe (9999)
curl -s -w " -> HTTP %%{http_code}\n" -X POST "%BASE%" -H "Content-Type: application/json" -d "{\"clienteId\":1,\"items\":[{\"productoId\":9999,\"cantidad\":1}]}"
echo.
echo PP-09 Producto repetido en el pedido (producto 1 dos veces)
curl -s -w " -> HTTP %%{http_code}\n" -X POST "%BASE%" -H "Content-Type: application/json" -d "{\"clienteId\":1,\"items\":[{\"productoId\":1,\"cantidad\":1},{\"productoId\":1,\"cantidad\":1}]}"
echo.
echo PP-11 Consultar pedido que existe (id 1)
curl -s -w " -> HTTP %%{http_code}\n" "%BASE%/1"
echo.
echo PP-12 Consultar pedido que no existe (id 9999)
curl -s -w " -> HTTP %%{http_code}\n" "%BASE%/9999"
echo.
echo PP-13 Listar pedidos de un cliente (clienteId 1)
curl -s -w " -> HTTP %%{http_code}\n" "%BASE%?clienteId=1"
echo.
pause
cls
echo PP-10 ESB apagado: APAGA el ESB ahora y luego presiona una tecla.
pause
curl -s -w " -> HTTP %%{http_code}\n" -X POST "%BASE%" -H "Content-Type: application/json" -d "{\"clienteId\":1,\"items\":[{\"productoId\":1,\"cantidad\":2}]}"
echo.
echo Esperado: 503 sin detalles internos. Vuelve a encender el ESB y comprueba que el pedido NO se guardo:
curl -s -w " -> HTTP %%{http_code}\n" "%BASE%"
echo.
pause
