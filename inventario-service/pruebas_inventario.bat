@echo off
chcp 65001 >nul
set "BASE=http://localhost:8082/api/inventario"
echo ===== PRUEBAS DEL SERVICIO DE INVENTARIOS =====
echo.
echo PI-01 Listar productos
curl -s -w " -> HTTP %%{http_code}\n" "%BASE%/productos"
echo.
echo PI-02 Consultar producto existente (id 1)
curl -s -w " -> HTTP %%{http_code}\n" "%BASE%/productos/1"
echo.
echo PI-03 Consultar producto inexistente (id 9999)
curl -s -w " -> HTTP %%{http_code}\n" "%BASE%/productos/9999"
echo.
echo PI-04 Disponibilidad con stock suficiente (id 1, cantidad 5)
curl -s -w " -> HTTP %%{http_code}\n" "%BASE%/disponibilidad/1?cantidad=5"
echo.
echo PI-05 Disponibilidad con stock insuficiente (id 3, cantidad 20)
curl -s -w " -> HTTP %%{http_code}\n" "%BASE%/disponibilidad/3?cantidad=20"
echo.
echo PI-06 Producto sin stock (id 4, cantidad 1)
curl -s -w " -> HTTP %%{http_code}\n" "%BASE%/disponibilidad/4?cantidad=1"
echo.
echo PI-07 Producto inactivo (id 5, cantidad 1)
curl -s -w " -> HTTP %%{http_code}\n" "%BASE%/disponibilidad/5?cantidad=1"
echo.
echo PI-08 Cantidad invalida (id 1, cantidad 0)
curl -s -w " -> HTTP %%{http_code}\n" "%BASE%/disponibilidad/1?cantidad=0"
echo.
pause
cls
echo PI-09 Verificar pedido completo (ids 1 y 2)
curl -s -w " -> HTTP %%{http_code}\n" -X POST "%BASE%/verificar" -H "Content-Type: application/json" -d "{\"items\":[{\"productoId\":1,\"cantidad\":2},{\"productoId\":2,\"cantidad\":3}]}"
echo.
echo PI-10 Verificar con un producto sin stock (ids 1 y 3, cantidad 20)
curl -s -w " -> HTTP %%{http_code}\n" -X POST "%BASE%/verificar" -H "Content-Type: application/json" -d "{\"items\":[{\"productoId\":1,\"cantidad\":2},{\"productoId\":3,\"cantidad\":20}]}"
echo.
echo PI-11 Verificar con lista vacia
curl -s -w " -> HTTP %%{http_code}\n" -X POST "%BASE%/verificar" -H "Content-Type: application/json" -d "{\"items\":[]}"
echo.
echo PI-12 Verificar con producto inexistente (id 9999)
curl -s -w " -> HTTP %%{http_code}\n" -X POST "%BASE%/verificar" -H "Content-Type: application/json" -d "{\"items\":[{\"productoId\":9999,\"cantidad\":1}]}"
echo.
echo PI-13 Ruta inexistente
curl -s -w " -> HTTP %%{http_code}\n" "%BASE%/xyz"
echo.
pause
