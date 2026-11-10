# Servicio de consulta de inventarios

Es un microservicio REST (Representational State Transfer: arquitectura que usa HTTP para intercambiar datos) independiente para la "consulta de inventarios" del Proyecto 1: Integración de servicios de una empresa comercial mediante ESB (Enterprise Service Bus: intermediario que conecta servicios). Solo consulta productos, su disponibilidad y verifica las existencias de un pedido; **no descuenta stock**.

## Tecnologías usadas
| Tecnología | Versión (según pom.xml) |
|---|---|
| Java | 17 |
| Spring Boot | 3.3.5 |
| Spring Web | (gestionado por Spring Boot) |
| Spring Data JPA | (gestionado por Spring Boot) |
| Validation | (gestionado por Spring Boot) |
| MySQL Connector/J | mysql-connector-j (versión gestionada por Spring Boot) |
| Mockito / JUnit 5 | (solo para pruebas, vienen con spring-boot-starter-test) |
| Maven | (gestor de dependencias) |

## Requisitos previos
| Herramienta | Versión mínima |
|---|---|
| Java | JDK 17 o superior |
| Maven | 3.6+ |
| MySQL | 8.0+ |

## Configuración de la base de datos
- La base de datos `db_inventarios` se crea sola si no existe (`createDatabaseIfNotExist=true`).
- La tabla `producto` se crea/actualiza sola con `spring.jpa.hibernate.ddl-auto=update`.
- Al arrancar se cargan 5 productos de ejemplo desde `src/main/resources/data.sql` (`INSERT IGNORE`, no se duplican al reiniciar).
- En `src/main/resources/application.properties` el usuario y la clave se leen de variables de entorno, con estos valores por defecto:
  - `DB_USER` (por defecto `root`)
  - `DB_PASSWORD` (por defecto `root`)
- **Aviso:** No se debe subir la clave real a GitHub. Usa variables de entorno.

Ejemplo en Windows (Símbolo del sistema), antes de ejecutar:
```bat
set "DB_USER=root"
set "DB_PASSWORD=tu_clave"
```

## Cómo ejecutar el servicio
1. Tener MySQL corriendo (puerto 3306) y las credenciales configuradas.
2. Ejecutar:
```bash
mvn spring-boot:run
```
3. El servicio quedará en: `http://localhost:8082`

## Endpoints
| Método | URL | Qué hace | Código de respuesta exitosa |
|---|---|---|---|
| GET | `/api/inventario/productos` | Lista los productos con su stock | 200 (OK) |
| GET | `/api/inventario/productos/{id}` | Consulta un producto por id | 200 (OK) |
| GET | `/api/inventario/disponibilidad/{id}?cantidad=N` | Indica si hay N unidades (por defecto N = 1) | 200 (OK) |
| POST | `/api/inventario/verificar` | Verifica las existencias de todos los productos de un pedido | 200 (OK) |

## Ejemplos de uso con curl
### Listar productos
```bash
curl http://localhost:8082/api/inventario/productos
```

### Consultar disponibilidad
```bash
curl "http://localhost:8082/api/inventario/disponibilidad/1?cantidad=5"
```
Respuesta (200):
```json
{
  "productoId": 1,
  "nombre": "Teclado mecánico",
  "cantidadSolicitada": 5,
  "stockActual": 25,
  "disponible": true,
  "mensaje": "Disponible"
}
```

### Verificar un pedido
Petición (JSON):
```json
{
  "items": [
    { "productoId": 1, "cantidad": 2 },
    { "productoId": 3, "cantidad": 20 }
  ]
}
```
Curl (Linux/Mac):
```bash
curl -X POST http://localhost:8082/api/inventario/verificar \
  -H "Content-Type: application/json" \
  -d '{"items":[{"productoId":1,"cantidad":2},{"productoId":3,"cantidad":20}]}'
```
En Windows (Símbolo del sistema) las comillas del JSON van con `\"`:
```bat
curl -X POST http://localhost:8082/api/inventario/verificar -H "Content-Type: application/json" -d "{\"items\":[{\"productoId\":1,\"cantidad\":2},{\"productoId\":3,\"cantidad\":20}]}"
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
Reglas de la verificación:
- Si un producto aparece repetido, se suman las cantidades.
- Un producto que no existe se reporta como `disponible: false` ("El producto no existe") en lugar de devolver 404, para que pedidos reciba el resultado completo en una sola respuesta.
- Un producto inactivo se reporta como no disponible, aunque tenga stock.

## Campos del producto
| Nombre | Tipo | Obligatorio | Reglas |
|---|---|---|---|
| id | Long | automático | autogenerado |
| codigo | texto | sí | único, máximo 30 caracteres |
| nombre | texto | sí | máximo 120 caracteres |
| descripcion | texto | no | máximo 255 caracteres |
| precio | decimal | sí | 12 dígitos, 2 decimales |
| stock | entero | sí | unidades disponibles |
| activo | booleano | sí | indica si el producto se puede vender (por defecto true) |

## Datos de ejemplo
| id | codigo | nombre | stock | activo |
|---|---|---|---|---|
| 1 | PRD-001 | Teclado mecánico | 25 | sí |
| 2 | PRD-002 | Mouse inalámbrico | 40 | sí |
| 3 | PRD-003 | Monitor 24 pulgadas | 8 | sí |
| 4 | PRD-004 | Auriculares Bluetooth | 0 | sí |
| 5 | PRD-005 | Cámara web HD | 15 | no |

## Códigos de error
| Código | Cuándo ocurre | Ejemplo de mensaje |
|---|---|---|
| 400 | Cantidad menor a 1, JSON mal formado o lista `items` vacía | "La cantidad debe ser mayor o igual a 1" |
| 404 | Producto no existe al consultarlo por id, o ruta inexistente | "No existe un producto con id 9999" |
| 500 | Error inesperado | "Ocurrió un error interno. Intente nuevamente más tarde" (sin detalles internos ni stacktrace) |

Formato JSON de error siempre: `timestamp`, `status`, `error`, `mensaje`.

## Pruebas
- Pruebas unitarias: `mvn test` (9 pruebas con Mockito sobre la capa de servicio; **no necesitan MySQL**).
- Pruebas de integración manuales (PI-01 a PI-13), con el servicio corriendo: ejecutar `pruebas_inventario.bat` (Windows). Muestra cada prueba con su código HTTP.

| Prueba | Qué verifica |
|---|---|
| PI-01 | Listar productos -> 200 con 5 productos. |
| PI-02 | Consultar producto existente (id 1) -> 200 con sus datos. |
| PI-03 | Consultar producto inexistente (id 9999) -> 404 con mensaje claro. |
| PI-04 | Disponibilidad con stock suficiente (id 1, cantidad 5) -> 200, disponible: true. |
| PI-05 | Disponibilidad con stock insuficiente (id 3, cantidad 20) -> 200, disponible: false. |
| PI-06 | Producto sin stock (id 4, cantidad 1) -> 200, disponible: false. |
| PI-07 | Producto inactivo (id 5, cantidad 1) -> 200, disponible: false. |
| PI-08 | Cantidad inválida (id 1, cantidad 0) -> 400. |
| PI-09 | Verificar pedido completo (ids 1 y 2) -> 200, todoDisponible: true. |
| PI-10 | Verificar con un producto sin stock (ids 1 y 3, cantidad 20) -> 200, todoDisponible: false. |
| PI-11 | Verificar con lista vacía -> 400. |
| PI-12 | Verificar con producto inexistente (id 9999) -> 200, disponible: false. |
| PI-13 | Ruta inexistente -> 404 con mensaje claro. |

## Estructura del proyecto
```text
inventario-service/
├── pom.xml
├── README.md
├── .gitignore
├── pruebas_inventario.bat                   (Ejecuta PI-01 a PI-13 con curl)
└── src
    ├── main
    │   ├── java/com/empresa/inventario
    │   │   ├── InventarioApplication.java          (Clase principal Spring Boot)
    │   │   ├── controller/InventarioController.java (Expone los 4 endpoints REST)
    │   │   ├── service/InventarioService.java       (Lógica de disponibilidad y verificación)
    │   │   ├── repository/ProductoRepository.java   (Acceso a datos con JpaRepository)
    │   │   ├── model/Producto.java                  (Entidad JPA mapeada a tabla producto)
    │   │   ├── dto/                                 (Peticiones y respuestas JSON)
    │   │   └── exception/                           (Excepciones propias y GlobalExceptionHandler)
    │   └── resources
    │       ├── application.properties               (Config MySQL, usuario/clave por variables de entorno)
    │       └── data.sql                             (5 productos de ejemplo)
    └── test
        └── java/com/empresa/inventario/service
            └── InventarioServiceTest.java           (9 pruebas con Mockito)
```

## Cómo se conecta con el ESB
**Regla (RF-07):** ningún servicio llama directamente a otro. Todo pasa por el ESB (WSO2 o Apache Camel, puerto 8280 propuesto).

El ESB debe llamar al servicio REST en `http://localhost:8082/api/inventario` usando **REST** (HTTP) con **JSON**.

| Componente | Relación con el servicio de inventarios |
|---|---|
| ESB | Recibe las peticiones REST/JSON, las enruta al puerto 8082 y devuelve la respuesta. |
| Servicio de pedidos | Es el cliente principal. Antes de aceptar un pedido llama (vía ESB) a `POST /api/inventario/verificar`; si `todoDisponible` es `false`, rechaza el pedido. |
| MySQL (db_inventarios) | Solo este servicio accede a su base de datos. |
| Clientes, Facturación, Notificaciones | No se comunican con inventarios. |
