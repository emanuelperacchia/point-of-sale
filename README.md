# Point of Sale — POS System

Sistema de punto de venta completo con backend Spring Boot + frontend React (TypeScript).
Incluye facturación electrónica, turnos de caja, devoluciones, motor de promociones y programa de fidelización.

## Stack

| Capa | Tecnología |
|------|-----------|
| **Backend** | Java 17, Spring Boot 3, Spring Security, Spring Data JPA, Flyway, PostgreSQL |
| **Frontend** | React 19, TypeScript, Vite, Axios, Tailwind CSS |
| **Build** | Maven (backend), pnpm (frontend) |
| **Testing** | JUnit 5, Mockito, Spring MockMvc |

## Estructura

```
point-of-sale/
├── backend/          → API REST (Spring Boot)
│   ├── src/
│   ├── pom.xml
│   └── ...
├── frontend/         → SPA (React + Vite)
│   ├── src/
│   ├── package.json
│   └── ...
├── .gitignore
└── README.md
```

## Requisitos

- **Java 17+**
- **Node.js 18+** y **pnpm** (`npm install -g pnpm`)
- **PostgreSQL 15+**
- **Maven** (o usar `./mvnw` incluido)

## Configuración rápida

### 1. Base de datos

```bash
createdb pos_db
```

### 2. Backend

```bash
cd backend

# Configurar variables de entorno
$env:JWT_SECRET="tu-secreto-jwt"
$env:DB_PASSWORD="tu-contraseña"

# Iniciar
./mvnw spring-boot:run
```

La API corre en `http://localhost:8080`. Swagger UI en `http://localhost:8080/swagger-ui.html`.

### 3. Frontend

```bash
cd frontend
pnpm install
pnpm dev
```

El frontend corre en `http://localhost:5173` con proxy automático al backend.

## Variables de Entorno

| Variable | Descripción |
|----------|-------------|
| `JWT_SECRET` | Clave secreta para firmar tokens JWT |
| `DB_PASSWORD` | Contraseña de PostgreSQL |
| `CORS_ORIGINS` | Orígenes permitidos (default: `http://localhost:5173`) |

## Roles del Sistema

| Rol | Acceso |
|-----|--------|
| `ADMIN` | Administración completa |
| `GERENTE` | CRUD promociones, reportes, aprobar devoluciones |
| `CAJERO` | POS, cobros, devoluciones |
| `VENDEDOR` | Consultas, clientes |

## Sprints implementados

| Sprint | Feature |
|--------|---------|
| 1–4 | Fundación, compras, proveedores, gestión de stock |
| 5 | POS core (carrito, cobro, cliente, turno) |
| 6 | Facturación electrónica (AFIP-style, PDF) |
| 7 | Turnos de caja, devoluciones |
| 8 | **Promociones automáticas, cupones, fidelización (puntos + tiers)** |

## Licencia

Proyecto privado — uso interno.
