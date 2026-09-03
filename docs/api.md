# API REST — Image Duplicate Manager

> **Estado:** borrador. Se refinará en la Fase 8 (API REST). Contrato base aprobado en el plan v1.3.

Todas las rutas bajo `/api`. Respuestas JSON. Servidor local `http://localhost:8080`.
El **código** de los campos es inglés; las **descripciones** mostradas al usuario son español.

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
