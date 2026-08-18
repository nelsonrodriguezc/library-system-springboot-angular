**🌐 Idioma:** [English](README.md) · **Español**

# Libris — Sistema de préstamos para una biblioteca

Reemplaza la planilla Excel de préstamos por una aplicación real: catálogo con búsqueda,
préstamos con reglas de negocio (atrasos, bloqueos, lista de espera), avisos por correo
antes de que venza un préstamo y autocompletado de fichas desde Open Library con solo el ISBN.

<p align="center">
  <img alt="Java 21" src="https://img.shields.io/badge/Java-21-orange">
  <img alt="Spring Boot 3.5" src="https://img.shields.io/badge/Spring%20Boot-3.5-6DB33F">
  <img alt="Angular 21" src="https://img.shields.io/badge/Angular-21-DD0031">
  <img alt="PostgreSQL 16" src="https://img.shields.io/badge/PostgreSQL-16-336791">
</p>

---

## 1. Arrancar todo

Necesitas solo **Docker**. No hace falta JDK, Maven ni Node instalados.

```bash
docker compose up --build
```

| Qué | Dónde |
|---|---|
| Aplicación | http://localhost:4200 |
| API + Swagger UI | http://localhost:8080/swagger-ui.html |
| Buzón de correo (MailHog) | http://localhost:8025 |
| Salud del backend | http://localhost:8080/actuator/health |

La primera vez tarda unos minutos (compila el backend y el frontend dentro de las imágenes).
Para detener todo y borrar los datos: `docker compose down -v`.

### Cuentas de prueba

Se crean con las migraciones. Son credenciales desechables para una base de datos
desechable; **no hay ningún secreto real en el repositorio**.

| Correo | Contraseña | Rol |
|---|---|---|
| `admin@libris.cl` | `Admin123!` | ADMIN |
| `bibliotecario@libris.cl` | `Biblio123!` | BIBLIOTECARIO |
| `lector@libris.cl` | `Demo123!` | BIBLIOTECARIO |

Con el perfil `demo` (activo por defecto) se cargan además préstamos, devoluciones tardías,
reservas y una cuenta bloqueada, para que los paneles no salgan vacíos.

### Ver los correos

Todo lo que la aplicación envía queda en **MailHog**: http://localhost:8025

Para no esperar al cron de las 08:00, un ADMIN puede disparar las tareas a mano desde
**Administración → Resumen**, o por API:

```bash
curl -X POST http://localhost:8080/api/admin/notifications/due-soon-reminders \
  -H "Authorization: Bearer $TOKEN"
```

---

## 2. Recorrido de 5 minutos

1. Entra como **admin@libris.cl / Admin123!**
2. **Catálogo → Agregar libro** → ISBN `9780321356680` → *Buscar información*.
   Se completa solo: *Effective Java*, Joshua Bloch, 2008, con portada y temas.
3. Confirma. El libro queda disponible.
4. Pídelo prestado → llega el **correo de confirmación** a MailHog.
5. Abre otra sesión con `lector@libris.cl` y **reserva** ese mismo libro (ya está prestado).
6. Devuélvelo desde la primera sesión → el ejemplar queda **RESERVADO** y al lector le llega
   el aviso de *libro disponible*.
7. En **Administración → Resumen**, pulsa *Enviar recordatorios* y revisa MailHog.

---

## 3. Arquitectura

```
backend/    API REST en Spring Boot (Java 21)
frontend/   Aplicación Angular 21 (standalone + signals)
docs/       Modelo de datos y colección Postman
```

📐 **Diagrama entidad-relación, restricciones y consultas de ejemplo:**
[`docs/data-model.es.md`](docs/data-model.es.md)

### Backend: paquetes por funcionalidad

```
com.libris
├── auth/           Registro, login, JWT
├── book/           Catálogo, ISBN
│   ├── metadata/   Puerto hacia catálogos externos + adaptadores de Open Library
│   └── recommendation/  Recomendador content-based
├── loan/           Préstamos
│   ├── policy/     Cálculo de fecha límite y estado derivado
│   └── rule/       Reglas de elegibilidad, una clase por regla
├── reservation/    Lista de espera
├── user/           Cuentas, política de bloqueo, administración
├── stats/          Estadísticas del panel
├── notification/   Eventos, plantillas, envío y tareas programadas
└── shared/         Excepciones de negocio y forma única de error
```

### Cómo se aplica SOLID

No como etiqueta: cada principio resolvió un problema concreto de este sistema.

**SRP — una razón para cambiar.**
`DueDateCalculator` solo calcula `préstamo + 14 días`. `OverdueBlockPolicy` solo decide si
una devolución tardía cuesta el bloqueo. `LoanStatusResolver` solo traduce un préstamo al
estado que ve la interfaz. `LoanService` no contiene ninguna regla: orquesta y abre la
transacción. Los controladores no tienen lógica de negocio.

**OCP — abierto a extensión, cerrado a modificación.**
Las condiciones para prestar son beans que implementan
[`LoanRule`](backend/src/main/java/com/libris/loan/rule/LoanRule.java); Spring los inyecta
como lista ordenada. Agregar una regla es agregar una clase: `LoanService` no cambia.
La prueba está en el historial — la regla de la lista de espera
([`ReservationHolderRule`](backend/src/main/java/com/libris/reservation/ReservationHolderRule.java))
la aporta el módulo de reservas y se enchufa sin tocar el de préstamos.

**LSP — sustituibles de verdad.**
[`BookMetadataSource`](backend/src/main/java/com/libris/book/metadata/BookMetadataSource.java)
tiene un contrato total: **nunca lanza excepciones**. Caída, timeout, 429 o ISBN desconocido
producen el mismo `Optional.empty()`. Por eso las dos implementaciones de Open Library son
intercambiables y el catálogo sigue funcionando cuando el tercero no.

**ISP — interfaces del tamaño de quien las usa.**
En vez de un `NotificationService` con cinco métodos, hay tres puertos:
[`LoanNotifier`](backend/src/main/java/com/libris/notification/port/LoanNotifier.java),
`AccountNotifier` y `ReservationNotifier`. Las tareas programadas dependen solo del primero.

**DIP — depender de abstracciones.**
El dominio depende de `EmailSender`, `TemplateRenderer`, `BookMetadataProvider` y de un
`Clock` inyectado. Ninguna regla importa `JavaMailSender`, `RestClient` ni llama a
`LocalDate.now()`; por eso los tests fijan el tiempo con `Clock.fixed` y verifican la
ventana de 90 días sin depender del día en que se ejecuten.

---

## 4. Reglas de negocio

| Regla | Dónde vive | Error que devuelve |
|---|---|---|
| El ejemplar debe estar en estantería | `BookAvailableRule` | `409 BOOK_NOT_AVAILABLE` |
| Un ejemplar retenido es solo para su titular | `ReservationHolderRule` | `409 BOOK_RESERVED_FOR_ANOTHER_USER` |
| La cuenta no puede estar bloqueada | `BorrowerNotBlockedRule` | `409 USER_BLOCKED` |
| Máximo 3 préstamos activos | `MaxActiveLoansRule` | `409 MAX_ACTIVE_LOANS` |
| ISBN único | `BookService` + índice único | `409 DUPLICATE_ISBN` |
| Solo se elimina lo disponible | `BookService` | `409 BOOK_NOT_DELETABLE` |

**Al prestar:** el libro pasa a `PRESTADO`, la fecha límite se fija a 14 días y se publica un
evento que envía el correo de confirmación fuera del hilo HTTP.

**Al devolver:** si vuelve tarde, se registra un atraso. Al **tercer atraso en 90 días** la
cuenta queda bloqueada **una semana** y recibe el aviso; un ADMIN puede levantarlo antes.
Si alguien esperaba ese título, el ejemplar queda `RESERVADO` para el primero de la fila y se
le avisa por correo, en vez de volver a `DISPONIBLE`.

El bloqueo se guarda como `blocked_until` (marca de tiempo), no como un booleano: **expira
solo**, sin necesidad de una tarea que lo limpie.

---

## 5. Correo

Cinco plantillas Thymeleaf en
[`templates/email/`](backend/src/main/resources/templates/email), sobre un layout compartido
con tablas y estilos en línea (los clientes de correo siguen sin soportar flexbox):

| Plantilla | Cuándo |
|---|---|
| `loan-confirmation` | al registrar el préstamo |
| `due-soon-reminder` | tarea diaria, 2 días antes del vencimiento |
| `overdue-notice` | tarea diaria, préstamo ya vencido |
| `account-blocked` | al llegar al tercer atraso |
| `book-available` | al liberarse un título que alguien esperaba |

El envío **nunca bloquea la petición HTTP**:

```java
@Async(AsyncConfig.NOTIFICATION_EXECUTOR)
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public void onLoanCreated(LoanCreatedEvent event) { ... }
```

`AFTER_COMMIT` garantiza que ningún correo describa un préstamo que terminó revertido, y
`@Async` sobre un pool propio evita que un SMTP lento consuma los hilos del servidor web.

Las dos tareas son **idempotentes**: marcan `reminder_sent_at` / `overdue_notice_sent_at` al
enviar, así que ejecutarlas dos veces el mismo día no duplica avisos.

---

## 6. Integración con Open Library

`GET /api/books/lookup/{isbn}` previsualiza sin guardar nada, y `POST /api/books` acepta
**solo el ISBN** y completa el resto.

**Dos fuentes tras el mismo puerto.** El enunciado ilustra la integración con
`/isbn/{isbn}.json`. Ese endpoint devuelve la *edición*: trae título, año y portada, pero **no
el autor ni los temas**, que viven en el *work* y costarían tres o cuatro llamadas encadenadas.
Por eso la fuente principal es `/api/books?jscmd=data`, que responde todo en una sola llamada,
y el endpoint del enunciado queda como respaldo. Ambas implementan
`BookMetadataSource` y se consultan en orden.

**Detalles que costó descubrir y están resueltos:**

- `/isbn/{isbn}.json` responde **302** hacia la URL canónica → el cliente sigue redirecciones.
- Open Library **limita el tráfico anónimo** → se envía un `User-Agent` propio.
- Entre los temas vienen **códigos de estantería** (`Qa76.73.j38`, `005.13/3`), que ensucian
  los filtros y falsean las recomendaciones →
  [`SubjectSanitizer`](backend/src/main/java/com/libris/book/metadata/SubjectSanitizer.java)
  los descarta.

**Caché.** `@Cacheable` sobre Caffeine (24 h, 1.000 entradas). Los resultados **vacíos no se
cachean**: un fallo momentáneo del servicio no debe envenenar la caché durante un día.

**Cuando falla.** El registro del libro **no se rompe**: se guarda con lo que la persona
escribió. Lo que se escribe a mano siempre gana sobre lo que devuelve el catálogo externo. Si
no hay datos manuales ni externos, se responde `400 INCOMPLETE_BOOK_DATA` pidiendo el título y
el autor. La previsualización sí informa el fallo (`503 EXTERNAL_LOOKUP_FAILED`), porque ahí
el usuario pidió explícitamente los datos externos.

> Latencia en el peor caso: 3 s por fuente, 6 s si ninguna responde.

---

## 7. API

Errores siempre con la misma forma:

```json
{
  "timestamp": "2026-08-11T20:46:18.114Z",
  "status": 409,
  "code": "DUPLICATE_ISBN",
  "message": "Ya existe un libro registrado con el ISBN 9780132350884",
  "path": "/api/books",
  "fieldErrors": { "isbn": "El ISBN no es válido" }
}
```

`code` es estable y pensado para que el cliente reaccione a una regla concreta sin leer el mensaje.

| Método | Ruta | Acceso |
|---|---|---|
| POST | `/api/auth/register` · `/api/auth/login` | público |
| GET | `/api/books` · `/api/books/{id}` · `/api/books/subjects` | autenticado |
| GET | `/api/books/lookup/{isbn}` | autenticado |
| GET | `/api/books/recommendations` | autenticado |
| POST | `/api/books` | **ADMIN** |
| DELETE | `/api/books/{id}` | **ADMIN**, solo si `DISPONIBLE` |
| POST | `/api/loans` · GET `/api/loans/mine` · PUT `/api/loans/{id}/return` | autenticado |
| POST | `/api/reservations` · GET `/api/reservations/mine` · DELETE `/api/reservations/{id}` | autenticado |
| GET | `/api/admin/stats` | **ADMIN** |
| GET | `/api/admin/users` · PUT `/api/admin/users/{id}/unblock` | **ADMIN** |
| POST | `/api/admin/notifications/{due-soon-reminders,overdue-notices}` | **ADMIN** |

**Colección Postman:** [`docs/postman/`](docs/postman) — 30 peticiones con tests, incluida una
carpeta *Reglas de negocio* con los casos que **deben fallar**. Importa la colección y el
entorno, ejecuta *Login (ADMIN)* y el token se guarda solo.

**Seguridad.** JWT HS256 sobre Spring Security, sin estado, contraseñas con BCrypt.
`JWT_SECRET` viene por variable de entorno; **si no se define, la aplicación genera una clave
efímera y lo avisa con un WARN**. Es lo que permite que `docker compose up` sea un solo comando
sin subir secretos al repositorio, a costa de que los tokens dejen de valer al reiniciar.

---

## 8. Frontend

Angular 21 *standalone*, **zoneless**, con signals, `@if/@for` y rutas cargadas bajo demanda.
Sin librerías de UI ni de gráficos: el donut y la línea de tendencia son **SVG escritos a mano**.
Bundle inicial: **~95 kB comprimidos**.

| Pantalla | Qué resuelve |
|---|---|
| Login / Registro | validación espejo del backend, «Recordarme» |
| Inicio | métricas propias, próximos a vencer, recomendaciones |
| Catálogo | búsqueda, filtros por estado y tema, orden, paginación |
| Agregar libro | asistente de 3 pasos con autocompletado por ISBN |
| Mis préstamos | pestañas, cuenta regresiva, devolución |
| Mis reservas | posición en la fila, cancelar, pedir prestado |
| Administración | resumen con gráficos, cuentas y bloqueos, catálogo |

Un interceptor añade el token a cada llamada a `/api` (y **solo** ahí: las portadas van a
Open Library y enviarles el token sería filtrarlo). Otro traduce cualquier fallo a un aviso
visible: no hay errores silenciosos. Cada pantalla tiene sus estados de **carga** (esqueletos
con la forma del contenido), **error** (con botón de reintento) y **vacío**.

Los filtros del catálogo viven en la URL (`/catalogo?q=clean&estado=DISPONIBLE`), así que una
búsqueda se puede compartir y el botón «atrás» hace lo esperable.

### Recomendaciones

Filtrado basado en contenido, el clásico de los sistemas de recomendación: se construye el
perfil de temas del lector desde su historial (ponderado por antigüedad, con vida media de 180
días), se pesan los temas por **TF-IDF** —«Software engineering», que está en casi todos los
libros, vale mucho menos que «Distributed systems»— y se ordena por **similitud coseno**.

**Sin ningún servicio externo, sin credenciales y sin costo.** La interfaz explica cada
sugerencia mostrando los temas en común, y una cuenta sin historial no recibe recomendaciones
inventadas: simplemente no se muestra el panel.

---

## 9. Pruebas

```bash
cd backend && ./mvnw verify
```

**126 pruebas** (101 unitarias + 25 de integración).

| Qué se cubre | Con qué |
|---|---|
| Reglas de préstamo, atrasos y bloqueo | JUnit + Mockito con `Clock.fixed` |
| ISBN duplicado, borrado, enriquecimiento | `BookServiceTest` |
| Open Library: éxito, 404, 500, 429, JSON roto y **timeout** | **MockWebServer** |
| Endpoints de catálogo y préstamos, roles 401/403 | **MockMvc + Testcontainers** (PostgreSQL real) |
| Que el correo **sale de verdad**, con asunto y destinatario | **GreenMail** |

Se prueba contra PostgreSQL real y no H2 porque el esquema usa índices únicos parciales y
`to_char`: probar sobre otro motor no diría nada de las migraciones que se despliegan.

> **Nota sobre Windows:** las pruebas de MockWebServer necesitan sockets de *loopback*. En
> algunos equipos con antivirus estricto fallan con `Unable to establish loopback connection`.
> En Linux, en Docker y en CI pasan sin problema; si te ocurre, ejecuta la suite dentro de un
> contenedor.

---

## 10. Decisiones tomadas

El enunciado pide documentar las decisiones en vez de dejar huecos. Estas son.

### Sobre el modelo

**El préstamo se vincula a la cuenta por correo.** El enunciado define `Loan` con
`borrowerName`/`borrowerEmail` (sin relación a `AppUser`) pero también pide `/api/loans/mine`
y bloquear cuentas. Se conservan ambos campos como dice el enunciado y se resuelven contra
`AppUser` por correo: el bloqueo aplica a esa cuenta y `/mine` filtra por el correo autenticado.
Un ADMIN puede registrar un préstamo a nombre de otra cuenta; un BIBLIOTECARIO solo para sí.

**`RESERVADO` no significa «libre».** Un ejemplar retenido solo puede llevárselo el titular de
la reserva; la reserva pasa a `CUMPLIDO` automáticamente al registrarse ese préstamo.

### Añadidas al enunciado

**Máximo 3 préstamos activos.** No está en el enunciado: viene de la interfaz, que promete
«puedes tener hasta 3 préstamos activos». Se implementó como regla configurable
(`LIBRIS_MAX_ACTIVE_LOANS`) en lugar de borrar el texto.

**Aviso de préstamo vencido.** El enunciado pide cuatro correos, pero `Loan` incluye
`overdueNoticeSentAt`: ese campo solo tiene sentido si algo envía ese aviso y lo registra.

**Tres endpoints fuera de la lista**, exigidos por las propias reglas y por las pantallas:
`GET /api/admin/users` y `PUT /api/admin/users/{id}/unblock` (el enunciado pide que un ADMIN
pueda levantar el bloqueo), `GET /api/reservations/mine` (sin él no existe «Mis reservas») y
`POST /api/admin/notifications/*` (para poder demostrar las tareas programadas sin esperar a
las 08:00).

### Correcciones al diseño recibido

El diseño de referencia tenía elementos que contradicen las reglas o que no pide el enunciado.
Lo que se hizo:

| En el diseño | Qué se hizo | Por qué |
|---|---|---|
| Sección ADMINISTRACIÓN visible para un bibliotecario | Se oculta salvo para ADMIN | Solo ADMIN registra libros y ve estadísticas |
| KPIs globales en «Inicio» | «Inicio» muestra métricas **propias** | `/api/admin/stats` es ADMIN-only: un bibliotecario vería solo 403 |
| «Marcar como recibido» en una reserva | Se reemplaza por **«Pedir prestado»** | No existe tal endpoint; la reserva se cierra sola al prestar |
| Donut con «Por vencer» dentro de «Activos» sumando 100 % | Cuatro estados **mutuamente excluyentes** | Si no, los porcentajes cuentan dos veces el mismo préstamo |
| Total 2.148 ≠ Disponibles 1.258 + Prestados 138 | Total = disponibles + prestados + reservados | Coherencia aritmética |
| Login con Google, recuperar contraseña | Eliminados | No los pide el enunciado y exigirían credenciales OAuth en el repositorio |
| Campana de notificaciones, «Actividad reciente», «Configuración» | Eliminados | Requerirían entidades que nadie pide |
| Faltaban registro, «Reservar» y «Eliminar libro» | Añadidos | El enunciado sí exige esos endpoints |

### Técnicas

- **Spring Boot 3.5**, no 4.x: la línea 3.x es la madura para springdoc y Testcontainers, y el
  enunciado pide «3.3+».
- **Sin Lombok**: `record` para los DTOs y accesores explícitos en las entidades. Menos magia
  en un proyecto que se lee para evaluarlo.
- **Nulos explícitos en el JSON**: `returnDate` viaja como `null` en vez de desaparecer, para
  que el cliente distinga «sin devolver» de «campo ausente».
- **El health check no depende del SMTP**: la API es perfectamente usable con el correo caído,
  porque los envíos son asíncronos.

---

## 11. Variables de entorno

Todas tienen valor por defecto: `docker compose up` funciona sin crear ningún archivo. Para
personalizar, copia [`.env.example`](.env.example) a `.env`.

| Variable | Por defecto | Para qué |
|---|---|---|
| `FRONTEND_PORT` / `BACKEND_PORT` | `4200` / `8080` | puertos publicados |
| `POSTGRES_DB` / `_USER` / `_PASSWORD` | `libris` | base de datos |
| `JWT_SECRET` | *(vacío)* | clave HS256; vacío ⇒ efímera + WARN |
| `JWT_EXPIRATION_MINUTES` | `480` | vigencia del token |
| `SPRING_PROFILES_ACTIVE` | `demo` | `default` para arrancar sin datos de ejemplo |
| `MAIL_HOST` / `MAIL_PORT` | `mailhog` / `1025` | servidor SMTP |
| `LIBRIS_LOAN_DAYS` | `14` | duración del préstamo |
| `LIBRIS_MAX_ACTIVE_LOANS` | `3` | préstamos simultáneos |
| `LIBRIS_DUE_SOON_DAYS` | `3` | ventana de «por vencer» |
| `LIBRIS_REMINDER_DAYS_BEFORE` | `2` | antelación del recordatorio |
| `LIBRIS_OVERDUE_LIMIT` | `3` | atrasos que bloquean |
| `LIBRIS_OVERDUE_WINDOW_DAYS` | `90` | ventana móvil de atrasos |
| `LIBRIS_BLOCK_DAYS` | `7` | duración del bloqueo |
| `TZ` | `America/Santiago` | las fechas límite son fechas de negocio |

---

## 12. Desarrollo sin Docker

```bash
# Base de datos y buzón
docker compose up -d postgres mailhog

# Backend (requiere JDK 21)
cd backend && ./mvnw spring-boot:run

# Frontend (requiere Node 20.19+/22.12+/24+)
cd frontend && npm install && npm start
```

`npm start` levanta Angular en el 4200 y redirige `/api` al 8080 mediante
[`proxy.conf.json`](frontend/proxy.conf.json).

---

## 13. Git

Se trabajó con **GitFlow**: `main` ← `release/*` ← `develop` ← `feature/*` y `fix/*`, con
merges `--no-ff` para conservar la historia de cada rama. Los commits son cortos y en inglés,
con estilo *conventional commits*.

```bash
git log --graph --oneline --all
```
