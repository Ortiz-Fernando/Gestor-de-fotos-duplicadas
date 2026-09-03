# Arquitectura — Image Duplicate Manager

Versión de referencia del plan: **v1.3** (aprobada).

## Visión general

Monolito local que se ejecuta en el equipo del usuario y escucha **solo** en
`localhost:8080`. Spring Boot sirve la interfaz React (compilada) y la API REST.
La base de datos SQLite almacena únicamente metadatos, hashes, resultados y
operaciones (nunca imágenes). El análisis de carpetas es 100 % local.

```text
Navegador (Chrome/Edge)
        │  http://localhost:8080
        ▼
┌────────────────────────────────────────────────┐
│             SPRING BOOT (monolito)             │
│  React (build) en /static                      │
│  API REST /api/**                              │
│  service/   → lógica de negocio                │
│  hashing/   → algoritmos técnicos (aislados)   │
│  SQLite (WAL) · miniaturas en cache local      │
└────────────────────────────────────────────────┘
```

## Principios

1. **Seguridad primero**: nunca eliminar automáticamente; toda operación destructiva
   requiere confirmación explícita; nada se marca prescindible por pHash.
2. **Localidad**: no se envían imágenes a servicios externos.
3. **Separación de responsabilidades**:
   - `web/` (controllers REST) → sin lógica de negocio.
   - `service/` → orquestación y lógica de negocio.
   - `hashing/` → algoritmos técnicos de hashing, aislados e intercambiables.
   - `repository/` → acceso a datos (Spring Data JPA).
4. **Configuración externa** (`application.yml` + `@ConfigurationProperties`).
5. **Sin comparaciones O(n²) innecesarias sobre toda la colección** (ver estrategia v1).

## Decisiones clave

Ver `docs/decisions.md` (ADR D1–D7).

| Área | Decisión |
|---|---|
| Duplicados exactos | SHA-256 por streaming, agrupando primero por tamaño de archivo |
| Similitud visual | pHash DCT-64 interno (`hashing/`), distancia de Hamming |
| Clasificación | `EXACT` · `POSSIBLE_VISUAL` · `SIMILAR_REVIEW` (umbrales configurables) |
| Formato de imágenes | JPG/JPEG/PNG/GIF/BMP/WEBP/TIFF analizados visualmente; HEIC/HEIF/RAW solo SHA-256 en v1 |
| UI | Español. Código Java/TS en inglés |

## Servicios principales (`service/`)

- `ScanService` — exploración recursiva, progreso, cancelación, manejo de unidades no disponibles.
- `DuplicateService` — detección (exacta y visual) y agrupación.
- `RenameService` — renombrado seguro (previsualización, resolución de conflictos).
- `DeleteService` — envío a la papelera (JNA/Shell32), nunca borrado silencioso.
- `OperationService` — historial de operaciones y deshacer.

## Configuración principal

```yaml
server:
  address: localhost
  port: 8080

duplicate:
  perceptual:
    threshold: 10          # banda "posible duplicado visual"
    review-threshold: 22   # banda "similar → revisión humana"
```

> El umbral **no** es un porcentaje de similitud. Requiere calibración con fotografías
> reales (ver `docs/duplicate-detection.md`).

## Modelo de datos (resumen)

`Scan`, `ImageRecord` (sha256, phash, dimensiones, orientación EXIF), `DupGroup`
(categoría `EXACT` | `POSSIBLE_VISUAL`), `OperationLog`, `AppSetting`. Detalle en
`docs/database.md` *(se generará en Fase 3)*.
