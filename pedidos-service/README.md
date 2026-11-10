# Servicio de procesamiento de pedidos

Es un microservicio REST (Representational State Transfer: arquitectura que usa HTTP para intercambiar datos) independiente para el "procesamiento de pedidos" del Proyecto 1: Integración de servicios de una empresa comercial mediante ESB (Enterprise Service Bus: intermediario que conecta servicios). Recibe un pedido, valida que el cliente exista, verifica el inventario y lo guarda como `CONFIRMADO` o `RECHAZADO`. **No genera facturas, no envía notificaciones y no descuenta stock**: eso lo hacen sus propios servicios, coordinados por el ESB.

## Tecnologías usadas
| Tecnología | Versión (según pom.xml) |
|---|---|
| Java | 17 |
| Spring Boot | 3.3.5 |
| Spring Web (incluye `RestClient` para llamar al ESB) | (gestionado por Spring Boot) |
| Spring Data JPA | (gestionado por Spring Boot) |
| Validation | (gestionado por Spring Boot) |
| MySQL Connector/J | mysql-connector-j (versión gestionada por Spring Boot) |
| H2 Database | (solo para pruebas) |
| Mockito / JUnit 5 | (solo para pruebas, vienen con spring-boot-starter-test) |
| Maven | (gestor de dependencias) |

## Requisitos previos
| Herramienta | Versión mínima |
|---|---|
| Java | JDK 17 o superior |
| Maven | 3.6+ |
| MySQL | 8.0+ |

## Configuración
Todo está en `src/main/resources/application.properties`.

### Base de datos
- La base de datos `db_pedidos` se crea sola si no existe (`createDatabaseIfNotExist=true`).
- Las tablas `pedidos` y `pedido_items` se crean/actualizan solas con `spring.jpa.hibernate.ddl-auto=update`.
- El usuario y la clave se leen de variables de entorno (la clave **no** tiene valor por defecto):
  - `DB_USER` (por defecto `root`)
  - `DB_PASSWORD` (por defecto vacía)
- **Aviso:** No se debe subir la clave real a GitHub. Usa variables de entorno.

Ejemplo en Windows (Símbolo del sistema), antes de ejecutar:
```bat
set "DB_USER=root"
set "DB_PASSWORD=tu_clave"
```

### ESB
| Propiedad | Valor por defecto | Para qué sirve |
|---|---|---|
| `esb.base-url` (variable `ESB_URL`) | `http://localhost:8280` | Única dirección que conoce pedidos |
| `esb.connect-timeout` | `2s` | Tiempo máximo para conectar con el ESB |
| `esb.read-timeout` | `5s` | Tiempo máximo para esperar la respuesta del ESB |

## Cómo ejecutar el servicio
1. Tener MySQL corriendo (puerto 3306) y las credenciales configuradas.
2. Tener el ESB corriendo (puerto 8280) con las rutas hacia clientes (8081) e inventarios (8082). Mientras no esté el ESB definitivo (WSO2 o Camel), se puede usar el ESB local de esta carpeta (ver más abajo).
3. Ejecutar:
```bash
mvn spring-boot:run
```
4. El servicio quedará en: `http://localhost:8083`

## Endpoints
| Método | URL | Qué hace | Código de respuesta exitosa |
|---|---|---|---|
| POST | `/api/pedidos` | Crea un pedido: valida el cliente, verifica el inventario y lo guarda confirmado o rechazado | 201 (Created) |
| GET | `/api/pedidos/{id}` | Consulta un pedido | 200 (OK) |
| GET | `/api/pedidos` | Lista todos los pedidos (lista vacía si no hay) | 200 (OK) |
| GET | `/api/pedidos?clienteId=N` | Lista los pedidos de un cliente | 200 (OK) |

## Ejemplos de uso con curl
### Crear un pedido
Petición (JSON):
```json
{
  "clienteId": 7,
  "items": [
    { "productoId": 1, "cantidad": 2 },
    { "productoId": 2, "cantidad": 3 }
  ]
}
```
Curl (Linux/Mac):
```bash
curl -X POST http://localhost:8083/api/pedidos \
  -H "Content-Type: application/json" \
  -d '{"clienteId":7,"items":[{"productoId":1,"cantidad":2},{"productoId":2,"cantidad":3}]}'
```
En Windows (Símbolo del sistema) las comillas del JSON van con `\"`:
```bat
curl -X POST http://localhost:8083/api/pedidos -H "Content-Type: application/json" -d "{\"clienteId\":7,\"items\":[{\"productoId\":1,\"cantidad\":2},{\"productoId\":2,\"cantidad\":3}]}"
```
Respuesta cuando hay inventario (201, pedido confirmado):
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
Respuesta cuando falta stock (201, pedido rechazado que queda registrado):
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
Si hay varios productos no disponibles, los motivos se separan con `; `.

### Consultar y listar
```bash
curl http://localhost:8083/api/pedidos/15
curl http://localhost:8083/api/pedidos
curl "http://localhost:8083/api/pedidos?clienteId=7"
```

### Respuesta de error (PP-03, cliente que no existe)
```json
{
  "timestamp": "2026-11-02T15:30:00",
  "status": 404,
  "error": "Not Found",
  "message": "El cliente con id 9999 no existe"
}
```

## Campos del pedido (tabla `pedidos`)
| Nombre | Tipo | Obligatorio | Reglas |
|---|---|---|---|
| id | Long | automático | autogenerado |
| clienteId | Long | sí | referencia al cliente (vive en `db_clientes`); número positivo |
| estado | texto | automático | `CONFIRMADO` o `RECHAZADO`, según la verificación de inventario |
| motivoRechazo | texto | no | razón del rechazo, máximo 1000 caracteres; `null` si se confirmó |
| fechaCreacion | fecha y hora | automático | se asigna sola al guardar |

## Campos de cada línea (tabla `pedido_items`)
| Nombre | Tipo | Obligatorio | Reglas |
|---|---|---|---|
| id | Long | automático | autogenerado |
| pedido_id | Long | automático | pedido al que pertenece la línea |
| productoId | Long | sí | referencia al producto (vive en `db_inventarios`); número positivo |
| cantidad | entero | sí | mínimo 1 |

## Códigos de error
| Código | Cuándo ocurre | Ejemplo de mensaje |
|---|---|---|
| 400 | Falta `clienteId`, lista `items` vacía, producto sin id o con cantidad menor a 1, JSON mal formado | "La cantidad debe ser mayor o igual a 1" |
| 404 | El cliente del pedido no existe, el pedido consultado no existe o la ruta no existe | "El cliente con id 9999 no existe" |
| 503 | El ESB (o un servicio detrás de él) no responde o responde algo inesperado; el pedido **no** se guarda | "El servicio no está disponible en este momento. Intente nuevamente más tarde" |
| 500 | Error inesperado | "Ocurrió un error interno. Intente nuevamente más tarde" (sin detalles internos ni stacktrace) |

Formato JSON de error siempre: `timestamp`, `status`, `error`, `message`.

## Diseño de integración
### Regla de comunicación
Los servicios nunca se llaman directamente entre sí (RF-07). El servicio de pedidos solo conoce la dirección del ESB (puerto 8280): le envía sus peticiones, y el ESB las reenvía al servicio de destino y devuelve la respuesta. Pedidos escucha en el puerto 8083 y es el único que accede a su base de datos `db_pedidos`. En el código, **toda** llamada saliente está en una sola clase: `esb/EsbClient.java`.

| Servicio | Puerto | Ruta base |
|---|---|---|
| ESB | 8280 | — |
| Clientes | 8081 | /api/clientes |
| Inventarios | 8082 | /api/inventario |
| Pedidos | 8083 | /api/pedidos |
| Facturación | 8084 | /api/facturas |
| Notificaciones | 8085 | /api/notificaciones |

### Quién se comunica con pedidos
| Componente | ¿Se comunica con pedidos? | Para qué |
|---|---|---|
| Consumidores (app web, Postman) | Sí, a través del ESB | Crear pedidos y consultarlos |
| Gestión de clientes | Sí, a través del ESB | Pedidos valida que el cliente exista (`GET /api/clientes/{id}`) |
| Consulta de inventarios | Sí, a través del ESB | Pedidos verifica las existencias antes de aceptar el pedido (`POST /api/inventario/verificar`) |
| Facturación | Sí, a través del ESB | El ESB le envía el pedido cuando queda CONFIRMADO |
| Notificaciones | Sí, a través del ESB | El ESB le envía el resultado (confirmado o rechazado) para avisar al cliente |
| ESB | Sí | Recibe las peticiones y las enruta al puerto 8083 |
| MySQL `db_pedidos` | Sí, solo pedidos | Guarda los pedidos y sus productos |

### Flujo de procesamiento de un pedido
1. El cliente envía un pedido al ESB (`POST /api/pedidos`).
2. El ESB lo enruta al servicio de pedidos (puerto 8083).
3. Pedidos valida los datos. Si son inválidos responde 400 y termina.
4. Pedidos llama al ESB con `GET /api/clientes/{id}`; el ESB lo enruta a clientes (puerto 8081). Si el cliente no existe, pedidos responde 404 y termina.
5. Pedidos llama al ESB con `POST /api/inventario/verificar`; el ESB lo enruta a inventarios (puerto 8082).
6. Si `todoDisponible` es `true`, pedidos guarda el pedido como `CONFIRMADO`. Si es `false`, lo guarda como `RECHAZADO` con el motivo.
7. Pedidos responde 201 con el pedido. A partir de su estado, el ESB lo envía a facturación y notificaciones (si fue confirmado) o solo a notificaciones para avisar al cliente (si fue rechazado).

### Protocolo y formato
- Protocolo: HTTP con REST.
- Formato de los mensajes: JSON, con codificación UTF-8.
- Códigos de estado: 201 (pedido procesado), 200 (consulta exitosa), 400 (datos inválidos), 404 (no existe), 503 (ESB o servicio no disponible) y 500 (error interno, sin detalles).

### Mensajes que pedidos envía al ESB (contratos de los otros servicios)
| Petición | Respuesta que pedidos entiende |
|---|---|
| `GET /api/clientes/{id}` | `200` (el cliente existe) o `404` (no existe). Del cuerpo no se usa nada. |
| `POST /api/inventario/verificar` con `{"items":[{"productoId":1,"cantidad":2}]}` | `{"todoDisponible":true\|false,"items":[...]}` |

De la respuesta de inventarios, pedidos solo usa `todoDisponible` y, por cada producto con `disponible: false`, su `productoId` y su `mensaje` (que forman el `motivoRechazo`). El resto de campos se ignora a propósito, así inventarios puede agregar campos sin romper a pedidos.

### Reglas importantes del contrato
- Un pedido rechazado por falta de stock responde **201**, porque el pedido sí se registró; el resultado está en el campo `estado`.
- Si el cliente no existe o los datos son inválidos, no se consulta el inventario ni se guarda nada.
- Si el ESB no responde (más de 2 s para conectar o 5 s para leer), pedidos responde **503** y no guarda el pedido, para no aceptar un pedido sin haber verificado el inventario. Se puede reintentar sin riesgo de duplicados. Lo mismo ocurre si, al consultar el cliente, el ESB responde algo distinto de 200 o 404; si, al verificar el inventario, responde con cualquier error; o si la respuesta de inventarios no trae `todoDisponible`.
- Si un producto viene repetido en el pedido, pedidos lo envía y lo guarda tal cual; es inventarios quien suma las cantidades al verificar.
- Los errores nunca muestran detalles internos del sistema (como fallos de la base de datos). El detalle solo queda en el log.
- El servicio no genera facturas, no envía notificaciones ni descuenta stock.

## Probar la comunicación completa con el ESB local
`herramientas/EsbLocal.java` es un ESB mínimo **solo para pruebas locales** (no reemplaza a WSO2 ni a Camel). Recibe en el puerto 8280, enruta según la ruta a cada servicio, devuelve la respuesta tal cual y muestra en consola cada paso. No necesita Maven ni dependencias, solo Java 17+:
```bash
java herramientas/EsbLocal.java
```
| Ruta | Puerto de destino |
|---|---|
| `/api/clientes/**` | 8081 |
| `/api/inventario/**` | 8082 |
| `/api/pedidos/**` | 8083 |
| `/api/facturas/**` | 8084 |
| `/api/notificaciones/**` | 8085 |

Si el servicio de destino no responde, el ESB local contesta 503 (como lo haría el ESB real), y pedidos lo traduce en su propio 503 sin guardar el pedido.

Orden para probar todo junto (cada uno en su propia terminal): MySQL, clientes (8081, rama `main`), inventarios (8082, rama `inventarios`), pedidos (8083), `EsbLocal` (8280) y al final `pruebas_pedidos.bat`. Este ESB local no envía el pedido a facturación ni a notificaciones: eso queda para el ESB definitivo.

## Pruebas
- Ejecutar todas: `mvn test` (**no necesitan MySQL ni el ESB**: usan H2 en memoria y simulan al ESB).
- Pruebas de integración manuales (PP-01 a PP-13), con el servicio, el ESB, clientes e inventarios corriendo: ejecutar `pruebas_pedidos.bat` (Windows). Muestra cada prueba con su código HTTP.

| Prueba | Qué verifica | Clase de prueba |
|---|---|---|
| PP-01 | Pedido con inventario suficiente (cliente 1, productos 1 x2 y 2 x3) -> 201, estado CONFIRMADO. | `PedidoControllerTest` |
| PP-02 | Pedido con stock insuficiente (producto 3, cantidad 20) -> 201, estado RECHAZADO con motivo. | `PedidoControllerTest` |
| PP-03 | Cliente que no existe (9999) -> 404 con mensaje claro; no consulta inventario. | `PedidoControllerTest` |
| PP-04 | Lista de productos vacía -> 400 con mensaje claro. | `PedidoControllerTest` |
| PP-05 | Cantidad inválida (0) -> 400. | `PedidoControllerTest` |
| PP-06 | Pedido sin cliente -> 400. | `PedidoControllerTest` |
| PP-07 | Producto inactivo (5) -> 201, estado RECHAZADO. | `PedidoControllerTest` |
| PP-08 | Producto que no existe (9999) -> 201, estado RECHAZADO. | `PedidoControllerTest` |
| PP-09 | Producto repetido en el pedido -> inventarios suma las cantidades. | `PedidoControllerTest` |
| PP-10 | ESB apagado/caído -> 503 sin detalles internos; el pedido no se guarda. | `PedidoControllerTest`, `EsbClientTest` (puerto cerrado y tiempo de lectura reales) |
| PP-11 | Consultar pedido que existe -> 200 con los datos. | `PedidoControllerTest` |
| PP-12 | Consultar pedido que no existe (9999) -> 404 con mensaje claro. | `PedidoControllerTest` |
| PP-13 | Listar pedidos de un cliente -> 200 con sus pedidos. | `PedidoControllerTest` |

Además: `EsbClientTest` comprueba los mensajes que se intercambian con el ESB (URL, método, JSON enviado y cómo se interpretan las respuestas de clientes e inventarios) y `PedidoServiceTest` comprueba la lógica con Mockito.

## Trazabilidad de requerimientos
| Requerimiento | Dónde se cumple | Prueba |
|---|---|---|
| RF-P01 Crear pedido | `PedidoController.crear`, `PedidoRequest` | PP-01 |
| RF-P02 Rechazar sin `clienteId` | `PedidoRequest` (`@NotNull`) | PP-06 |
| RF-P03 Rechazar lista vacía | `PedidoRequest` (`@NotEmpty`) | PP-04 |
| RF-P04 Rechazar producto sin id o cantidad < 1 | `ItemPedidoRequest` (`@NotNull`, `@Min(1)`) | PP-05 |
| RF-P05 Validar cliente por el ESB | `EsbClient.clienteExiste`, `ClienteNoExisteException` | PP-03 |
| RF-P06 Verificar inventario por el ESB | `EsbClient.verificarInventario` | PP-01, PP-02 |
| RF-P07 Guardar CONFIRMADO | `Pedido.confirmado` | PP-01 |
| RF-P08 Guardar RECHAZADO con motivo | `Pedido.rechazado`, `PedidoService.construirMotivo` | PP-02, PP-07, PP-08 |
| RF-P09 Id y fecha automáticos | `@GeneratedValue`, `@PrePersist` | PP-01 |
| RF-P10 Consultar por id | `GET /api/pedidos/{id}` | PP-11 |
| RF-P11 Error claro si no existe | `PedidoNoEncontradoException` | PP-12 |
| RF-P12 Listar todos o por cliente | `GET /api/pedidos[?clienteId=N]` | PP-13 |
| RF-P13 Error si el ESB no responde y no guardar | `EsbNoDisponibleException` -> 503 | PP-10 |
| RF-P14 Sin llamadas directas a otros servicios | Solo `EsbClient`, con una única URL base | `EsbClientTest` |

## Estructura del proyecto
```text
pedidos-service/
├── pom.xml
├── README.md
├── .gitignore
├── .gitattributes                           (Mantiene los saltos de línea de Windows en el .bat)
├── pruebas_pedidos.bat                      (Ejecuta PP-01 a PP-13 con curl)
├── herramientas
│   └── EsbLocal.java                        (ESB mínimo para pruebas locales, puerto 8280)
└── src
    ├── main
    │   ├── java/com/empresa/pedidos
    │   │   ├── PedidosApplication.java            (Clase principal Spring Boot)
    │   │   ├── controller/PedidoController.java   (Expone los 3 endpoints REST)
    │   │   ├── service/PedidoService.java         (Flujo: validar cliente, verificar inventario, guardar)
    │   │   ├── repository/PedidoRepository.java   (Acceso a datos con JpaRepository)
    │   │   ├── model/                             (Entidades JPA: Pedido, PedidoItem y EstadoPedido)
    │   │   ├── dto/                               (Peticiones y respuestas JSON)
    │   │   ├── esb/                               (Toda la comunicación con el ESB)
    │   │   │   ├── EsbClient.java                 (Llama a clientes e inventarios a través del ESB)
    │   │   │   ├── EsbClientConfig.java           (Cliente HTTP con los tiempos máximos)
    │   │   │   └── EsbProperties.java             (Dirección y tiempos del ESB)
    │   │   └── exception/                         (Excepciones propias y GlobalExceptionHandler)
    │   └── resources
    │       └── application.properties             (Config MySQL y ESB, usuario/clave por variables de entorno)
    └── test
        ├── java/com/empresa/pedidos
        │   ├── controller/PedidoControllerTest.java  (PP-01 a PP-13 sobre HTTP, con H2)
        │   ├── esb/EsbClientTest.java                (Mensajes con el ESB, ESB apagado y lento)
        │   └── service/PedidoServiceTest.java        (Lógica con Mockito)
        └── resources
            └── application-test.properties        (Perfil de pruebas: H2 en lugar de MySQL)
```

## Alcance y límites conocidos
- Este servicio **no descuenta stock** (inventarios solo consulta). Por eso no hay reserva de existencias: dos pedidos simultáneos podrían ser confirmados sobre el mismo stock. Queda fuera del alcance del taller.
- Facturación y notificaciones los activa el ESB a partir del `estado` del pedido; este servicio no las llama.
