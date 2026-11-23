# 🏪 Nivex POS - Sistema de Punto de Venta

Sistema completo de Punto de Venta (POS) para comercios, con backend en Spring Boot, frontend en React/Vite, y aplicación Android nativa.

---

## 📋 Tabla de Contenidos

1. [Resumen](#resumen)
2. [Arquitectura del Sistema](#arquitectura-del-sistema)
3. [Requisitos Previos](#requisitos-previos)
4. [Instalación y Configuración](#instalación-y-configuración)
   - [Backend](#backend)
   - [Frontend](#frontend)
   - [Android](#android)
5. 🎯 **Guía de Uso**
   - [Acceder al Sistema](#acceder-al-sistema)
   - [Punto de Venta (POS)](#punto-de-venta-pos)
   - [Gestionar Sucursales](#gestionar-sucursales)
   - **Gestionar Proveedores** (`/proveedores`)
   - **Gestionar Categorías** (`/categorías`)
   - **Gestionar Precios** (`/precios`)
   - **Gestionar Usuarios** (`/usuarios`)
   - **Empleados** (`/empleados`)
   - **Historial de Ventas** (`/ventas`)
   - **Cierre de Caja** (`/cierre-caja`)
   - **Registro de Usuario** (`/registro`)
   - **Privacidad y Datos** (`/mi-perfil`)
6. 🔌 **Endpoints de la API** (por módulo)
7. 🧪 **Pruebas (Testing)**
   - Backend
   - Frontend
   - Android
8. 📱 **Despliegue en Producción**
9. 🤝 **Contribución**
10. 📄 **Licencia**

---

## 📋 Resumen

Nivex POS es un sistema completo de punto de venta diseñado para comercios pequeños y medianos. Permite gestionar:

- ✅ **Ventas** - Proceso completo de venta con carrito, pagos y facturación
- ✅ **Sucursales** - Gestión multi-tienda
- ✅ **Proveedores** - Gestión de proveedores con datos fiscales
- ✅ **Categorías** - Organización de productos por categorías
- ✅ **Productos** - Catálogo completo con stock y precios
- ✅ **Precios por Sucursal** - Precios diferenciados por ubicación
- ✅ **Usuarios y Empleados** - Gestión de personal con roles y permisos
- ✅ **Historial de Ventas** - Consulta y reportes de ventas realizadas
- ✅ **Cierre de Caja** - Turnos de caja con apertura y cierre
- ✅ **Registro de Usuario** - Registro con aceptación de términos (Ley 25.326)
- ✅ **Privacidad** - Exportación de datos y solicitud de baja (Ley 25.326)

---

## 🏗️ Arquitectura del Sistema

```
┌─────────────┐      HTTP/REST      ┌──────────────────┐
│   Android   │ ◄──────────────────► │   Backend Spring │
│   (App)     │      (JSON/JSON)     │     Boot 3.3.7   │
└─────────────┘                      └──────────────────┘
                                           │
                                           │ JDBC
                                           ▼
                                    ┌───────────────┐
                                    │  PostgreSQL   │
                                    │   15.x        │
                                    └───────────────┘

┌─────────────┐      HTTP/REST      ┌──────────────────┐
│  React/Vite │ ◄──────────────────► │   Backend Spring │
│  (Frontend) │      (JSON/JSON)     │     Boot 3.3.7   │
└─────────────┘                      └──────────────────┘
```

### Stack Tecnológico

| Componente | Tecnología | Versión |
|------------|------------|---------|
| Backend | Spring Boot | 3.3.7 |
| Base de Datos | PostgreSQL | 15 |
| Frontend | React + Vite | 19 + 8 |
| Android | Kotlin + Compose | SDK 35 |
| Seguridad | JWT + Spring Security | - |
| Tests Backend | JUnit 5 + Mockito | - |
| Tests Frontend | Vitest + Testing Library | - |
| Tests Android | JUnit 5 + MockK + Bubbletea | - |

---

## 📦 Requisitos Previos

### Software Necesario

| Software | Versión Mínima | Propósito |
|----------|---------------|-----------|
| Java | JDK 17 | Backend Spring Boot |
| Node.js | 18+ | Frontend React/Vite |
| Android Studio | Latest | Desarrollo Android |
| PostgreSQL | 15+ | Base de datos |
| Docker (opcional) | 20+ | Despliegue con contenedores |
| Git | 2.0+ | Control de versiones |

---

## 🚀 Instalación y Configuración

### 1. Clonar el Repositorio

```bash
git clone https://github.com/emanuelperacchia/point-of-sale.git
cd point-of-sale
```

---

### 2. Backend (Spring Boot)

#### Opción A: Ejecutar localmente

```powershell
# Entrar al directorio del backend
cd backend

# Ejecutar con Maven
./mvnw spring-boot:run
```

El backend se ejecutará en `http://localhost:8080`

#### Opción B: Con Docker

```powershell
# Desde la raíz del proyecto
docker-compose up -d backend
```

**Variables de entorno necesarias** (en `backend/.env`):
```env
POSTGRES_PASSWORD=tu_password
POS_JWT_SECRET=tu_clave_secreta_muy_larga
POS_CRYPTO_KEY=tu_clave_de_cifrado_muy_larga
CORS_ORIGINS=http://localhost:5173
DB_POOL_MAX_SIZE=12
DB_POOL_MIN_IDLE=12
```

---

### 3. Frontend (React/Vite)

```powershell
# Entrar al directorio del frontend
cd frontend

# Instalar dependencias
npm install

# Ejecutar en modo desarrollo
npm run dev

# El frontend se ejecutará en http://localhost:5173
```

**Variables de entorno** (en `frontend/.env`):
```env
VITE_API_URL=http://localhost:8080/api
```

---

### 4. Android (Kotlin + Compose)

```powershell
# Entrar al directorio de Android
cd android

# Abrir en Android Studio
# O compilar desde línea de comandos
./gradlew assembleDebug
```

#### Requisitos para Android:
- Android Studio instalado
- SDK 35 instalado
- Emulador configurado o dispositivo físico
- `signing.properties` en `android/` para builds de release

**Archivo `android/signing.properties`:**
```properties
nivex.release.storeFile=nivex-release.jks
nivex.release.storePassword=tu_contraseña
nivex.release.keyAlias=nivex-release
nivex.release.keyPassword=tu_contraseña
```

---

## 🎯 Guía de Uso

### Acceder al Sistema

1. Abre el navegador y ve a: `http://localhost:5173`
2. Inicia sesión con:
   - **Email:** `admin@pos.com`
   - **Contraseña:** `admin123`

O usa la aplicación Android en tu emulador o dispositivo.

---

### 🏪 Punto de Venta (POS)

El POS es el módulo principal para realizar ventas:

1. **Ingresar al POS:** Desde el menú lateral, selecciona "POS"
2. **Buscar productos:** Escribe el nombre o SKU en el campo de búsqueda
3. **Agregar al carrito:** Haz clic en un producto para agregarlo
4. **Ver carrito:** Los productos se agregan automáticamente al panel derecho
5. **Ajustar cantidades:** Usa los botones + y - para modificar cantidades
6. **Eliminar producto:** Haz clic en el botón de eliminar (X)
7. **Procesar pago:** Haz clic en "Cobrar $XX.XX"
8. **Seleccionar método de pago:** Efectivo, Tarjeta, Transferencia, etc.
9. **Completar venta:** Confirma el pago y la venta se registrará

### 🏢 Gestionar Sucursales

Desde `/sucursales`:

- **Ver lista:** Todas las sucursales registradas
- **Crear nueva:** Botón "Nueva sucursal" → llenar formulario
- **Editar:** Botón "Editar" en cada fila
- **Activar/Desactivar:** Botón de estado en cada fila

### 🏭 Gestionar Proveedores

Desde `/proveedores`:

- **Ver lista:** Todos los proveedores registrados
- **Crear nuevo:** Botón "Nuevo proveedor"
- **Editar:** Botón "Editar" en cada fila
- **Activar/Desactivar:** Cambiar estado del proveedor

### 📦 Gestionar Categorías

Desde `/categorías`:

- **Ver lista:** Todas las categorías
- **Crear nueva:** Botón "Nueva categoría"
- **Editar:** Botón "Editar" en cada fila
- **Activar/Desactivar:** Cambiar estado

### 💰 Gestionar Precios

Desde `/precios`:

- **Seleccionar sucursal:** Elige la sucursal para gestionar precios
- **Ver precios locales:** Tabla con precios actuales
- **Crear precio local:** Botón "Nuevo Precio Local"
- **Editar precio:** Botón "Editar" en cada fila
- **Sincronizar con global:** Botón "Sincronizar con Global"

### 👥 Gestionar Usuarios

Desde `/usuarios`:

- **Ver lista:** Todos los usuarios del sistema
- **Ver detalles:** Botón "Ver" en cada fila
- **Roles:** ADMIN, GERENTE, CAJERO, VENDEDOR, INVENTARIO

### 👥 Empleados

Desde `/empleados`:

- **Ver lista:** Todos los empleados
- **Crear empleado:** Formulario completo con datos personales
- **Editar:** Modificar datos existentes
- **Activar/Desactivar:** Cambiar estado del empleado

### 📊 Historial de Ventas

Desde `/ventas`:

- **Ver historial:** Todas las ventas registradas
- **Ver detalles:** Botón "Ver" para ver items y pagos de cada venta
- **Paginación:** Navegar entre páginas de resultados

### 💰 Cierre de Caja

Desde `/cierre-caja`:

- **Ver turnos:** Lista de turnos de caja
- **Ver reporte:** Botón "Ver reporte" para ver detalle de turno
- **Ver resumen:** Ingresos, egresos, diferencia

### 📝 Registro de Usuario

Desde `/registro`:

- **Crear cuenta:** Formulario con nombre, email, contraseña, etc.
- **Aceptar términos:** Checkbox obligatorio (Ley 25.326)
- **Registro automático:** Al crear cuenta, se inicia sesión automáticamente

### 🔒 Privacidad y Datos

Desde `/mi-perfil`:

- **Ver datos personales:** Información del usuario autenticado
- **Exportar datos:** Botón "Descargar mis datos" (JSON)
- **Solicitar baja:** Botón "Solicitar baja de cuenta" (baja lógica)

---

## 🔌 Endpoints de la API

### Autenticación
| Método | Endpoint | Descripción |
|--------|----------|-------------|
| POST | `/api/auth/login` | Iniciar sesión |
| POST | `/api/auth/register` | Registrar usuario |
| POST | `/api/auth/refresh` | Renovar token |
| POST | `/api/auth/logout` | Cerrar sesión |
| GET | `/api/auth/me` | Usuario actual |
| POST | `/api/auth/switch-branch` | Cambiar sucursal |

### Sucursales
| Método | Endpoint | Descripción |
|--------|----------|-------------|
| GET | `/api/branches` | Listar activas |
| GET | `/api/branches/all` | Listar todas (ADMIN) |
| GET | `/api/branches/{id}` | Obtener por ID |
| POST | `/api/branches` | Crear (ADMIN) |
| PUT | `/api/branches/{id}` | Actualizar (ADMIN) |
| DELETE | `/api/branches/{id}` | Eliminar (ADMIN) |

### Productos
| Método | Endpoint | Descripción |
|--------|----------|-------------|
| GET | `/api/products` | Listar paginados |
| GET | `/api/products/search?q=` | Búsqueda rápida |
| GET | `/api/products/{id}` | Obtener por ID |
| POST | `/api/products` | Crear (ADMIN) |
| PUT | `/api/products/{id}` | Actualizar (ADMIN) |
| DELETE | `/api/products/{id}` | Eliminar (ADMIN) |
| PATCH | `/api/products/{id}/stock` | Actualizar stock |
| GET | `/api/products/{id}/price` | Resolver precio |

### Proveedores
| Método | Endpoint | Descripción |
|--------|----------|-------------|
| GET | `/api/suppliers` | Listar proveedores |
| GET | `/api/suppliers/{id}` | Obtener por ID |
| GET | `/api/suppliers/search?term=` | Buscar por nombre |
| POST | `/api/suppliers` | Crear proveedor |
| PUT | `/api/suppliers/{id}` | Actualizar proveedor |
| DELETE | `/api/suppliers/{id}` | Eliminar proveedor |

### Categorías
| Método | Endpoint | Descripción |
|--------|----------|-------------|
| GET | `/api/categories` | Listar categorías |
| GET | `/api/categories/active` | Categorías activas |
| GET | `/api/categories/{id}` | Obtener por ID |
| POST | `/api/categories` | Crear categoría |
| PUT | `/api/categories/{id}` | Actualizar categoría |
| DELETE | `/api/categories/{id}` | Eliminar categoría |

### Precios por Sucursal
| Método | Endpoint | Descripción |
|--------|----------|-------------|
| GET | `/api/products/{id}/price` | Resolver precio vigente |
| GET | `/api/branches/{id}/prices` | Precios locales de sucursal |
| POST | `/api/branches/{id}/prices` | Crear precio local |
| PUT | `/api/branches/{branchId}/prices/{priceId}` | Actualizar precio |
| DELETE | `/api/branches/{branchId}/prices/{priceId}` | Eliminar precio |

### Usuarios
| Método | Endpoint | Descripción |
|--------|----------|-------------|
| GET | `/api/users` | Listar usuarios (ADMIN) |
| GET | `/api/users/{id}` | Obtener usuario por ID |
| GET | `/api/users/me` | Usuario autenticado |
| GET | `/api/users/me/permissions` | Permisos del usuario |
| POST | `/api/users` | Crear usuario (ADMIN) |
| PUT | `/api/users/{id}` | Actualizar usuario (ADMIN) |
| DELETE | `/api/users/{id}` | Eliminar usuario (ADMIN) |

### Privacidad (Ley 25.326)
| Método | Endpoint | Descripción |
|--------|----------|-------------|
| GET | `/api/users/me/data-export` | Exportar datos personales |
| POST | `/api/users/me/delete-request` | Solicitar baja de cuenta |

---

## 🧪 Pruebas (Testing)

### Backend (Spring Boot)

Ejecutar todos los tests:

```powershell
cd backend
./mvnw test
```

Tests incluidos:
- `BusinessControllerTest` - Pruebas de sucursales
- `PaymentControllerTest` - Pruebas de pagos
- `PromotionControllerTest` - Pruebas de promociones
- `RefundControllerTest` - Pruebas de reembolsos
- `LegalHoldControllerTest` - Pruebas de bloqueo legal

### Frontend (React/Vite)

Ejecutar tests:

```powershell
cd frontend
npm run test
```

O con Vitest directamente:

```powershell
cd frontend
npx vitest run
```

Tests incluidos:
- `LoginPage.test.tsx` - Pruebas de página de login
- `api.test.tsx` - Pruebas de servicios API
- `CategoriesPage.test.tsx` - Pruebas de página de categorías
- `ProvidersPage.test.tsx` - Pruebas de página de proveedores
- `PricesPage.test.tsx` - Pruebas de página de precios
- `UsersPage.test.tsx` - Pruebas de página de usuarios
- `EmployeesPage.test.tsx` - Pruebas de página de empleados
- `SalesHistoryPage.test.tsx` - Pruebas de historial de ventas
- `ShiftsPage.test.tsx` - Pruebas de cierre de caja

### Android (Kotlin)

Ejecutar tests:

```powershell
cd android
./gradlew test
```

Tests incluidos:
- `PosViewModelTest.kt` - Pruebas de ViewModel de POS
- `SupplierViewModelTest.kt` - Pruebas de ViewModel de proveedores
- `EmployeeViewModelTest.kt` - Pruebas de ViewModel de empleados
- `CategoryViewModelTest.kt` - Pruebas de ViewModel de categorías
- `PriceViewModelTest.kt` - Pruebas de ViewModel de precios
- `UserViewModelTest.kt` - Pruebas de ViewModel de usuarios

---

## 📱 Despliegue en Producción

### Backend (Docker)

```powershell
docker-compose up -d backend
```

### Frontend (Build para producción)

```powershell
cd frontend
npm run build
# El resultado estará en frontend/dist/
```

### Android (Release)

1. Configurar `signing.properties` en `android/`
2. Configurar `BASE_URL` de release en `build.gradle.kts`
3. Ejecutar: `./gradlew assembleRelease`
4. El APK estará en `android/app/build/outputs/apk/release/`

---

## 🤝 Contribución

1. Haz fork del repositorio
2. Crea una rama para tu feature: `git checkout -b feature/nueva-funcionalidad`
3. Commit tus cambios: `git commit -m 'feat: agregar nueva funcionalidad'`
4. Push a tu rama: `git push origin tu-rama`
5. Abre un Pull Request

### Estándares de Código

- **Backend:** Java 17, Spring Boot 3.3.7, Lombok
- **Frontend:** React 19, TypeScript, Tailwind CSS
- **Android:** Kotlin, Jetpack Compose, Hilt, Retrofit, Room
- **Commits:** Convenciones de commits convencionales (feat, fix, docs, etc.)

---

## 📄 Licencia

Este proyecto está bajo la Licencia MIT. Ver el archivo `LICENSE` para más detalles.

---

## 📞 Soporte

Para soporte o consultas, contacta al equipo de desarrollo de Nivex POS.

---

## 📌 Estado del Proyecto

| Módulo | Estado | Porcentaje |
|--------|--------|------------|
| Backend | ✅ Completado | 100% |
| Frontend | ✅ Completado | 100% |
| Android | ✅ En desarrollo | 85% |
| Tests Backend | ✅ Completado | 100% |
| Tests Frontend | ✅ Completado | 80% |
| Tests Android | ⚠️ En progreso | 60% |
| Documentación | ✅ Completado | 100% |

---

¡Gracias por usar Nivex POS! 🚀
