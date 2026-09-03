# Image Duplicate Manager

Aplicación local para analizar carpetas de imágenes, detectar duplicados exactos e
imágenes visualmente similares, y gestionarlos de forma **segura** (renombrar, mover,
enviar a la papelera) manteniendo un historial de operaciones.

- Funciona **100 % local** (`http://localhost:8080`). Las imágenes nunca salen del equipo.
- Sin eliminaciones automáticas: toda operación destructiva requiere confirmación explícita.
- Interfaz en español.

## Stack

| Capa | Tecnología |
|---|---|
| Backend | Java 25 · Spring Boot · Maven · Spring Data JPA · Hibernate · SQLite |
| Análisis | SHA-256 (duplicados exactos) · pHash DCT-64 interno (similitud visual) |
| Frontend | React · TypeScript · Vite |

## Estado actual del proyecto

> **Fase 2 en curso** — Esqueleto backend + prueba mínima de compilación (Boot + JPA + SQLite).
> Ver `docs/development.md` para el estado detallado y `docs/decisions.md` para las decisiones (ADR).

## Estructura

```text
backend/    API REST y motor de análisis (Spring Boot + SQLite)
frontend/   Interfaz React (español)
docs/       Arquitectura, API, ADR, estado de desarrollo
data/       Base de datos SQLite y caché de miniaturas (local, no versionado)
```

## Requisitos

- JDK 25 (verificado: `25.0.1`)
- Node.js ≥ 22 (verificado: `v24.14.0`)
- Maven **no** es necesario: se usa el Maven Wrapper (`mvnw`) incluido en `backend/`.

## Ejecución (desarrollo)

```powershell
# Backend (Spring Boot en http://localhost:8080)
cd backend
.\mvnw.cmd spring-boot:run

# Frontend (Vite dev server en http://localhost:5173, proxy hacia :8080)
cd frontend
npm install
npm run dev
```

> Durante el desarrollo Vite se usa en `5173` con proxy a la API. En producción, el
> build de React se copia a `backend/src/main/resources/static/` y Spring Boot sirve
> toda la aplicación en `http://localhost:8080` (ver `docs/packaging.md`).

## Documentación

- `docs/architecture.md` — Arquitectura.
- `docs/api.md` — API REST.
- `docs/decisions.md` — Decisiones de diseño (ADR) e histórico.
- `docs/development.md` — Estado del desarrollo por fases.
- `docs/duplicate-detection.md` — Algoritmo y calibración de umbrales.
