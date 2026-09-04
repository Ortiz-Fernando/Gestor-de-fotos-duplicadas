# Empaquetado para Windows

## Estado actual (Fase 15)

✅ El **fat jar** funciona y sirve la interfaz + API en `http://localhost:8080`.

## Pasos verificados

```powershell
# 1. Compilar el frontend y copiarlo a Spring Boot (static/)
cd frontend
npm run build:prod        # -> backend/src/main/resources/static/

# 2. Empaquetar el backend como fat jar
cd backend
.\mvnw.cmd -DskipTests package
# -> backend/target/imageduplicatemanager-0.0.1-SNAPSHOT.jar (~68 MB)

# 3. Ejecutar
cd backend
java -jar target\imageduplicatemanager-0.0.1-SNAPSHOT.jar
# Abrir http://localhost:8080
```

La prueba automatizada confirmó: `GET /api/health` → `{"status":"OK"}` y la página de
inicio (React) servida desde `static/` con código 200.

## Datos locales

- Base de datos SQLite, miniaturas y papelera interna: `../data/` (relativa al directorio
  de ejecución; durante el desarrollo se crea en la raíz del repo: `data/database/`,
  `data/thumbnails/`, `data/trash/`).
- El servidor escucha solo en `localhost:8080` (configuración en `application.yml`).

## Opcional pendiente: ejecutable `.exe` (jpackage)

```powershell
cd backend
# 1. Generar el runtime mínimo con jlink (Java 25)
# 2. jpackage --input target --main-jar imageduplicatemanager-0.0.1-SNAPSHOT.jar
#      --name "ImageDuplicateManager" --win-console --type app-image
```

`jpackage` requiere un JDK con soporte de empaquetado. Al lanzar el `.exe`, la aplicación
debe: iniciar Spring Boot, abrir el navegador en `localhost:8080` y usar la carpeta de
datos local. Esta parte es opcional y se puede acometer cuando se quiera distribuir a
otros usuarios sin JDK.

## Requisitos

- JDK 25 (o JRE empaquetado con `jlink`).
- Sin dependencias de red en tiempo de ejecución (todo local).

