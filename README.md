# Depot — Warehouse Inventory Management Backend

A REST backend for managing a warehouse ("depot"): suppliers stock products into
inventory, users place and track orders, and administrators manage product schemas,
categories, and stock — all behind token-based authentication and role-based access.

Built as my **BSc thesis project** at the Budapest University of Technology and Economics
(BME), Department of Automation and Applied Informatics (AUT), 2024.

**Tech stack:** Java · Spring Boot · Spring Security (JWT) · Maven · PostgreSQL · Docker /
Docker Compose · GitHub Actions CI · SonarCloud

## Features

- **Authentication & authorization** — JWT-based login with role-based access control.
- **Inventory** — track stock per supplier, enforce depot/inventory capacity limits,
  reservations, and out-of-stock handling.
- **Orders** — create, validate, cancel, and query orders with ownership checks.
- **Catalog** — manage categories, products, and product schemas (storage-space rules,
  expiry validation).
- **Suppliers & users** — full lifecycle management with duplicate/validation guards.
- **Robust error handling** — a dedicated exception hierarchy surfaced through a global
  exception handler.

## Project structure

```
src/main/java/com/bme/vik/aut/thesis/depot/
├── DepotApplication.java        # Spring Boot entry point
├── exception/                   # domain exception hierarchy + GlobalExceptionHandler
└── general/                     # admin, inventory, order, product, supplier, user modules
```

Supporting files: `Dockerfile`, `docker-compose.yaml`, GitHub Actions (`.github/workflows/build.yml`),
SonarCloud config (`sonar-project.properties`), and the thesis writeup
(`Raktarkeszlet-kezelo-alkalmazas-backend.pdf`).

## Build & run

```bash
# Run the app together with its database
docker compose up --build

# …or run locally against your own database
./mvnw spring-boot:run

# Tests
./mvnw test
```

See `docker-compose.yaml` for the database service and the environment variables the app
expects.

---

*BME thesis project, 2024. Java / Spring Boot backend with CI and static analysis wired up.*
