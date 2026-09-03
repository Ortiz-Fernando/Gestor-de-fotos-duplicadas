# Estado del desarrollo

Este documento describe la **fase actual**, lo completado, lo pendiente y las
decisiones en curso. Consultar antes de cada sesión de desarrollo.

## Fase actual

**Fase 3 — Persistencia (entidades, repositorios, SQLite WAL).** *Pendiente de iniciar.*

## Funcionalidades completadas

| Fase | Contenido | Estado |
|---|---|---|
| 0 | Repositorio: `.gitignore`, `README.md`. `git init` **pendiente** (Git no instalado en el sistema) | ⚠️ Docs OK; git pendiente |
| 1 | `docs/architecture.md`, `docs/decisions.md` (ADR D1–D7), `docs/api.md`, `docs/duplicate-detection.md`, `docs/packaging.md` | ✅ |
| 2 | Esqueleto backend generado con Spring Initializr (Boot 4.1.1, Java 25) bajo `backend/`. **Prueba mínima superada**: `mvnw compile` + `mvnw test` → contexto Spring con JPA/Hibernate + SQLiteDialect arranca y `SELECT 1` funciona. Versiones resueltas reales: Hibernate ORM **7.4.5.Final**, sqlite-jdbc **3.53.4.0** | ✅ |

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

