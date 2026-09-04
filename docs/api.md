# API REST — Image Duplicate Manager

> **Estado:** API REST básica implementada en la **Fase 8** (análisis, grupos, imágenes).
> Los endpoints de explorador de carpetas, vistas previas y operaciones sobre archivos se
> implementarán en Fases 9-12.

Todas las rutas bajo `/api`. Respuestas JSON. Servidor local `http://localhost:8080`.
El **código** de los campos es inglés; las **descripciones** mostradas al usuario son español.

## Endpoints implementados

```text
GET    /api/health                        Estado del servicio

POST   /api/scans                         Inicia análisis asíncrono  { rootPath }  -> 202
GET    /api/scans                         Lista de análisis
GET    /api/scans/{id}                    Detalle de un análisis
POST   /api/scans/{id}/cancel             Solicita cancelación
POST   /api/scans/{id}/detect             Ejecuta detección/agrupación

GET    /api/scans/{scanId}/groups         Grupos de un análisis
GET    /api/groups/{id}                   Detalle de grupo (miembros + recomendada)

GET    /api/images/{id}                   Metadatos de una imagen

POST   /api/images/{id}/rename/preview    Previsualiza un renombrado  { newName }
POST   /api/images/{id}/rename            Aplica un renombrado (no sobrescribe)
POST   /api/images/{id}/trash             Envía a la Papelera  { confirm: true } (obligatorio)

GET    /api/operations                    Historial de operaciones
POST   /api/operations/{id}/undo          Deshace una operación reversible (rename)
```

## Esbozo de endpoints

```text
GET    /api/health                        Estado del servicio

GET    /api/drives                        Unidades disponibles (C:, D:, E:, …)
GET    /api/folders?path=...&showHidden=  Contenido de un directorio (explorador)

POST   /api/analyses                      Inicia análisis  { rootPath, options }
GET    /api/analyses/{id}                 Detalle de un análisis
GET    /api/analyses/{id}/status          Progreso  { total, processed, errors, status, percentage }
POST   /api/analyses/{id}/cancel          Cancelar análisis

GET    /api/analyses/{id}/groups          Grupos de duplicados (EXACT | POSSIBLE_VISUAL)
GET    /api/groups/{id}                   Detalle de grupo (miembros, espacio recuperable, recomendada)

GET    /api/images/{id}                   Metadatos de una imagen
GET    /api/images/{id}/preview           Miniatura (stream)
GET    /api/images/{id}/content           Imagen original (stream, bajo demanda)
GET    /api/images/{id}/compare?otherId=  Comparación visual (banda SIMILAR_REVIEW)

POST   /api/files/rename/preview          Previsualización de renombrado
POST   /api/files/rename                  Aplica renombrado
POST   /api/files/delete                  Envía a papelera (requiere confirmación)

GET    /api/operations                    Historial de operaciones
POST   /api/operations/{id}/undo          Deshacer (cuando sea técnicamente posible)
```

## Categorías de resultados

| Categoría (API) | Significado | Comportamiento |
|---|---|---|
| `EXACT` | SHA-256 idéntico | Grupo seguro; acciones tras confirmación explícita |
| `POSSIBLE_VISUAL` | pHash dentro de `threshold` | Requiere revisión humana |
| `SIMILAR_REVIEW` | Banda entre umbrales | Solo revisión/comparación; sin agrupación |
