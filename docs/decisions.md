# Decisiones de diseño (ADR)

Registro de decisiones arquitectónicas. Convención: `D<n>` + estado (`aceptada` /
`reemplazada` / `diferida`).

---

## D1 — Perceptual hashing interno, sin dependencia externa
**Estado:** aceptada · **Fecha:** 2026-09-03

- **Contexto:** no existe una biblioteca de perceptual hashing mantenida en Maven Central.
- **Decisión:** implementar pHash DCT de 64 bits como módulo interno en `hashing/`,
  aislado tras la interfaz `ImagePerceptualHasher`.
- **Consecuencia:** `DctPhashHasher` y `HammingDistance` son clases propias y testeables.
  Sustituir por biblioteca externa en el futuro solo requiere una nueva implementación
  de `ImagePerceptualHasher`.

## D2 — Idioma y paquete base
**Estado:** aceptada · **Fecha:** 2026-09-03

- Paquete base: `com.imagedupmanager`.
- Código (clases, métodos, variables, comentarios técnicos): **inglés**.
- Interfaz de usuario y mensajes mostrados al usuario: **español**.

## D3 — Clasificación en tres categorías y umbrales configurables
**Estado:** aceptada · **Fecha:** 2026-09-03

- `DUPLICADO_EXACTO` → SHA-256 idéntico.
- `POSIBLE_DUPLICADO_VISUAL` → distancia Hamming pHash-64 ≤ `duplicate.perceptual.threshold` (10).
- `SIMILAR_REVIEW` → `threshold` < distancia ≤ `duplicate.perceptual.review-threshold` (22).
  Solo aparece en resultados de revisión humana / comparador; nunca se agrupa automáticamente.
- **Ninguna imagen se elimina ni se marca prescindible únicamente por pHash.**
- La distancia de Hamming **no equivale** a un porcentaje exacto de similitud visual;
  debe calibrarse con fotografías reales.

## D4 — Formatos analizados en v1
**Estado:** aceptada · **Fecha:** 2026-09-03

- Análisis visual completo: JPG, JPEG, PNG, GIF, BMP, WEBP, TIFF.
- HEIC, HEIF, RAW: en v1 solo enumeración y duplicado exacto por SHA-256.
  El análisis perceptual de estos formatos se estudiará más adelante.

## D5 — Versiones de dependencias (verificación en Fase 2)
**Estado:** aceptada · **Fecha:** 2026-09-03

- Spring Boot **4.1.1** (estable). Documentación oficial: compatible con Java 17–26
  → Java 25 ✅. *Respaldo documentado: Boot 3.5.16* (si la prueba mínima de Fase 2
  detectara incompatibilidad con JPA/Hibernate/SQLite, se adopta el respaldo y se
  registra aquí el motivo).
- `sqlite-jdbc` **3.53.4.0**, `metadata-extractor` **2.21.0**, JNA/JNA-platform **5.19.1**,
  React **19.2.8**.
- `hibernate-community-dialects`: versión alineada con la de Hibernate gestionada por el
  BOM de Spring Boot (se confirma antes de escribir el `pom.xml` definitivo).
- Los starters se generan con Spring Initializr (nombres oficiales, nunca escritos a mano).

**Resultado de la verificación (2026-09-03):** la prueba mínima de la Fase 2 pasó.
Combinación confirmada y funcionando: Boot **4.1.1** + Java **25.0.1** + Spring Data JPA
+ Hibernate ORM **7.4.5.Final** (gestionado por el BOM) + `hibernate-community-dialects`
(también gestionado por el BOM, sin fijar versión) + `SQLiteDialect` + sqlite-jdbc
**3.53.4.0** (conexión real `SELECT 1` verificada en test). **No fue necesario aplicar
el respaldo 3.5.16.**

## D6 — Organización del módulo de hashing
**Estado:** aceptada · **Fecha:** 2026-09-03

- `hashing/` contiene los algoritmos técnicos puros:
  `ImagePerceptualHasher` (interfaz), `DctPhashHasher`, `HammingDistance`, `Sha256Hasher`.
- `service/` contiene la lógica de negocio (`ScanService`, `DuplicateService`,
  `RenameService`, `DeleteService`, `OperationService`) y **no** contiene algoritmos
  de hashing: solo los invoca.

## D7 — Estrategia de detección v1 (sin índice de pHash)
**Estado:** aceptada · **Fecha:** 2026-09-03

- Prioridad en v1: **corrección y mantenibilidad** sobre optimización.
- Flujo: enumerar → agrupar por tamaño → SHA-256 → duplicados exactos → pHash-64
  (formatos D4) → comparación Hamming → clasificación (D3) → agrupación.
- Se evita O(n²) sobre toda la colección mediante un **filtro dimensional conservador**
  (dimensiones almacenadas) previo a la comparación Hamming, que es en memoria y barata.
- **Diferido:** el índice especializado de pHash (particiones/subfirmas) solo se
  implementará si las pruebas con colecciones reales demuestran que es necesario.
- Complejidad declarada de la comparación visual en v1: O(m²) en memoria sobre enteros
  de 64 bits, con `m` = imágenes con pHash de un mismo análisis.

---

## Histórico de versiones del plan

| Versión | Cambios |
|---|---|
| v1 | Arquitectura inicial completa |
| v1.1 | pHash interno, UI español/paquete, umbrales configurables, formatos |
| v1.2 | Verificación de versiones, detección simplificada, módulo `hashing/` separado |
| v1.3 | `Sha256Hasher` fijado en `hashing/` (D6); Fase 2 con prueba mínima de compilación antes del resto del backend |
