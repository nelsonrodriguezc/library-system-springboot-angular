# Libris — Sistema de préstamos para una biblioteca

Aplicación fullstack que reemplaza la planilla Excel de préstamos de una biblioteca: catálogo de libros,
préstamos con reglas de negocio reales (atrasos, bloqueos, lista de espera), notificaciones por correo
y autocompletado de fichas desde Open Library a partir del ISBN.

## Stack

| Capa | Tecnología |
|---|---|
| Backend | Java 21 · Spring Boot 3.5 (Web, Data JPA, Validation, Security, Mail, Cache) |
| Base de datos | PostgreSQL 16 con migraciones versionadas en Flyway |
| Correo | Spring Mail + plantillas Thymeleaf, contra MailHog en local |
| API externa | Open Library vía `RestClient`, con caché Caffeine |
| Frontend | Angular 21 (standalone components + signals) con TypeScript |
| Infra local | Docker Compose (aplicación, PostgreSQL y MailHog) |

## Estructura del repositorio

```
backend/    API REST en Spring Boot
frontend/   Aplicación Angular
docs/       Colección Postman y material de apoyo
```

## Cómo ejecutarlo

> La documentación completa de arranque, variables de entorno, credenciales de prueba y decisiones de
> diseño se incorpora al cerrar el proyecto. Por ahora, para trabajar sobre el backend:

```bash
cd backend && ./mvnw spring-boot:run
```

Requisitos: JDK 21 y una instancia de PostgreSQL accesible en `jdbc:postgresql://localhost:5432/libris`.
