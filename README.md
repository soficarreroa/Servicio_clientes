# Proyecto 1 — Integración de servicios de una empresa comercial mediante ESB

Conjunto de microservicios REST independientes que componen el backend de una empresa comercial. Cada servicio tiene su propia base de datos MySQL y se comunican **exclusivamente** a través de un ESB (Enterprise Service Bus), cumpliendo la regla RF-07: ningún servicio llama directamente a otro.

---

## Índice

- [Arquitectura general](#arquitectura-general)
- [Mapa de puertos y bases de datos](#mapa-de-puertos-y-bases-de-datos)
- [Tecnologías comunes](#tecnologías-comunes)
- [Requisitos previos](#requisitos-previos)
- [Microservicio 1 — Gestión de clientes](#microservicio-1--gestión-de-clientes)
- [Microservicio 2 — Consulta de inventarios](#microservicio-2--consulta-de-inventarios)
- [Microservicio 3 — Procesamiento de pedidos](#microservicio-3--procesamiento-de-pedidos)
- [Microservicio 4 — Facturación de pedidos](#microservicio-4--facturación-de-pedidos)
- [Microservicio 5 — Notificaciones al cliente](#microservicio-5--notificaciones-al-cliente)
- [Frontend Dashboard Web](#frontend-dashboard-web)
- [ESB local con orquestador](#esb-local-para-pruebas)
- [Cómo levantar todo el sistema](#cómo-levantar-todo-el-sistema)
- [Estructura completa del repositorio](#estructura-completa-del-repositorio)

---

## Arquitectura general

```
┌────────────┐      ┌────────────┐      ┌────────────────┐
│  Cliente   │─────▸│    ESB     │─────▸│   Clientes     │ :8081
│ (Postman,  │      │  :8280     │─────▸│   Inventarios  │ :8082
│  app web)  │      │            │─────▸│   Pedidos      │ :8083
└────────────┘      │            │─────▸│   Facturación  │ :8084
                    │            │─────▸│   Notificac.   │ :8085
                    └────────────┘
```

- Protocolo: **HTTP REST** con **JSON** (UTF-8).
- Cada servicio tiene su propia base de datos MySQL; ninguno accede a la de otro.
- Los 5 microservicios se encuentran 100% implementados e integrados vía ESB (:8280).

---

## Mapa de puertos y bases de datos

| Servicio | Puerto | Base de datos | Ruta base |
|---|---|---|---|
| Gestión de clientes | 8081 | `clientes_db` | `/api/clientes` |
| Consulta de inventarios | 8082 | `db_inventarios` | `/api/inventario` |
| Procesamiento de pedidos | 8083 | `db_pedidos` | `/api/pedidos` |
| Facturación | 8084 | `db_facturacion` | `/api/facturas` |
| Notificaciones | 8085 | Memoria / Audit log | `/api/notificaciones` |
| ESB | 8280 | — | — |

---

## Tecnologías comunes

| Tecnología | Versión |
|---|---|
| Java | 17 |
| Spring Boot | 3.3.5 |
| Spring Web | gestionado por Spring Boot |
| Spring Data JPA | gestionado por Spring Boot |
| Bean Validation | gestionado por Spring Boot |
| MySQL Connector/J | gestionado por Spring Boot |
| H2 Database | solo para pruebas |
| Mockito / JUnit 5 | solo para pruebas |
| Maven | gestor de dependencias |

---

## Requisitos previos

| Herramienta | Versión mínima |
|---|---|
| Java | JDK 17 o superior |
| Maven | 3.6+ |
| MySQL | 8.0+ |

---

## Microservicio 1 — Gestión de clientes

> **Carpeta:** raíz del proyecto (`src/`)  
> **Puerto:** 8081  
> **BD:** `clientes_db` (se crea sola)

Registra y consulta clientes. No modifica ni elimina.

### Configuración de la base de datos

La base de datos y la tabla se crean solas. En `src/main/resources/application.properties` hay que ajustar:

```properties
spring.datasource.username=root        # cambiar por tu usuario
spring.datasource.password=tu_clave    # cambiar por tu clave
```

> **Aviso:** No subir la clave real a GitHub.

### Ejecución

```bash
mvn spring-boot:run
```

### Endpoints

| Método | URL | Qué hace | Código |
|---|---|---|---|
| POST | `/api/clientes` | Registra un cliente | 201 |
| GET | `/api/clientes/{id}` | Consulta un cliente por id | 200 |
| GET | `/api/clientes` | Lista todos los clientes | 200 |

### Ejemplo — Registrar un cliente

```bash
curl -X POST http://localhost:8081/api/clientes \
  -H "Content-Type: application/json" \
  -d '{"documento":"1020304050","nombre":"Laura","apellido":"Gómez","email":"laura.gomez@correo.com","telefono":"3001234567","direccion":"Calle 10 # 5-20, Bogotá"}'
```

Respuesta (201):

```json
{
  "id": 1,
  "documento": "1020304050",
  "nombre": "Laura",
  "apellido": "Gómez",
  "email": "laura.gomez@correo.com",
  "telefono": "3001234567",
  "direccion": "Calle 10 # 5-20, Bogotá",
  "fechaRegistro": "2026-10-28T10:30:00"
}
```

### Campos del cliente

| Campo | Tipo | Obligatorio | Reglas |
|---|---|---|---|
| id | Long | automático | autogenerado |
| documento | texto | sí | único, máx. 30 caracteres |
| nombre | texto | sí | máx. 80 caracteres |
| apellido | texto | sí | máx. 80 caracteres |
| email | texto | sí | formato válido, único, máx. 120, se guarda en minúsculas |
| telefono | texto | no | solo números, espacios o `+`, máx. 20 |
| direccion | texto | no | máx. 200 caracteres |
| fechaRegistro | fecha y hora | automático | se asigna al guardar |

### Códigos de error

| Código | Cuándo ocurre |
|---|---|
| 400 | Datos faltantes o inválidos |
| 404 | Cliente no existe |
| 409 | Documento o email repetido |
| 500 | Error inesperado (sin detalles internos) |

Formato JSON de error: `fecha`, `estado`, `error`, `mensaje`.

### Pruebas (10)

Ejecutar: `mvn test` (usan H2 en memoria, **no necesitan MySQL**).

| Prueba | Qué verifica |
|---|---|
| P-01 | Registrar con datos correctos → 201 |
| P-02 | Registrar sin campos opcionales → 201 |
| P-03 | Registrar sin nombre → 400 |
| P-04 | Email mal escrito → 400 |
| P-05 | Documento repetido → 409 |
| P-06 | Email repetido → 409 |
| P-07 | Consultar cliente existente → 200 |
| P-08 | Consultar cliente inexistente → 404 |
| P-09 | Listar con varios registrados → 200 |
| P-10 | Listar sin clientes → 200 (lista vacía) |

---

## Microservicio 2 — Consulta de inventarios

> **Carpeta:** `inventario-service/`  
> **Puerto:** 8082  
> **BD:** `db_inventarios` (se crea sola)

Permite consultar productos, verificar existencias, registrar nuevos productos y descontar stock automáticamente tras la confirmación de pedidos.

### Configuración

Las credenciales y puertos se gestionan de forma centralizada en el archivo `.env` de la raíz del proyecto (ver `.env.example`).

Al arrancar se cargan 5 productos de ejemplo desde `data.sql` (no se duplican al reiniciar).

### Ejecución

```bash
mvn spring-boot:run
```

### Endpoints

| Método | URL | Qué hace | Código |
|---|---|---|---|
| GET | `/api/inventario/productos` | Lista productos con stock | 200 |
| POST | `/api/inventario/productos` | Registra un nuevo producto | 201 |
| GET | `/api/inventario/productos/{id}` | Consulta un producto por id | 200 |
| GET | `/api/inventario/disponibilidad/{id}?cantidad=N` | ¿Hay N unidades? (por defecto N = 1) | 200 |
| POST | `/api/inventario/verificar` | Verifica existencias de un pedido completo | 200 |
| POST | `/api/inventario/descontar` | Descuenta existencias de los ítems del pedido | 200 |

### Ejemplo — Verificar un pedido

```bash
curl -X POST http://localhost:8082/api/inventario/verificar \
  -H "Content-Type: application/json" \
  -d '{"items":[{"productoId":1,"cantidad":2},{"productoId":3,"cantidad":20}]}'
```

Respuesta (200):

```json
{
  "todoDisponible": false,
  "items": [
    { "productoId": 1, "nombre": "Teclado mecánico", "cantidadSolicitada": 2,
      "stockActual": 25, "disponible": true, "mensaje": "Disponible" },
    { "productoId": 3, "nombre": "Monitor 24 pulgadas", "cantidadSolicitada": 20,
      "stockActual": 8, "disponible": false, "mensaje": "Stock insuficiente: hay 8 unidad(es)" }
  ]
}
```

**Reglas de verificación:**
- Productos repetidos: se suman las cantidades.
- Producto inexistente: `disponible: false` (no devuelve 404).
- Producto inactivo: no disponible aunque tenga stock.

### Campos del producto

| Campo | Tipo | Obligatorio | Reglas |
|---|---|---|---|
| id | Long | automático | autogenerado |
| codigo | texto | sí | único, máx. 30 caracteres |
| nombre | texto | sí | máx. 120 caracteres |
| descripcion | texto | no | máx. 255 caracteres |
| precio | decimal | sí | 12 dígitos, 2 decimales |
| stock | entero | sí | unidades disponibles |
| activo | booleano | sí | indica si se puede vender (por defecto `true`) |

### Datos de ejemplo

| id | codigo | nombre | stock | activo |
|---|---|---|---|---|
| 1 | PRD-001 | Teclado mecánico | 25 | sí |
| 2 | PRD-002 | Mouse inalámbrico | 40 | sí |
| 3 | PRD-003 | Monitor 24 pulgadas | 8 | sí |
| 4 | PRD-004 | Auriculares Bluetooth | 0 | sí |
| 5 | PRD-005 | Cámara web HD | 15 | no |

### Códigos de error

| Código | Cuándo ocurre |
|---|---|
| 400 | Cantidad < 1, JSON mal formado o lista `items` vacía |
| 404 | Producto no existe (consulta directa) o ruta inexistente |
| 500 | Error inesperado (sin detalles internos) |

Formato JSON de error: `timestamp`, `status`, `error`, `mensaje`.

### Pruebas

- **Unitarias (9):** `mvn test` (Mockito, **no necesitan MySQL**).
- **Integración manual (PI-01 a PI-13):** ejecutar `pruebas_inventario.bat` con el servicio corriendo.

| Prueba | Qué verifica |
|---|---|
| PI-01 | Listar productos → 200 con 5 productos |
| PI-02 | Consultar producto existente → 200 |
| PI-03 | Producto inexistente → 404 |
| PI-04 | Disponibilidad con stock suficiente → `disponible: true` |
| PI-05 | Stock insuficiente → `disponible: false` |
| PI-06 | Sin stock → `disponible: false` |
| PI-07 | Producto inactivo → `disponible: false` |
| PI-08 | Cantidad inválida → 400 |
| PI-09 | Verificar pedido completo → `todoDisponible: true` |
| PI-10 | Verificar con stock insuficiente → `todoDisponible: false` |
| PI-11 | Lista vacía → 400 |
| PI-12 | Producto inexistente en verificación → `disponible: false` |
| PI-13 | Ruta inexistente → 404 |

---

## Microservicio 3 — Procesamiento de pedidos

> **Carpeta:** `pedidos-service/`  
> **Puerto:** 8083  
> **BD:** `db_pedidos` (se crea sola, tablas `pedidos` y `pedido_items`)

Recibe un pedido, valida que el cliente exista (vía ESB → clientes), verifica el inventario (vía ESB → inventarios) y lo guarda como `CONFIRMADO` o `RECHAZADO`. **No genera facturas, no envía notificaciones ni descuenta stock.**

### Configuración

```bat
set "DB_USER=root"
set "DB_PASSWORD=tu_clave"
set "ESB_URL=http://localhost:8280"
```

| Propiedad | Valor por defecto | Para qué sirve |
|---|---|---|
| `esb.base-url` (`ESB_URL`) | `http://localhost:8280` | Dirección del ESB |
| `esb.connect-timeout` | `2s` | Tiempo máximo para conectar |
| `esb.read-timeout` | `5s` | Tiempo máximo para respuesta |

### Ejecución

Requiere que el ESB, clientes (8081) e inventarios (8082) estén corriendo.

```bash
mvn spring-boot:run
```

### Endpoints

| Método | URL | Qué hace | Código |
|---|---|---|---|
| POST | `/api/pedidos` | Crea un pedido (valida cliente, verifica inventario, guarda) | 201 |
| GET | `/api/pedidos/{id}` | Consulta un pedido | 200 |
| GET | `/api/pedidos` | Lista todos los pedidos | 200 |
| GET | `/api/pedidos?clienteId=N` | Lista pedidos de un cliente | 200 |

### Ejemplo — Crear un pedido

```bash
curl -X POST http://localhost:8083/api/pedidos \
  -H "Content-Type: application/json" \
  -d '{"clienteId":7,"items":[{"productoId":1,"cantidad":2},{"productoId":2,"cantidad":3}]}'
```

Respuesta confirmada (201):

```json
{
  "id": 15,
  "clienteId": 7,
  "estado": "CONFIRMADO",
  "motivoRechazo": null,
  "fechaCreacion": "2026-11-02T15:20:41",
  "items": [
    { "productoId": 1, "cantidad": 2 },
    { "productoId": 2, "cantidad": 3 }
  ]
}
```

Respuesta rechazada (201 — el pedido sí se registró):

```json
{
  "id": 16,
  "clienteId": 7,
  "estado": "RECHAZADO",
  "motivoRechazo": "Producto 3: Stock insuficiente: hay 8 unidad(es)",
  "fechaCreacion": "2026-11-02T15:22:10",
  "items": [
    { "productoId": 3, "cantidad": 20 }
  ]
}
```

### Campos del pedido (tabla `pedidos`)

| Campo | Tipo | Obligatorio | Reglas |
|---|---|---|---|
| id | Long | automático | autogenerado |
| clienteId | Long | sí | referencia al cliente; número positivo |
| estado | texto | automático | `CONFIRMADO` o `RECHAZADO` |
| motivoRechazo | texto | no | razón del rechazo, máx. 1000 caracteres; `null` si confirmado |
| fechaCreacion | fecha y hora | automático | se asigna al guardar |

### Campos de cada línea (tabla `pedido_items`)

| Campo | Tipo | Obligatorio | Reglas |
|---|---|---|---|
| id | Long | automático | autogenerado |
| pedido_id | Long | automático | pedido al que pertenece |
| productoId | Long | sí | referencia al producto; número positivo |
| cantidad | entero | sí | mínimo 1 |

### Códigos de error

| Código | Cuándo ocurre |
|---|---|
| 400 | Falta `clienteId`, lista `items` vacía, producto sin id o cantidad < 1 |
| 404 | Cliente no existe o pedido no encontrado |
| 503 | ESB o servicio no responde; el pedido **no** se guarda |
| 500 | Error inesperado (sin detalles internos) |

Formato JSON de error: `timestamp`, `status`, `error`, `message`.

### Mensajes que pedidos envía al ESB

| Petición | Respuesta esperada |
|---|---|
| `GET /api/clientes/{id}` | `200` (existe) o `404` (no existe) |
| `POST /api/inventario/verificar` | `{"todoDisponible":true\|false,"items":[...]}` |

### Reglas importantes

- Un pedido rechazado responde **201** (se registró; el resultado está en `estado`).
- Si el cliente no existe o los datos son inválidos, no se consulta inventario ni se guarda nada.
- Si el ESB no responde → **503** y el pedido no se guarda.
- Productos repetidos: pedidos los envía tal cual; inventarios suma las cantidades.
- Los errores nunca muestran detalles internos del sistema.

### Pruebas

- **Automatizadas:** `mvn test` (**no necesitan MySQL ni el ESB**; usan H2 y simulan el ESB).
- **Integración manual (PP-01 a PP-13):** ejecutar `pruebas_pedidos.bat` con todo levantado.

| Prueba | Qué verifica | Clase |
|---|---|---|
| PP-01 | Pedido confirmado (inventario suficiente) → 201 | `PedidoControllerTest` |
| PP-02 | Pedido rechazado (stock insuficiente) → 201 | `PedidoControllerTest` |
| PP-03 | Cliente inexistente → 404 | `PedidoControllerTest` |
| PP-04 | Lista de items vacía → 400 | `PedidoControllerTest` |
| PP-05 | Cantidad inválida → 400 | `PedidoControllerTest` |
| PP-06 | Sin clienteId → 400 | `PedidoControllerTest` |
| PP-07 | Producto inactivo → rechazado | `PedidoControllerTest` |
| PP-08 | Producto inexistente → rechazado | `PedidoControllerTest` |
| PP-09 | Producto repetido → inventarios suma cantidades | `PedidoControllerTest` |
| PP-10 | ESB caído → 503, pedido no se guarda | `PedidoControllerTest`, `EsbClientTest` |
| PP-11 | Consultar pedido existente → 200 | `PedidoControllerTest` |
| PP-12 | Consultar pedido inexistente → 404 | `PedidoControllerTest` |
| PP-13 | Listar por clienteId → 200 | `PedidoControllerTest` |

Además: `EsbClientTest` comprueba los mensajes intercambiados con el ESB y `PedidoServiceTest` la lógica con Mockito.

### Trazabilidad de requerimientos

| Requerimiento | Dónde se cumple | Prueba |
|---|---|---|
| RF-P01 Crear pedido | `PedidoController.crear`, `PedidoRequest` | PP-01 |
| RF-P02 Rechazar sin `clienteId` | `PedidoRequest` (`@NotNull`) | PP-06 |
| RF-P03 Rechazar lista vacía | `PedidoRequest` (`@NotEmpty`) | PP-04 |
| RF-P04 Rechazar cantidad < 1 | `ItemPedidoRequest` (`@Min(1)`) | PP-05 |
| RF-P05 Validar cliente por ESB | `EsbClient.clienteExiste` | PP-03 |
| RF-P06 Verificar inventario por ESB | `EsbClient.verificarInventario` | PP-01, PP-02 |
| RF-P07 Guardar CONFIRMADO | `Pedido.confirmado` | PP-01 |
| RF-P08 Guardar RECHAZADO con motivo | `Pedido.rechazado` | PP-02, PP-07, PP-08 |
| RF-P09 Id y fecha automáticos | `@GeneratedValue`, `@PrePersist` | PP-01 |
| RF-P10 Consultar por id | `GET /api/pedidos/{id}` | PP-11 |
| RF-P11 Error si no existe | `PedidoNoEncontradoException` | PP-12 |
| RF-P12 Listar todos o por cliente | `GET /api/pedidos[?clienteId=N]` | PP-13 |
| RF-P13 Error 503 si ESB no responde | `EsbNoDisponibleException` | PP-10 |
| RF-P14 Sin llamadas directas | Solo `EsbClient`, URL base única | `EsbClientTest` |

---

## Microservicio 4 — Facturación de pedidos (Responsable: Villa)

> **Carpeta:** `facturacion-service/`  
> **Puerto:** 8084  
> **BD:** `db_facturacion` (se crea sola)

Genera y consulta las facturas fiscales para los pedidos confirmados por el ESB. Controla montos válidos y evita facturación duplicada.

### Configuración
```bat
set "DB_USER=root"
set "DB_PASSWORD=tu_clave"
```

### Endpoints
| Método | URL | Qué hace | Código |
|---|---|---|---|
| POST | `/api/facturas` | Genera la factura para un pedido | 201 |
| GET | `/api/facturas/{id}` | Consulta una factura por su ID | 200 |
| GET | `/api/facturas/pedido/{pedidoId}` | Consulta la factura asociada a un pedido | 200 |
| GET | `/api/facturas` | Lista todas las facturas emitidas | 200 |

### Pruebas Unitarias
Ejecutar: `cd facturacion-service && mvn test` (6 pruebas con Mockito y H2, no requieren MySQL).

---

## Microservicio 5 — Notificaciones al cliente (Responsable: Villa)

> **Carpeta:** `notificaciones-service/`  
> **Puerto:** 8085  
> **BD:** Sin BD / Bitácora de auditoría en memoria

Procesa eventos de confirmación y rechazo enviados por el ESB. Simula el envío de manera estructurada en consola y ofrece integración SMTP configurable en `application.properties`.

### Configuración
```properties
notificaciones.smtp.habilitado=false    # true para envío real por correo
spring.mail.host=smtp.servidor.com
```

### Endpoints
| Método | URL | Qué hace | Código |
|---|---|---|---|
| POST | `/api/notificaciones/enviar` | Recibe evento y despacha notificación | 200 |
| GET | `/api/notificaciones` | Bitácora de auditoría de notificaciones enviadas | 200 |
| GET | `/api/notificaciones/{codigo}` | Consulta detalle por tracking | 200 |

### Pruebas Unitarias
Ejecutar: `cd notificaciones-service && mvn test` (4 pruebas JUnit 5).

---

## Frontend Dashboard Web (Responsable: Villa)

> **Archivo:** `frontend/index.html` (Single Page Application - SPA)

Interfaz moderna y responsiva construida con HTML5, Bootstrap 5 y JavaScript nativo (Fetch API):
- Se comunica directamente con el ESB en `http://localhost:8280`.
- **Clientes:** Registro y tabla de consulta.
- **Inventario:** Monitoreo en tiempo real de stock y precios de catálogo.
- **Creación de Pedidos:** Selector interactivo de cliente y productos con cálculo dinámico de subtotal.
- **Facturas y Notificaciones:** Visualización inmediata del estado (CONFIRMADO/RECHAZADO), facturas generadas y bitácora de mensajes enviados.

---

## ESB local para pruebas y orquestación

`pedidos-service/herramientas/EsbLocal.java` es un ESB mínimo **solo para pruebas locales** (no reemplaza a WSO2 ni a Camel). Recibe en el puerto 8280 y enruta según la ruta:

| Ruta | Puerto destino |
|---|---|
| `/api/clientes/**` | 8081 |
| `/api/inventario/**` | 8082 |
| `/api/pedidos/**` | 8083 |
| `/api/facturas/**` | 8084 |
| `/api/notificaciones/**` | 8085 |

Ejecutar (solo necesita Java 17+, sin Maven ni dependencias):

```bash
java pedidos-service/herramientas/EsbLocal.java
```

Si un servicio destino no responde, contesta 503.

---

## Flujo completo de un pedido

```
1. Cliente ──POST /api/pedidos──▸ ESB (:8280)
2. ESB ──enruta──▸ Pedidos (:8083)
3. Pedidos valida los datos. Si son inválidos → 400, fin.
4. Pedidos ──GET /api/clientes/{id}──▸ ESB ──▸ Clientes (:8081)
   Si el cliente no existe → 404, fin.
5. Pedidos ──POST /api/inventario/verificar──▸ ESB ──▸ Inventarios (:8082)
6. Si todoDisponible = true → CONFIRMADO; si false → RECHAZADO con motivo.
7. Pedidos guarda el pedido y responde 201.
8. (Futuro) El ESB envía el resultado a Facturación y/o Notificaciones.
```

---

## Cómo levantar todo el sistema

### Configuración Centralizada (.env)

El proyecto cuenta con un archivo `.env` en la raíz que centraliza toda la configuración (host, puertos, credenciales de MySQL y SMTP). Cada microservicio lo importa automáticamente al iniciar, y `ejecutar_todo.bat` lo carga en el entorno de ejecución:

```env
DB_HOST=localhost
DB_PORT=3306
DB_USER=root
DB_PASSWORD=root
ESB_PORT=8280
ESB_URL=http://localhost:8280
```

### Opción A — Ejecución Automática Centralizada (Recomendada)

Simplemente ejecuta en la terminal el script automatizado:

```cmd
ejecutar_todo.bat
```

Este script:
1. Lee automáticamente la configuración desde `.env`.
2. Abre y arranca automáticamente los 5 microservicios más el ESB en ventanas independientes:
   - Clientes (:8081)
   - Inventarios (:8082)
   - ESB Local (:8280)
   - Pedidos (:8083)
   - Facturación (:8084)
   - Notificaciones (:8085)
3. Abre automáticamente el Frontend Dashboard en el navegador (`frontend/index.html`).

### Opción B — Ejecución Manual Servicio por Servicio

Cada uno en su propia terminal. Tener MySQL corriendo.

```bash
# 1. Servicio de clientes (puerto 8081)
mvn spring-boot:run

# 2. Servicio de inventarios (puerto 8082)
cd inventario-service && mvn spring-boot:run

# 3. ESB local con orquestador (puerto 8280)
java pedidos-service/herramientas/EsbLocal.java

# 4. Servicio de pedidos (puerto 8083)
cd pedidos-service && mvn spring-boot:run

# 5. Servicio de facturación (puerto 8084)
cd facturacion-service && mvn spring-boot:run

# 6. Servicio de notificaciones (puerto 8085)
cd notificaciones-service && mvn spring-boot:run
```

Ejecutar las pruebas manuales:

```bash
inventario-service/pruebas_inventario.bat      # PI-01 a PI-13
pedidos-service/pruebas_pedidos.bat            # PP-01 a PP-13
pruebas_facturacion_notificaciones.bat         # PF-01..07, PN-01..04, PE-01..03
```

---

## Estructura completa del repositorio

```text
Servicio_clientes/                                 (Microservicio de clientes — raíz)
├── pom.xml                                        (Maven: Java 17, Spring Boot 3.3.5, MySQL, H2 test)
├── README.md                                      (Este archivo)
├── ejecutar_todo.bat                              (Script de inicio global para Windows)
├── pruebas_facturacion_notificaciones.bat         (Pruebas de Facturación, Notificaciones y ESB)
├── src/                                           (Microservicio 1: Clientes :8081)
│   ├── main/java/com/empresa/clientes/
│   └── test/java/com/empresa/clientes/
│
├── inventario-service/                            (Microservicio 2: Inventarios :8082)
│   ├── pom.xml
│   ├── pruebas_inventario.bat
│   └── src/
│
├── pedidos-service/                               (Microservicio 3: Pedidos :8083)
│   ├── pom.xml
│   ├── pruebas_pedidos.bat
│   ├── herramientas/EsbLocal.java                 (ESB Orquestador local en puerto 8280)
│   └── src/
│
├── facturacion-service/                           (Microservicio 4: Facturación :8084)
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/empresa/facturacion/
│       │   ├── FacturacionApplication.java
│       │   ├── controller/FacturaController.java
│       │   ├── service/FacturacionService.java
│       │   ├── repository/FacturaRepository.java
│       │   ├── model/ (Factura, EstadoFactura)
│       │   ├── dto/ (FacturaRequest, FacturaResponse, RespuestaError)
│       │   └── exception/ (GlobalExceptionHandler, Duplicada, NoEncontrada, MontoInvalido)
│       └── test/java/com/empresa/facturacion/
│           └── FacturacionServiceTest.java        (6 pruebas Mockito/JUnit 5)
│
├── notificaciones-service/                        (Microservicio 5: Notificaciones :8085)
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/empresa/notificaciones/
│       │   ├── NotificacionesApplication.java
│       │   ├── controller/NotificacionController.java
│       │   ├── service/NotificacionService.java   (Simulación Log estructurada + soporte SMTP)
│       │   ├── model/ (TipoNotificacion, NotificacionAudit)
│       │   ├── dto/ (NotificacionRequest, NotificacionResponse, RespuestaError)
│       │   └── exception/GlobalExceptionHandler.java
│       └── test/java/com/empresa/notificaciones/
│           └── NotificacionServiceTest.java       (4 pruebas JUnit 5)
│
└── frontend/                                      (Dashboard Web de Operación Centralizado)
    └── index.html                                 (SPA HTML5 + Bootstrap 5 + Fetch API directa al ESB)
```

---

## Alcance y funcionamiento

- **Descuento de stock en tiempo real:** Cuando un pedido queda en estado `CONFIRMADO`, el ESB orquesta automáticamente el descuento de unidades (`POST /api/inventario/descontar`) garantizando que las existencias se sincronicen de inmediato.
- **Facturación y Notificaciones:** completamente implementados y orquestados por el ESB (:8280) a partir del `estado` del pedido (CONFIRMADO genera factura y notificación; RECHAZADO genera notificación con causa).
- **El ESB local** (`EsbLocal.java`) provee enrutamiento transparente, soporte CORS, orquestación reactiva y lectura centralizada de `.env`.
