# Postman Collection - POS System API v1.0

Colección completa de 45+ endpoints organizados por módulo funcional.

## Importar Collection

1. Abrir Postman
2. File → Import
3. Seleccionar `POS System API - v1.0.postman_collection.json`
4. Crear environment "Local Development":
   - `base_url = http://localhost:8080`

## Autenticación

La mayoría de los endpoints requieren autenticación Bearer JWT.

### Flujo rápido:
1. Abrir `Auth > Login`
2. Usar credenciales por defecto: `admin@pos.com` / `Admin123!`
3. Click "Send" → el script guarda automáticamente el token
4. Todos los demás requests ya incluyen `{{auth_token}}` en el header

## Estructura de la Colección

| Folder | Endpoints | Auth Required |
|--------|-----------|---------------|
| Auth | Login, Register, Refresh, Logout, Me | Parcial |
| Users | List, By ID, Profile, Permissions | Sí |
| Products | CRUD completo + stock | Sí |
| Categories | CRUD completo | Sí |
| Inventory - Stock | Entradas, salidas, ajustes, transferencias, mermas | Sí |
| Inventory - Kardex | Historial de movimientos y stock | Sí |
| Inventory - Alerts | Alertas activas, no leídas, resolver, reportes | Sí |
| Taxes | CRUD, asignación a productos, cálculo precios | Sí |
| Configuration | CRUD configuraciones del sistema | Sí |
| Audit Logs | Listado, exportación, tipos de acciones | Sí |
| Health | Health check, métricas, DB | No |

## Tests Automáticos

Los requests de Health y Login incluyen tests JavaScript que verifican:
- Código de estado HTTP
- Estructura de respuesta JSON
- Asignación automática de tokens

## Notas

- Los IDs en las URLs (`/1`) son ejemplos; ajustar según los datos creados
- La colección usa variables de colección (`auth_token`, `refresh_token`) que se persisten entre requests
- Los endpoints de Stock requieren un producto y bodega existentes
