# AGENTS.md

# Image Duplicate Manager — Development Instructions

## 1. Rol del agente

Actúa como **Senior Software Architect + Senior Java/Spring Boot Developer + Frontend Developer + Image Processing Engineer**.

El proyecto es una aplicación local para analizar carpetas, discos externos y unidades USB con fotografías e imágenes, detectar archivos duplicados o visualmente muy similares y permitir al usuario gestionarlos de forma segura.

Tu prioridad es, en este orden:

1. Corrección.
2. Seguridad de los archivos del usuario.
3. Mantenibilidad.
4. Claridad arquitectónica.
5. Rendimiento.
6. Experiencia de usuario.

No sacrifiques seguridad o corrección para obtener pequeñas mejoras de rendimiento.

---

# 2. Reglas fundamentales del agente

Antes de modificar código:

1. Inspecciona la estructura actual del proyecto.
2. Lee este `AGENTS.md` completo.
3. Lee `README.md` y la documentación existente en `docs/`.
4. Comprueba qué fase del proyecto está completada.
5. Comprueba el estado de Git.
6. No sobrescribas código existente sin entenderlo.
7. No introduzcas dependencias innecesarias.
8. No cambies decisiones arquitectónicas sin justificarlo.
9. Si una decisión contradice este documento, detente y explica el conflicto.
10. Si detectas una mejora importante, propónla antes de implementarla.

### Regla de desarrollo incremental

No desarrolles todo el proyecto de una sola vez.

Trabaja por fases.

Cada fase debe terminar con:

* compilación correcta;
* tests ejecutados;
* errores críticos solucionados;
* documentación actualizada;
* revisión de arquitectura;
* estado del proyecto registrado en `docs/development.md`.

No avances a la siguiente fase si la anterior contiene errores graves.

---

# 3. Objetivo de la aplicación

La aplicación debe permitir:

* seleccionar una carpeta o unidad;
* recorrerla recursivamente;
* detectar imágenes;
* obtener metadatos;
* calcular SHA-256;
* detectar duplicados exactos;
* calcular pHash en formatos compatibles;
* detectar posibles duplicados visuales;
* clasificar los resultados;
* mostrar grupos de imágenes;
* visualizar comparaciones;
* recomendar qué archivo conservar;
* calcular espacio recuperable;
* renombrar archivos;
* enviar archivos a la Papelera de Windows;
* registrar operaciones;
* permitir deshacer operaciones cuando sea técnicamente posible.

### Regla crítica

**Nunca eliminar, mover o marcar automáticamente como prescindible una imagen basándose únicamente en pHash.**

La decisión final siempre corresponde al usuario.

---

# 4. Arquitectura general

La aplicación será un **monolito local**.

```text
                         ┌──────────────────────┐
                         │      Navegador       │
                         │   React + TypeScript │
                         └──────────┬───────────┘
                                    │
                                    │ HTTP
                                    │
                              localhost:8080
                                    │
                         ┌──────────▼───────────┐
                         │     Spring Boot      │
                         │    Backend local     │
                         ├──────────────────────┤
                         │ REST API /api/**     │
                         │ Servicios de negocio │
                         │ Scanner               │
                         │ Hashing               │
                         │ Persistencia          │
                         │ Operaciones           │
                         │ Miniaturas            │
                         └──────────┬───────────┘
                                    │
                  ┌─────────────────┼─────────────────┐
                  │                 │                 │
                  ▼                 ▼                 ▼
               SQLite           Sistema de        Sistema de
               local            archivos          Papelera
```

## Principios

* 100 % local.
* Sin nube.
* Sin subida de fotografías.
* Sin servicios externos para analizar imágenes.
* Sin autenticación en v1.
* Sin microservicios.
* Sin servidor remoto.
* Sin acceso directo del frontend al sistema de archivos.

Todas las operaciones sobre archivos deben pasar por Spring Boot.

---

# 5. Arquitectura localhost

## Producción

La aplicación final debe funcionar mediante:

```text
http://localhost:8080
```

Spring Boot será responsable de:

* servir la API REST;
* servir la aplicación React compilada;
* acceder a SQLite;
* acceder al sistema de archivos;
* procesar imágenes;
* realizar operaciones de renombrado;
* gestionar la Papelera.

La aplicación debe escuchar únicamente en localhost.

No utilizar:

```text
0.0.0.0
```

La configuración debe favorecer:

```yaml
server:
  address: localhost
  port: 8080
```

Si Windows o el entorno requiriesen una configuración alternativa entre `localhost` y `127.0.0.1`, documentar la decisión.

---

# 6. Desarrollo frontend/backend

Durante desarrollo:

```text
React + Vite
localhost:5173
       │
       │ proxy /api
       ▼
Spring Boot
localhost:8080
```

Vite debe utilizar proxy para `/api`.

El frontend no debe contener rutas absolutas como:

```text
http://localhost:8080/api/...
```

Preferir:

```text
/api/...
```

Esto facilita desarrollo y producción.

---

# 7. Producción

El frontend React se compilará mediante:

```bash
npm run build
```

Los archivos generados se integrarán en:

```text
backend/src/main/resources/static/
```

El resultado final será:

```text
Usuario
   ↓
http://localhost:8080
   ↓
Spring Boot
   ├── React
   ├── REST API
   ├── SQLite
   └── sistema de archivos
```

El usuario final no debería necesitar ejecutar manualmente:

* Node.js;
* npm;
* Vite;
* Maven.

El objetivo final es poder empaquetar la aplicación para Windows.

---

# 8. Stack tecnológico

## Backend

* Java 25.
* Spring Boot 4.1.1.
* Maven.
* Spring Web.
* Spring Data JPA.
* Hibernate ORM.
* Hibernate Community Dialects.
* SQLite.
* Jakarta Validation.
* JUnit.
* MockMvc.

### Compatibilidad

Java:

```text
25
```

Spring Boot objetivo:

```text
4.1.1
```

Existe una decisión de respaldo:

```text
Spring Boot 3.5.16
```

Antes de generar el `pom.xml` definitivo, verificar:

* versión real de Hibernate gestionada por Boot;
* compatibilidad con SQLite;
* compatibilidad de `hibernate-community-dialects`;
* compilación con Java 25.

Si Boot 4.1.1 presenta una incompatibilidad real, documentar el problema mediante ADR y utilizar Boot 3.5.16.

No cambiar de versión simplemente por preferencia.

---

# 9. Dependencias

Dependencias previstas:

| Dependencia        |  Versión |
| ------------------ | -------: |
| Java               |       25 |
| Spring Boot        |    4.1.1 |
| SQLite JDBC        | 3.53.4.0 |
| metadata-extractor |   2.21.0 |
| JNA                |   5.19.1 |
| JNA Platform       |   5.19.1 |
| React              |   19.2.8 |

Las versiones de:

* TypeScript;
* Vite;
* `@vitejs/plugin-react`;

se resolverán al generar el proyecto, pero deben quedar registradas en `package.json`.

### Importante

No utilizar ninguna biblioteca externa específica de pHash en v1.

El pHash será una implementación propia.

---

# 10. Estructura del proyecto

La estructura objetivo es:

```text
Aplicacion de fotos duplicadas/
│
├── AGENTS.md
├── README.md
├── .gitignore
│
├── backend/
│   ├── pom.xml
│   ├── mvnw
│   ├── mvnw.cmd
│   ├── .mvn/
│   │
│   └── src/
│       ├── main/
│       │   ├── java/
│       │   │   └── com/
│       │   │       └── imagedupmanager/
│       │   │           │
│       │   │           ├── config/
│       │   │           │
│       │   │           ├── domain/
│       │   │           │
│       │   │           ├── repository/
│       │   │           │
│       │   │           ├── hashing/
│       │   │           │   ├── ImagePerceptualHasher.java
│       │   │           │   ├── DctPhashHasher.java
│       │   │           │   ├── HammingDistance.java
│       │   │           │   └── Sha256Hasher.java
│       │   │           │
│       │   │           ├── service/
│       │   │           │   ├── ScanService.java
│       │   │           │   ├── DuplicateService.java
│       │   │           │   ├── RenameService.java
│       │   │           │   ├── DeleteService.java
│       │   │           │   └── OperationService.java
│       │   │           │
│       │   │           ├── web/
│       │   │           │
│       │   │           └── ImageDuplicateManagerApplication.java
│       │   │
│       │   └── resources/
│       │       ├── application.yml
│       │       └── static/
│       │
│       └── test/
│           └── java/
│
├── frontend/
│   ├── package.json
│   ├── vite.config.ts
│   ├── tsconfig.json
│   └── src/
│       ├── components/
│       ├── pages/
│       ├── services/
│       ├── hooks/
│       ├── types/
│       ├── styles/
│       └── App.tsx
│
└── docs/
    ├── architecture.md
    ├── decisions.md
    ├── api.md
    ├── development.md
    └── packaging.md
```

---

# 11. Naming conventions

El paquete Java raíz será:

```text
com.imagedupmanager
```

Código:

* inglés;
* nombres descriptivos;
* clases en PascalCase;
* métodos y variables en camelCase;
* constantes en UPPER_SNAKE_CASE.

Ejemplo:

```java
public class DuplicateService {
}
```

La interfaz:

```java
public interface ImagePerceptualHasher {
}
```

La interfaz de usuario será completamente en español.

---

# 12. Separación entre negocio y algoritmos

Los algoritmos técnicos deben estar separados de la lógica de negocio.

## hashing/

```text
hashing/
├── ImagePerceptualHasher.java
├── DctPhashHasher.java
├── HammingDistance.java
└── Sha256Hasher.java
```

## service/

```text
service/
├── ScanService.java
├── DuplicateService.java
├── RenameService.java
├── DeleteService.java
└── OperationService.java
```

### Regla

`DctPhashHasher` no debe decidir:

* qué archivo eliminar;
* qué grupo crear;
* qué imagen conservar;
* qué operación mostrar al usuario.

Su responsabilidad es únicamente calcular el hash perceptual.

---

# 13. Detección de duplicados

El flujo v1 será:

```text
1. Enumerar archivos
        ↓
2. Filtrar formatos soportados
        ↓
3. Agrupar por tamaño
        ↓
4. SHA-256
        ↓
5. Duplicados exactos
        ↓
6. Analizar imágenes visualmente compatibles
        ↓
7. pHash DCT-64
        ↓
8. Distancia Hamming
        ↓
9. Clasificación
        ↓
10. Agrupación
        ↓
11. Mostrar resultados
```

---

# 14. Exploración del sistema de archivos

Utilizar:

```java
java.nio.file.Path
java.nio.file.Files
```

Preferir `Files.walkFileTree()` cuando sea apropiado.

El escáner debe:

* recorrer subdirectorios;
* no seguir enlaces simbólicos por defecto;
* detectar unidades no disponibles;
* tolerar archivos que desaparezcan durante el análisis;
* registrar errores sin detener todo el análisis;
* informar del progreso;
* permitir cancelación;
* no cargar todos los archivos en memoria.

Debe funcionar con:

* discos internos;
* discos externos;
* pendrives;
* carpetas profundas;
* rutas con espacios;
* nombres Unicode;
* nombres largos cuando Windows lo permita.

---

# 15. Formatos soportados

## Análisis completo

Se intentará realizar análisis visual de:

```text
JPG
JPEG
PNG
GIF
BMP
WEBP
TIFF
```

## Análisis exacto únicamente

Los formatos que no puedan ser decodificados de forma fiable:

```text
HEIC
HEIF
RAW
```

podrán ser:

* enumerados;
* registrados;
* calculados mediante SHA-256;
* detectados como duplicados exactos.

No se debe intentar crear un pHash falso sobre formatos no soportados.

---

# 16. SHA-256

SHA-256 será el mecanismo definitivo para determinar igualdad binaria.

Regla:

```text
mismo SHA-256
+
mismo contenido
=
duplicado exacto
```

El cálculo debe realizarse mediante streaming.

No cargar archivos completos en RAM.

Buffer objetivo:

```text
8 MB
```

como valor inicial configurable si fuera necesario.

---

# 17. Caché SHA-256

Se podrá reutilizar SHA-256 cuando sea razonablemente seguro.

Candidato de caché:

```text
tamaño
+
fecha de modificación
```

Si cambia:

* tamaño;
* fecha de modificación;

el hash debe considerarse potencialmente obsoleto.

Nunca utilizar una caché que pueda provocar una identificación incorrecta de contenido.

---

# 18. pHash

Implementar pHash propio basado en DCT.

Interfaz:

```java
public interface ImagePerceptualHasher {

    long hash(BufferedImage image);
}
```

Implementación:

```text
DctPhashHasher
```

El pHash será:

```text
64 bits
```

Proceso:

```text
imagen
 ↓
orientación EXIF
 ↓
escalado
 ↓
32 × 32
 ↓
escala de grises
 ↓
DCT
 ↓
selección de coeficientes
 ↓
mediana
 ↓
64 bits
```

La implementación debe estar aislada para permitir sustituirla posteriormente.

---

# 19. Orientación EXIF

Antes del cálculo perceptual:

* leer orientación EXIF;
* aplicar la transformación correspondiente;
* calcular el pHash sobre la orientación visual correcta.

Una misma fotografía girada no debería producir hashes completamente diferentes simplemente por la orientación EXIF.

Utilizar:

```text
metadata-extractor
```

para metadatos EXIF.

---

# 20. Distancia Hamming

La distancia entre dos pHash de 64 bits será calculada mediante XOR + `Long.bitCount`.

Conceptualmente:

```java
Long.bitCount(hashA ^ hashB)
```

No realizar operaciones de disco durante la comparación de hashes.

Los hashes deben permanecer en memoria durante la fase de comparación cuando sea razonable.

---

# 21. Umbrales perceptuales

Configuración inicial:

```yaml
duplicate:
  perceptual:
    threshold: 10
    review-threshold: 22
```

Interpretación:

```text
0–10
→ POSSIBLE_VISUAL

11–22
→ SIMILAR_REVIEW

>22
→ sin coincidencia visual relevante
```

Estos valores son iniciales y deben poder modificarse mediante configuración.

### Importante

Una distancia Hamming de 10 **no significa exactamente un porcentaje de similitud**.

No mostrar al usuario afirmaciones como:

```text
90 % iguales
```

salvo que posteriormente exista una metodología validada que lo justifique.

Mostrar preferentemente:

```text
Distancia perceptual: 7
```

y una explicación sencilla.

---

# 22. Categorías

Duplicados exactos:

```text
EXACT
```

Posibles duplicados visuales:

```text
POSSIBLE_VISUAL
```

Revisión humana:

```text
SIMILAR_REVIEW
```

`SIMILAR_REVIEW` no constituye un grupo definitivo.

Debe utilizarse como resultado para revisión.

---

# 23. Filtro dimensional

Antes de realizar comparaciones perceptuales se puede utilizar un filtro conservador basado en:

* ancho;
* alto;
* proporción.

El filtro debe ser:

```yaml
duplicate:
  visual:
    dimension-filter-enabled: true
```

Debe evitar falsos negativos importantes.

Nunca utilizar un filtro dimensional agresivo que pueda descartar fotografías visualmente equivalentes.

Este filtro no constituye un índice pHash.

---

# 24. Índice pHash

NO implementar inicialmente un índice avanzado de pHash.

En v1 se prioriza:

* simplicidad;
* corrección;
* mantenibilidad;
* facilidad de prueba.

No implementar:

* particiones complejas;
* árboles especializados;
* índices aproximados;
* subfirmas;
* estructuras de búsqueda avanzadas;

salvo que las pruebas reales demuestren que son necesarias.

Cualquier optimización de este tipo deberá realizarse en una fase posterior y documentarse mediante ADR.

---

# 25. Complejidad

No afirmar que el algoritmo v1 es subcuadrático con recall total.

El diseño v1 evita comparaciones innecesarias mediante:

* agrupación por tamaño para SHA-256;
* filtros dimensionales conservadores;
* procesamiento por etapas.

Pero la comparación perceptual general puede llegar a ser O(n²).

Esto es aceptable inicialmente para colecciones típicas.

Si las pruebas reales muestran problemas de rendimiento:

```text
medir
 ↓
identificar cuello de botella
 ↓
proponer solución
 ↓
ADR
 ↓
implementar
 ↓
comparar resultados
```

No optimizar prematuramente.

---

# 26. Agrupación

Los duplicados exactos deben agruparse mediante SHA-256.

Los duplicados visuales deben agruparse de forma controlada.

El sistema puede utilizar una unión transitiva, pero debe evitar agrupaciones excesivamente amplias provocadas por cadenas de similitud.

Ejemplo problemático:

```text
A ≈ B
B ≈ C
C ≈ D
D ≈ E
```

No asumir automáticamente:

```text
A ≈ E
```

La lógica de agrupación debe comprobarse contra el representante del grupo y mediante tests.

---

# 27. Recomendación de imagen a conservar

El sistema puede sugerir una imagen como:

```text
RECOMENDADA
```

Criterios posibles:

1. mayor resolución;
2. mayor calidad o tamaño cuando resulte indicativo;
3. formato preferible;
4. archivo más antiguo/nuevo según criterio configurable;
5. ubicación;
6. ausencia de señales de archivo problemático.

La recomendación nunca debe ejecutar ninguna acción automáticamente.

Debe aparecer como:

```text
Sugerencia: conservar esta imagen
```

y permitir al usuario cambiar la decisión.

---

# 28. Modelo de datos

## Scan

Campos:

```text
id
rootPath
estado
inicio
fin
numeroArchivos
numeroErrores
opciones
```

Estados:

```text
RUNNING
COMPLETED
FAILED
CANCELLED
```

---

## ImageRecord

Campos principales:

```text
id
scanId
rutaAbsoluta
nombre
carpeta
extension
tamaño
lastModified
sha256
phash
ancho
alto
orientacionEXIF
analizable
estado
```

Estado de imagen:

```text
ACTIVO
A_PAPELERA
BORRADO
```

El pHash será:

```text
Long
```

---

## DupGroup

Campos:

```text
id
scanId
categoria
idRecomendada
numeroMiembros
espacioRecuperable
```

Categorías:

```text
EXACT
POSSIBLE_VISUAL
```

No persistir `SIMILAR_REVIEW` como grupo definitivo en v1.

---

## OperationLog

Campos:

```text
id
tipo
idImagen
rutaOrigen
rutaDestino
fecha
reversible
deshechaEn
```

Tipos:

```text
RENOMBRAR
MOVER
PAPELERA
UNDO
```

---

## AppSetting

Campos:

```text
clave
valor
```

Ejemplos:

```text
ultimaCarpeta
perceptualThreshold
reviewThreshold
```

---

# 29. SQLite

SQLite será la base de datos local.

La base de datos almacenará:

* metadatos;
* hashes;
* grupos;
* operaciones;
* configuración.

No almacenar imágenes originales en SQLite.

No almacenar miniaturas grandes como BLOB salvo que exista una razón técnica justificada.

Preferir almacenamiento local de miniaturas.

---

# 30. SQLite WAL

Configurar SQLite utilizando:

```text
WAL
```

y:

```text
busy_timeout
```

cuando corresponda.

Los índices iniciales serán:

```text
sha256
(scanId, tamaño)
(scanId, phash)
(scanId, grupoId)
ruta
```

No crear índices innecesarios.

---

# 31. Miniaturas

Las miniaturas se almacenarán en una caché local.

Estructura orientativa:

```text
data/
└── thumbnails/
```

Nunca sobrescribir la imagen original para generar una miniatura.

Las miniaturas deberán poder regenerarse.

Si una miniatura desaparece:

```text
regenerarla
```

en lugar de considerar corrupta la imagen original.

---

# 32. API REST

Todas las rutas REST estarán bajo:

```text
/api/**
```

Ejemplos orientativos:

```text
GET    /api/scans
POST   /api/scans
GET    /api/scans/{id}
POST   /api/scans/{id}/cancel

GET    /api/groups
GET    /api/groups/{id}

GET    /api/images/{id}
GET    /api/images/{id}/thumbnail

POST   /api/images/{id}/rename
POST   /api/images/{id}/trash

GET    /api/operations
POST   /api/operations/{id}/undo
```

No crear endpoints innecesarios.

Los contratos definitivos deben documentarse en:

```text
docs/api.md
```

---

# 33. DTOs

No exponer directamente entidades JPA mediante la API.

Utilizar:

```text
DTO
 ↓
Service
 ↓
Entity
```

y:

```text
Entity
 ↓
Mapper
 ↓
DTO
```

cuando sea necesario.

---

# 34. Validación

Validar todas las entradas provenientes de la API.

Especialmente:

* rutas;
* nombres de archivo;
* identificadores;
* operaciones;
* parámetros de configuración;
* umbrales.

No confiar en datos enviados desde React.

---

# 35. Manejo de errores

Implementar manejo global de excepciones.

La API debe devolver respuestas consistentes.

No mostrar stack traces al usuario.

Registrar detalles técnicos en logs.

La interfaz debe recibir mensajes comprensibles en español.

Ejemplo:

```text
No se ha podido acceder a la carpeta seleccionada.
```

en lugar de:

```text
java.nio.file.AccessDeniedException
```

---

# 36. Renombrado

El renombrado debe realizarse mediante un servicio específico:

```text
RenameService
```

Antes de renombrar:

1. comprobar existencia;
2. comprobar nombre;
3. comprobar caracteres inválidos;
4. comprobar conflicto;
5. mostrar resultado previsto;
6. solicitar confirmación.

Nunca sobrescribir silenciosamente otro archivo.

Si existe:

```text
foto.jpg
```

y se intenta crear:

```text
foto.jpg
```

la operación debe bloquearse o solicitar una estrategia explícita.

---

# 37. Papelera de Windows

La eliminación lógica debe utilizar la Papelera de Windows.

Utilizar:

```text
JNA
```

y las APIs apropiadas de Windows/Shell32.

La intención es utilizar una operación equivalente a:

```text
FOF_ALLOWUNDO
```

para que el archivo pueda recuperarse desde la Papelera de Windows.

Nunca realizar:

```java
Files.delete(...)
```

como mecanismo normal de eliminación desde la interfaz de usuario.

Una eliminación definitiva solo podría existir como operación explícita y separada, si posteriormente se decide implementarla.

---

# 38. Historial y Undo

Toda operación destructiva o potencialmente destructiva debe registrarse.

Ejemplos:

```text
RENOMBRAR
MOVER
PAPELERA
```

Registrar:

```text
ruta origen
ruta destino
fecha
tipo
estado
```

Cuando sea técnicamente posible:

```text
UNDO
```

debe restaurar la operación.

Si no es posible deshacerla:

* indicarlo claramente;
* no presentar la operación como reversible.

---

# 39. Seguridad de archivos

Reglas absolutas:

### Nunca

* borrar automáticamente;
* sobrescribir archivos;
* modificar imágenes originales durante el análisis;
* cambiar extensiones automáticamente;
* subir fotografías;
* enviar fotografías a APIs externas;
* seguir enlaces simbólicos sin control;
* asumir que un archivo sigue existiendo.

### Siempre

* validar rutas;
* comprobar existencia;
* gestionar excepciones;
* registrar operaciones;
* solicitar confirmación;
* trabajar con copias/miniaturas para visualización.

---

# 40. Frontend

Tecnologías:

```text
React
TypeScript
Vite
```

La interfaz será:

* moderna;
* clara;
* responsive;
* orientada a escritorio;
* completamente en español.

No es necesario implementar un diseño excesivamente complejo.

La prioridad es:

```text
funcionalidad
+
claridad
+
seguridad
```

---

# 41. Pantallas previstas

## Inicio

Debe permitir:

```text
Seleccionar carpeta
```

y mostrar:

```text
Última carpeta utilizada
```

Botón principal:

```text
ANALIZAR
```

---

## Análisis

Mostrar:

```text
Archivos encontrados
Archivos analizados
Progreso
Errores
Tiempo transcurrido
```

Permitir:

```text
CANCELAR
```

---

## Resultados

Mostrar:

```text
Duplicados exactos
Posibles duplicados visuales
Similares para revisión
Espacio recuperable
```

---

## Comparador

Mostrar imágenes de forma clara:

```text
Imagen A        Imagen B
```

Información:

```text
nombre
ruta
tamaño
dimensiones
fecha
distancia perceptual
SHA-256
```

---

# 42. UI en español

Todos los textos visibles al usuario deben estar en español.

Ejemplos:

```text
Analizar
Cancelar
Duplicado exacto
Posible duplicado visual
Revisión necesaria
Conservar
Enviar a la papelera
Renombrar
Deshacer
Espacio recuperable
```

Los nombres internos del código permanecen en inglés.

---

# 43. Progreso y operaciones largas

El análisis puede tardar mucho.

No bloquear el hilo HTTP.

Utilizar ejecución asíncrona cuando corresponda.

El usuario debe poder consultar:

```text
estado
progreso
errores
```

y cancelar el análisis.

Evitar crear miles de hilos.

Controlar correctamente:

* Executor;
* concurrencia;
* acceso a SQLite;
* cancelación.

---

# 44. Memoria

Nunca cargar todas las imágenes originales en memoria.

Para SHA-256:

```text
streaming
```

Para pHash:

```text
imagen → representación reducida → cálculo → liberar recursos
```

Para visualización:

```text
miniatura
```

No utilizar imágenes originales gigantes en la interfaz salvo que sea estrictamente necesario.

---

# 45. Tests

Los tests son obligatorios.

Debe existir una batería que cubra como mínimo:

### Sistema de archivos

* carpeta vacía;
* subdirectorios;
* archivo inexistente durante análisis;
* permisos insuficientes;
* unidad desconectada;
* nombres Unicode.

### SHA-256

* archivos idénticos;
* archivos diferentes;
* archivos grandes.

### pHash

* imagen idéntica;
* imagen redimensionada;
* imagen comprimida;
* imagen ligeramente modificada;
* imagen girada mediante EXIF;
* imágenes claramente diferentes.

### Agrupación

* duplicados exactos;
* duplicados visuales;
* cadena de similitud;
* grupos independientes.

### Operaciones

* renombrado correcto;
* conflicto de nombres;
* envío a Papelera;
* Undo.

---

# 46. Tests de seguridad

Crear tests específicos para comprobar que:

```text
pHash similar
≠
eliminación automática
```

y:

```text
conflicto de nombre
≠
sobrescritura
```

También comprobar que:

```text
archivo desaparecido
≠
fallo completo del escaneo
```

cuando pueda gestionarse de forma segura.

---

# 47. Documentación

Mantener:

```text
docs/architecture.md
docs/decisions.md
docs/api.md
docs/development.md
docs/packaging.md
```

## architecture.md

Debe explicar:

* arquitectura;
* backend;
* frontend;
* SQLite;
* filesystem;
* hashing;
* localhost.

## decisions.md

Debe contener ADRs.

Especialmente:

* pHash propio;
* pHash 64-bit;
* umbral;
* SQLite;
* localhost;
* ausencia de índice avanzado en v1;
* versión Spring Boot;
* estrategia de Windows Trash.

## development.md

Debe indicar:

```text
fase actual
última fase completada
errores conocidos
siguiente paso
tests
decisiones pendientes
```

---

# 48. ADR

No cambiar una decisión arquitectónica importante silenciosamente.

Si se modifica:

```text
Spring Boot
SQLite
pHash
arquitectura
modelo de datos
estrategia de agrupación
algoritmo de detección
```

crear o actualizar un ADR.

Formato:

```text
ADR-001 — Título
Estado: Accepted / Superseded / Proposed
Contexto:
Decisión:
Consecuencias:
```

---

# 49. Git

Utilizar Git desde el comienzo.

Commits pequeños y descriptivos.

Preferir:

```text
feat:
fix:
refactor:
test:
docs:
build:
chore:
```

Ejemplos:

```text
feat: add recursive image scanner
feat: implement SHA-256 duplicate detection
test: add perceptual hash tests
docs: document local architecture
fix: handle disconnected drive during scan
```

No realizar commits gigantes que mezclen:

* backend;
* frontend;
* documentación;
* refactor;
* nuevas funcionalidades;

sin necesidad.

---

# 50. Fases oficiales

## Fase 0 — Repositorio

Crear:

```text
git init
.gitignore
README.md
```

---

## Fase 1 — Documentación

Crear:

```text
docs/architecture.md
docs/api.md
docs/decisions.md
```

Documentar las decisiones arquitectónicas.

---

## Fase 2 — Esqueleto

Crear:

```text
Spring Boot
Maven
mvnw
application.yml
React
Vite
TypeScript
```

Antes de continuar:

* comprobar Java 25;
* comprobar Maven;
* comprobar Spring Boot;
* comprobar Hibernate;
* comprobar SQLite;
* comprobar `hibernate-community-dialects`;
* compilar backend;
* ejecutar tests;
* ejecutar frontend.

Configurar:

```text
localhost:8080
```

y:

```text
localhost:5173
```

durante desarrollo.

---

## Fase 3 — Persistencia

Implementar:

* entidades;
* repositorios;
* SQLite;
* Hibernate;
* WAL;
* índices.

---

## Fase 4 — Exploración

Implementar:

```text
ScanService
```

con:

* recorrido recursivo;
* detección de imágenes;
* progreso;
* cancelación;
* manejo de errores.

---

## Fase 5 — SHA-256

Implementar:

```text
Sha256Hasher
```

con:

* streaming;
* caché;
* tests.

---

## Fase 6 — pHash

Implementar:

```text
ImagePerceptualHasher
DctPhashHasher
HammingDistance
```

con tests.

---

## Fase 7 — Detección

Implementar:

```text
DuplicateService
```

con:

* exactos;
* visuales;
* Hamming;
* clasificación;
* agrupación;
* espacio recuperable.

---

## Fase 8 — API REST

Implementar:

* controllers;
* DTOs;
* validación;
* excepciones;
* MockMvc.

---

## Fase 9 — Frontend

Implementar:

* selector de carpeta;
* análisis;
* progreso;
* resultados;
* grupos;
* comparador;
* decisiones.

---

## Fase 10 — Comparación visual

Calibrar:

```text
threshold = 10
reviewThreshold = 22
```

utilizando imágenes reales de prueba.

No modificar los umbrales sin documentarlo.

---

## Fase 11 — Renombrado

Implementar:

```text
RenameService
```

con:

* validación;
* conflictos;
* tests;
* confirmación.

---

## Fase 12 — Papelera y operaciones

Implementar:

```text
DeleteService
OperationService
```

con:

* JNA;
* Shell32;
* Papelera;
* historial;
* Undo;
* confirmaciones.

---

## Fase 13 — Optimización

Solo después de medir.

Posibles mejoras:

* índice pHash;
* procesamiento por lotes;
* análisis incremental;
* caché avanzada.

Toda mejora importante requiere ADR.

---

## Fase 14 — Tests finales

Ejecutar la suite completa.

Comprobar:

* backend;
* frontend;
* filesystem;
* hashing;
* detección;
* agrupación;
* operaciones.

---

## Fase 15 — Empaquetado Windows

Preparar:

```text
React build
↓
Spring Boot static
↓
fat jar
↓
jpackage
↓
aplicación Windows
```

Objetivo final:

```text
ImageDuplicateManager.exe
```

La aplicación debe poder:

1. iniciar;
2. levantar Spring Boot;
3. abrir el navegador;
4. mostrar la interfaz;
5. utilizar SQLite local;
6. analizar carpetas;
7. gestionar archivos.

El usuario final no debería necesitar instalar Node.js ni ejecutar comandos de desarrollo.

---

# 51. Configuración inicial

El `application.yml` debe centralizar:

```yaml
server:
  address: localhost
  port: 8080

duplicate:
  perceptual:
    threshold: 10
    review-threshold: 22
  visual:
    dimension-filter-enabled: true
```

No introducir valores mágicos en el código.

Los umbrales deben proceder de configuración.

---

# 52. Estado del proyecto

Mantener siempre actualizado:

```text
docs/development.md
```

Debe responder rápidamente:

```text
¿Qué está hecho?
¿Qué funciona?
¿Qué no funciona?
¿Qué fase estamos ejecutando?
¿Cuál es el siguiente paso?
```

Esto permite que cualquier sesión posterior de Cline pueda continuar el proyecto correctamente.

---

# 53. Regla para Cline

Cuando el usuario pida una nueva funcionalidad:

1. identificar la fase;
2. comprobar dependencias;
3. comprobar arquitectura;
4. proponer cambios;
5. implementar solo lo necesario;
6. compilar;
7. ejecutar tests;
8. documentar.

No implementar funcionalidades fuera de alcance sin autorización.

---

# 54. Regla contra sobreingeniería

No introducir:

* microservicios;
* Docker para producción;
* Kubernetes;
* Redis;
* Kafka;
* Elasticsearch;
* bases de datos remotas;
* autenticación;
* sistemas cloud;

salvo que el proyecto cambie explícitamente de objetivo.

La aplicación está diseñada para ejecutarse localmente en un ordenador Windows.

---

# 55. Regla de rendimiento

Primero:

```text
correcto
```

Después:

```text
medido
```

Después:

```text
optimizado
```

Nunca:

```text
optimizado
```

antes de demostrar que existe un problema.

---

# 56. Regla de seguridad de operaciones

Toda acción potencialmente destructiva debe seguir:

```text
detección
 ↓
previsualización
 ↓
decisión del usuario
 ↓
confirmación
 ↓
ejecución
 ↓
registro
 ↓
posible Undo
```

Nunca:

```text
detección
 ↓
eliminación automática
```

---

# 57. Regla final

Este proyecto debe comportarse como una herramienta profesional de gestión de fotografías.

El usuario debe poder confiar en que:

* analizar no modifica archivos;
* detectar no elimina archivos;
* pHash no decide por él;
* renombrar no sobrescribe;
* enviar a Papelera no equivale a borrar definitivamente;
* las operaciones quedan registradas;
* los errores de una imagen no destruyen el análisis completo;
* sus fotografías permanecen en su ordenador.

**La seguridad y la confianza del usuario están por encima de cualquier automatización.**
