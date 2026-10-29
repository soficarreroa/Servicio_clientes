# Servicio de gestión de clientes

Es un microservicio REST (Representational State Transfer: arquitectura que usa HTTP para intercambiar datos) independiente para la "gestión de clientes" del Proyecto 1: Integración de servicios de una empresa comercial mediante ESB (Enterprise Service Bus: intermediario que conecta servicios). Solo registra y consulta clientes.

## Tecnologías usadas
| Tecnología | Versión (según pom.xml) |
|---|---|
| Java | 17 |
| Spring Boot | 3.3.5 |
| Spring Web | (gestionado por Spring Boot) |
| Spring Data JPA | (gestionado por Spring Boot) |
| Validation | (gestionado por Spring Boot) |
| MySQL Connector/J | mysql-connector-j (versión gestionada por Spring Boot) |
| H2 Database | (solo para pruebas) |
| Maven | (gestor de dependencias) |

## Requisitos previos
| Herramienta | Versión mínima |
|---|---|
| Java | JDK 17 o superior |
| Maven | 3.6+ |
| MySQL | 8.0+ |

## Configuración de la base de datos
- La base de datos `clientes_db` se crea sola si no existe (`createDatabaseIfNotExist=true`).
- La tabla `clientes` se crea/actualiza sola con `spring.jpa.hibernate.ddl-auto=update`.
- En `src/main/resources/application.properties` hay que cambiar **dos líneas**:
  - `spring.datasource.username=root` (cambiar por tu usuario)
  - `spring.datasource.password=tu_clave` (cambiar por tu clave)
- **Aviso:** No se debe subir la clave real a GitHub. Usa `tu_clave` o variables de entorno.

## Cómo ejecutar el servicio
1. Configurar MySQL y las credenciales en `application.properties`.
2. Ejecutar:
```bash
mvn spring-boot:run
```
3. El servicio quedará en: `http://localhost:8081`

## Endpoints
| Método | URL | Qué hace | Código de respuesta exitosa |
|---|---|---|---|
| POST | `/api/clientes` | Registra un cliente | 201 (Created) |
| GET | `/api/clientes/{id}` | Consulta un cliente por id | 200 (OK) |
| GET | `/api/clientes` | Lista todos los clientes | 200 (OK) (lista vacía si no hay) |

## Ejemplos de uso con curl
### Registrar un cliente
Petición (JSON):
```json
{
  "documento": "1020304050",
  "nombre": "Laura",
  "apellido": "Gómez",
  "email": "laura.gomez@correo.com",
  "telefono": "3001234567",
  "direccion": "Calle 10 # 5-20, Bogotá"
}
```
Curl:
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

### Consultar por id
```bash
curl -X GET http://localhost:8081/api/clientes/1
```

### Listar
```bash
curl -X GET http://localhost:8081/api/clientes
```

## Campos del cliente
| Nombre | Tipo | Obligatorio | Reglas |
|---|---|---|---|
| id | Long | automático | autogenerado |
| documento | texto | sí | único, máximo 30 caracteres |
| nombre | texto | sí | máximo 80 caracteres |
| apellido | texto | sí | máximo 80 caracteres |
| email | texto | sí | formato válido, único, máximo 120, se guarda en minúsculas |
| telefono | texto | no | solo números, espacios o +, máximo 20 |
| direccion | texto | no | máximo 200 caracteres |
| fechaRegistro | fecha y hora | automático | se asigna sola al guardar |

## Códigos de error
| Código | Cuándo ocurre | Ejemplo de mensaje |
|---|---|---|
| 400 | Datos faltantes o inválidos (falta campo obligatorio o email con formato inválido) | "El campo nombre es obligatorio" |
| 404 | Cliente no existe al consultar por id | "No existe un cliente con id X" |
| 409 | Documento repetido o email repetido al registrar | "Ya existe un cliente con ese documento" o "Ya existe un cliente con ese email" |
| 500 | Error inesperado | "Ocurrió un error interno" (sin detalles internos ni stacktrace) |

Formato JSON de error siempre: `fecha`, `estado`, `error`, `mensaje`.

## Pruebas
- Ejecutar: `mvn test`
- Las pruebas usan **H2 en memoria** (solo para pruebas), por lo que **no necesitan MySQL**.
- Lista de pruebas (P-01 a P-10):

| Prueba | Qué verifica |
|---|---|
| P-01 | Registrar cliente con datos correctos -> 201 y el cliente con su id. |
| P-02 | Registrar sin campos opcionales (solo documento, nombre, apellido, email) -> 201. |
| P-03 | Registrar sin nombre -> 400 con mensaje claro. |
| P-04 | Registrar con email mal escrito -> 400. |
| P-05 | Registrar con documento repetido -> 409. |
| P-06 | Registrar con email repetido -> 409. |
| P-07 | Consultar cliente que existe -> 200 con los datos. |
| P-08 | Consultar cliente que no existe (id 9999) -> 404 con mensaje claro. |
| P-09 | Listar clientes con varios registrados -> 200 con la lista completa. |
| P-10 | Listar sin clientes -> 200 con lista vacía. |

## Estructura del proyecto
```text
servicio-clientes/
├── pom.xml
├── README.md
├── .gitignore
└── src
    ├── main
    │   ├── java/com/empresa/clientes
    │   │   ├── ServicioClientesApplication.java  (Clase principal Spring Boot)
    │   │   ├── controller/ClienteController.java (Expone los 3 endpoints REST)
    │   │   ├── service/ClienteService.java       (Lógica de negocio y validación de duplicados)
    │   │   ├── repository/ClienteRepository.java (Acceso a datos con JpaRepository)
    │   │   ├── model/Cliente.java                (Entidad JPA mapeada a tabla clientes)
    │   │   ├── dto/ClienteRequest.java           (DTO con validaciones)
    │   │   └── exception/                        (Excepciones propias y ManejadorErrores)
    │   │       ├── ClienteNoEncontradoException.java
    │   │       ├── ClienteDuplicadoException.java
    │   │       └── ManejadorErrores.java         (@RestControllerAdvice para errores JSON)
    │   └── resources
    │       └── application.properties           (Config MySQL - cambiar usuario/clave)
    └── test
        ├── java/com/empresa/clientes
        │   └── ClienteControllerTest.java       (10 pruebas con MockMvc + @SpringBootTest)
        └── resources
            └── application.properties           (Config H2 para pruebas)
```

## Cómo se conecta con el ESB
El ESB (WSO2 o Apache Camel) debe llamar al servicio REST en `http://localhost:8081/api/clientes` usando **REST** (HTTP) con **JSON**. Puede hacer peticiones POST para registrar y GET para consultar/listar, y debe tratar las respuestas con sus códigos HTTP y el formato JSON de error cuando aplique.
