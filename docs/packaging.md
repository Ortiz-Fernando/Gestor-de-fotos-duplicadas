# Empaquetado para Windows

> **Estado:** documento de planificación. Se completará en la **Fase 15**.

## Plan previsto

1. `npm run build` en `frontend/` → copia del resultado a
   `backend/src/main/resources/static/`.
2. `.\mvnw.cmd package` en `backend/` → fat jar ejecutable.
3. Ejecución: `http://localhost:8080` (Spring Boot sirve interfaz + API).
4. Opcional posterior: `jpackage`/script `.bat` de arranque y parada.

## Requisitos de ejecución

- JDK 25 (o JRE empaquetado mediante `jlink`/`jpackage`).
- Sin dependencias de red en tiempo de ejecución.
