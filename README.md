# Order Management API

![CI](https://github.com/caioamorimr/order-management-api/actions/workflows/ci.yml/badge.svg)
![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-brightgreen?logo=springboot&logoColor=white)
![Maven](https://img.shields.io/badge/Build-Maven-C71A36?logo=apachemaven&logoColor=white)

A REST API for managing an e-commerce-style order flow — users, products, categories, and orders — built with Spring
Boot, secured with JWT, backed by PostgreSQL in production, and with its schema fully versioned through Flyway
migrations.

This project was built as a portfolio piece to practice production-oriented backend engineering: not just CRUD
endpoints, but authentication, layered architecture, automated testing, database migrations, and CI.

## Table of Contents

- [Features](#features)
- [Tech Stack](#tech-stack)
- [Architecture](#architecture)
- [Domain Model](#domain-model)
- [API Endpoints](#api-endpoints)
- [Getting Started](#getting-started)
- [Environment Variables](#environment-variables)
- [Running with Docker](#running-with-docker)
- [Database Migrations](#database-migrations)
- [Running Tests](#running-tests)
- [Continuous Integration](#continuous-integration)
- [Project Structure](#project-structure)
- [About](#about)

## Features

- **JWT-based stateless authentication** — logging in returns a Bearer token; every endpoint except `/auth/login`
  requires it.
- **Full CRUD** for Users, Products, Categories, and Orders, with pagination on all collection endpoints via Spring
  Data's `Pageable`.
- **Many-to-many Product ↔ Category** relationship, with dedicated endpoints to attach/detach categories from a product.
- **Order lifecycle** modeled as a status enum (`WAITING_PAYMENT`, `PAID`, `SHIPPED`, `DELIVERED`, `CANCELED`), with
  order items and an associated payment record.
- **Centralized exception handling** — validation errors, not-found, and database-integrity conflicts all return a
  consistent JSON error shape instead of raw stack traces.
- **Schema managed by Flyway** — every environment (test, dev, prod) builds its schema from the same versioned SQL
  migrations, so there's no drift between what's in code and what's in the database.
- **Interactive API documentation** via Swagger / OpenAPI, automatically disabled in production.
- **Environment-specific configuration** (dev, test, prod), with production configured to fail fast at startup if a
  required secret is missing, rather than silently falling back to an insecure default.
- **70+ automated tests**: service-layer unit tests, controller slice tests, a dedicated security test suite, and a full
  end-to-end integration test that exercises the real login → create → fetch flow.
- **Continuous Integration** via GitHub Actions, running the full test suite on every push and pull request.

## Tech Stack

| Layer              | Technology                                                          |
|--------------------|---------------------------------------------------------------------|
| Language / Runtime | Java 21                                                             |
| Framework          | Spring Boot 3.5                                                     |
| Web                | Spring Web (MVC)                                                    |
| Persistence        | Spring Data JPA / Hibernate                                         |
| Database           | PostgreSQL (production) · H2 (tests, PostgreSQL-compatibility mode) |
| Migrations         | Flyway                                                              |
| Security           | Spring Security · JWT ([jjwt](https://github.com/jwtk/jjwt))        |
| Validation         | Jakarta Bean Validation                                             |
| API Docs           | springdoc-openapi (Swagger UI)                                      |
| Build Tool         | Maven                                                               |
| Testing            | JUnit 5 · Mockito · MockMvc · AssertJ                               |
| CI                 | GitHub Actions                                                      |
| Containerization   | Docker (multi-stage build)                                          |

## Architecture

The project follows a standard layered architecture, keeping HTTP, business logic, and persistence concerns separate:

```
Client
  │
  ▼
Resource (Controller)   ──▶  validates request DTOs, maps HTTP ↔ domain
  │
  ▼
Service                 ──▶  business rules, transactions
  │
  ▼
Repository (Spring Data JPA)
  │
  ▼
Database (PostgreSQL / H2)
```

Cross-cutting concerns are isolated into their own packages: `security/` for JWT authentication, `resources/exceptions/`
for a centralized error-handling layer shared by every controller, and `dto/` so entities are never exposed directly
over the wire.

## Domain Model

| Entity      | Description                                                              |
|-------------|--------------------------------------------------------------------------|
| `User`      | A client who authenticates and places orders.                            |
| `Category`  | Product classification; many-to-many with `Product`.                     |
| `Product`   | A sellable item; can belong to multiple categories.                      |
| `Order`     | Placed by a `User`; has a status and one or more items.                  |
| `OrderItem` | A product, quantity, and price snapshot within an order (composite key). |
| `Payment`   | One-to-one with `Order`; recorded once an order is paid.                 |

## API Endpoints

All business endpoints are versioned under `/api/v1`. They require an `Authorization: Bearer <token>` header, except the
Swagger routes, the H2 console (test profile only), and `/actuator/health`. `/auth/**` is unversioned infrastructure and
is always public - no token is exchanged yet at that point.

Role column values: **any** = any authenticated user, **self** = any authenticated user acting on their own resource,
**ADMIN** = requires the `ADMIN` role.

### Auth (`/auth`, unversioned, always public)

| Method | Endpoint         | Description                                                                     |
|--------|------------------|---------------------------------------------------------------------------------|
| POST   | `/auth/login`    | Authenticate with email + password, returns a JWT + refresh token               |
| POST   | `/auth/register` | Public self-registration; always creates an account with the `USER` role        |
| POST   | `/auth/refresh`  | Exchange a valid refresh token for a new token pair (rotates the refresh token) |
| POST   | `/auth/logout`   | Revoke a refresh token                                                          |

### Categories (`/api/v1/categories`)

| Method | Endpoint                  | Role  | Description                 |
|--------|---------------------------|-------|-----------------------------|
| GET    | `/api/v1/categories`      | any   | List categories (paginated) |
| GET    | `/api/v1/categories/{id}` | any   | Get a category by id        |
| POST   | `/api/v1/categories`      | ADMIN | Create a category           |
| PUT    | `/api/v1/categories/{id}` | ADMIN | Update a category           |
| DELETE | `/api/v1/categories/{id}` | ADMIN | Delete a category           |

### Products (`/api/v1/products`)

| Method | Endpoint                                               | Role  | Description                      |
|--------|--------------------------------------------------------|-------|----------------------------------|
| GET    | `/api/v1/products`                                     | any   | List products (paginated)        |
| GET    | `/api/v1/products/{id}`                                | any   | Get a product by id              |
| POST   | `/api/v1/products`                                     | ADMIN | Create a product                 |
| PUT    | `/api/v1/products/{id}`                                | ADMIN | Update a product                 |
| DELETE | `/api/v1/products/{id}`                                | ADMIN | Delete a product                 |
| PUT    | `/api/v1/products/{productId}/categories/{categoryId}` | ADMIN | Attach a category to a product   |
| DELETE | `/api/v1/products/{productId}/categories/{categoryId}` | ADMIN | Detach a category from a product |

### Users (`/api/v1/users`)

To register your own account, use `POST /auth/register` above - `POST /api/v1/users` is the administrative creation
endpoint and requires `ADMIN`.

| Method | Endpoint             | Role         | Description                    |
|--------|----------------------|--------------|--------------------------------|
| GET    | `/api/v1/users`      | ADMIN        | List users (paginated)         |
| GET    | `/api/v1/users/{id}` | self / ADMIN | Get a user by id               |
| POST   | `/api/v1/users`      | ADMIN        | Administratively create a user |
| PUT    | `/api/v1/users/{id}` | self / ADMIN | Update a user                  |
| DELETE | `/api/v1/users/{id}` | ADMIN        | Delete a user                  |

### Orders (`/api/v1/orders`)

| Method | Endpoint              | Role         | Description                                                                 |
|--------|-----------------------|--------------|-----------------------------------------------------------------------------|
| GET    | `/api/v1/orders`      | ADMIN        | List orders (paginated)                                                     |
| GET    | `/api/v1/orders/{id}` | self / ADMIN | Get an order by id, with its items and payment                              |
| POST   | `/api/v1/orders`      | self / ADMIN | Create an order with one or more items (always starts as `WAITING_PAYMENT`) |
| PUT    | `/api/v1/orders/{id}` | ADMIN        | Update an order (rejects illegal status transitions with a 409)             |
| DELETE | `/api/v1/orders/{id}` | ADMIN        | Delete an order                                                             |

### Health check (unversioned)

| Method | Endpoint           | Role   | Description                                |
|--------|--------------------|--------|--------------------------------------------|
| GET    | `/actuator/health` | public | Basic UP/DOWN status, no component details |

The full interactive documentation (with request/response schemas) is available at `/swagger-ui.html` while the
application is running with any profile other than `prod`.

## Getting Started

### Prerequisites

- Java 21
- Docker (optional — only needed if you want to run against a real PostgreSQL instance)

The Maven wrapper (`./mvnw`) is included, so a local Maven install isn't required.

### Clone

```bash
git clone https://github.com/caioamorimr/order-management-api.git
cd order-management-api
```

### Fastest way to try it — in-memory H2, pre-seeded data

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=test
```

This runs the API on `http://localhost:8080` against an in-memory H2 database, automatically migrated by Flyway and
pre-loaded with sample categories, products, users, and orders. Log in with the seeded user:

```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"caio@email.com","password":"123456"}'
```

Use the returned token as a Bearer token on any other endpoint.

### Running against PostgreSQL (production-like)

```bash
export DATABASE_URL=jdbc:postgresql://localhost:5432/order_management
export DATABASE_USERNAME=postgres
export DATABASE_PASSWORD=postgres
export JWT_SECRET=$(openssl rand -base64 32)

./mvnw spring-boot:run -Dspring-boot.run.profiles=prod
```

Flyway creates the schema automatically on first startup. Note that the `prod` profile has **no default value** for
`JWT_SECRET` — the application deliberately fails to start if it isn't set, instead of silently running with an insecure
key.

## Environment Variables

| Variable            | Used in | Default                                                                            | Description                  |
|---------------------|---------|------------------------------------------------------------------------------------|------------------------------|
| `PORT`              | prod    | `8080`                                                                             | HTTP port                    |
| `DATABASE_URL`      | prod    | — (required)                                                                       | JDBC URL for PostgreSQL      |
| `DATABASE_USERNAME` | prod    | — (required)                                                                       | Database username            |
| `DATABASE_PASSWORD` | prod    | — (required)                                                                       | Database password            |
| `JWT_SECRET`        | all     | insecure dev placeholder (default profile only) — **required, no default in prod** | Secret used to sign JWTs     |
| `JWT_EXPIRATION`    | all     | `30`                                                                               | Token expiration, in minutes |

## Running with Docker

The `Dockerfile` uses a multi-stage build: it compiles the project with Maven on a Temurin 21 image, then ships only the
resulting JAR on a slim Temurin 21 JRE Alpine image.

### Option A — Docker Compose (recommended)

`docker-compose.yml` runs the API alongside a PostgreSQL container on the same network, so there's no host-networking
configuration to worry about:

```bash
export JWT_SECRET=$(openssl rand -base64 32)
docker compose up --build
```

The API will be available at `http://localhost:8080`, backed by a Postgres instance managed entirely by Compose (data
persisted in a named volume). The `api` container talks to `db` over the internal Compose network on its default port
5432 — the container's port is only published to the host as `5433` (e.g. `psql -h localhost -p 5433 -U postgres`) in
case you want to inspect the database directly; if `5433` is also taken on your machine, change the host-side port in
`docker-compose.yml`.

### Option B — connecting to a PostgreSQL already running on your machine

```bash
docker build -t order-management-api .

docker run -p 8080:8080 \
  --add-host=host.docker.internal:host-gateway \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e DATABASE_URL=jdbc:postgresql://host.docker.internal:5432/order_management \
  -e DATABASE_USERNAME=postgres \
  -e DATABASE_PASSWORD=postgres \
  -e JWT_SECRET=$(openssl rand -base64 32) \
  order-management-api
```

> `host.docker.internal` resolves automatically on Docker Desktop (macOS/Windows). On native Docker Engine (Linux), the
> `--add-host=host.docker.internal:host-gateway` flag above is required (Docker 20.10+). Even with the hostname
> resolving,
> the connection will still be refused unless PostgreSQL is configured to accept it: by default `postgresql.conf` sets
> `listen_addresses = 'localhost'`, which rejects connections arriving from the Docker bridge network. You'll need to
> set
> `listen_addresses = '*'` (or the bridge subnet specifically) and add a matching entry to `pg_hba.conf`. If that sounds
> like more trouble than it's worth, use Option A instead.

## Database Migrations

Schema changes are managed by [Flyway](https://flywaydb.org/), with versioned SQL files under
`src/main/resources/db/migration/`, following the `V{version}__{description}.sql` naming convention. Every environment —
including the test suite — builds its schema from these same files, so there's a single source of truth for the database
structure instead of relying on Hibernate to infer it from the entities.

## Running Tests

```bash
./mvnw test
```

The suite covers:

- **Service layer** — unit tests with Mockito, isolating business logic from persistence.
- **Controller layer** — `@WebMvcTest` slice tests with mocked services.
- **Security** — verifies protected endpoints return `401` without a valid token.
- **Integration** — a full end-to-end test that authenticates via `/auth/login`, then creates a category, a product, a
  user, and an order through the real HTTP layer, backed by a real (in-memory) database.

## Continuous Integration

Every push and pull request against `main` triggers a GitHub Actions workflow that runs the full test suite on Java 21.
See [`.github/workflows/ci.yml`](.github/workflows/ci.yml).

## Project Structure

```
src/main/java/com/caioamorimr/ordermanagement/
├── config/         # Spring configuration (OpenAPI docs, test data seeding)
├── dto/            # Request/response contracts, decoupled from entities
├── entities/        # JPA entities and the OrderStatus enum
├── repositories/     # Spring Data JPA repositories
├── resources/        # REST controllers + centralized exception handling
├── security/         # JWT filter, JwtUtil, SecurityConfig, UserDetailsService
└── services/         # Business logic
```

## About

Built by **Caio Amorim**, an Information Systems student, as a hands-on project to practice REST API design, Spring
Security, and production-oriented backend practices.

- GitHub: [@caioamorimr](https://github.com/caioamorimr)
- Email: caioamorimribeiro@gmail.com