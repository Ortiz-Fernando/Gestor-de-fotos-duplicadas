# Estado del desarrollo

Este documento describe la **fase actual**, lo completado, lo pendiente y las
decisiones en curso. Consultar antes de cada sesión de desarrollo.

## Fase actual

**Fase 15 — Empaquetado Windows:** fat jar integrado y verificado ✅.
*Opcional pendiente: ejecutable `.exe` con `jpackage` (documentado en `docs/packaging.md`).*

## Funcionalidades completadas

| Fase | Contenido | Estado |
|---|---|---|
| 0 | Repositorio: `.gitignore`, `README.md`. Repositorio Git inicializado (rama `main`, 3 commits base) | ✅ |
| 1 | `docs/architecture.md`, `docs/decisions.md` (ADR D1–D9), `docs/api.md`, `docs/duplicate-detection.md`, `docs/packaging.md` | ✅ |
| 2 | Esqueleto backend (Spring Initializr Boot 4.1.1 + Java 25). Prueba mínima superada: JPA/Hibernate 7.4.5 + SQLiteDialect arrancan y conectan | ✅ |
| 3 | Persistencia: entidades, enums, repositorios, WAL + índice único explícito, directorios de datos. Tests de integración (7) en verde | ✅ |
| 4 | Exploración: `ScanService` (walk recursivo, filtro D4, errores tolerantes, progreso, cancelación, persistencia por lotes). Tests (6) en verde | ✅ |
| 5 | SHA-256: `hashing/Sha256Hasher` (streaming, buffer 8 MB) + `Sha256CacheValidator` + `HashingException`. Tests (9) en verde | ✅ |
| 6 | pHash perceptual: `hashing/ImagePerceptualHasher`, `DctPhashHasher` (DCT-64), `HammingDistance`, `ExifOrientationNormalizer`. Tests (12) en verde | ✅ |
| 7 | Detección/agrupación: `DuplicateService`, `DuplicateUpdater`, `DuplicateProperties`. Tests end-to-end (2) en verde | ✅ |
| 8 | API REST: controladores, DTOs, validación, manejo global de errores en español. Tests MockMvc (4) en verde | ✅ |
| 9 | Frontend React+TS+Vite: Inicio, Progreso, Resultados, Grupo (UI español) + `GET /api/images/{id}/content`. Build Vite verificado | ✅ |
| 10 | Comparación/calibración: `CalibrationReportGenerator`, medición con 40 fotos reales, umbrales calibrados (threshold=10, review=17) documentados (ADR D9) | ✅ |
| 11 | Renombrado seguro: `RenameService` + `RenameController`. Tests (4) en verde | ✅ |
| 12 | Papelera y operaciones: `DeleteService` (JNA/Shell32), `OperationService` (historial/Undo), `OperationController`, `TrashController`. Tests (5 nuevos) en verde | ✅ |
| 13 | Optimización: diferida por diseño (sin cuello de botella demostrado; AGENTS #55). Sin cambios | ✅ (documentado) |
| 14 | Tests finales: suite backend completa (53 tests) en verde + build frontend OK | ✅ |
| 15 | Empaquetado: acciones de UI activadas (renombrar/papelera/historial), `npm run build:prod` → `static/`, fat jar generado y verificado en `localhost:8080` (UI + API). `jpackage`/`.exe` opcional | ✅ (fat jar); ⏳ exe opcional |

## Funcionalidades pendientes (plan v1.3)

- **Fase 3** Persistencia: entidades, repositorios, dialecto SQLite (WAL, `busy_timeout`).
- **Fase 4** Exploración: `ScanService` (walk recursivo, unidades, progreso, cancelación).
- **Fase 5** `hashing/Sha256Hasher` (streaming) + caché.
- **Fase 6** `hashing/` perceptual: `ImagePerceptualHasher`, `DctPhashHasher`, `HammingDistance` (tests).
- **Fase 7** Detección/agrupación (`DuplicateService`).
- **Fase 8** API REST (controllers, DTOs, validación).
- **Fase 9** Frontend (React/TS, español).
- **Fase 10** Comparación visual y calibración de umbrales.
- **Fase 11** Renombrado (`RenameService`).
- **Fase 12** Papelera y operaciones (`DeleteService`, `OperationService`).
- **Fase 13** Optimización (solo si pruebas reales lo exigen).
- **Fase 14** Tests completos.
- **Fase 15** Empaquetado Windows.

## Problemas conocidos

- Git no está instalado en el equipo: no se puede inicializar el repositorio ni crear
  commits hasta instalarlo (`winget install --id Git.Git -e`).
- Warnings cosméticos en tests (JDK 24+): `--enable-native-access` para sqlite-jdbc y
  agente Mockito. Sin impacto funcional; se silenciarán al empaquetar (Fase 15).

## Resultado de la verificación de compatibilidad (Fase 2)

- Boot **4.1.1** (generado por Initializr) + Java **25.0.1**: compila y ejecuta tests.
- Hibernate ORM **7.4.5.Final** gestionado por el BOM; `hibernate-community-dialects`
  también lo gestiona el BOM (sin necesidad de fijar versión).
- Dialecto `org.hibernate.community.dialect.SQLiteDialect` + `sqlite-jdbc` **3.53.4.0**:
  conexión real verificada. **No fue necesario aplicar el respaldo D5 (Boot 3.5.16).**

## Notas para la sesión

1. Los cambios de cada fase deben compilar y pasar tests antes de continuar.
2. Ejecutar Maven desde `backend/` con `.\mvnw.cmd` (el directorio de trabajo no
   persiste entre comandos: usar `Set-Location` explícito con ruta absoluta).

