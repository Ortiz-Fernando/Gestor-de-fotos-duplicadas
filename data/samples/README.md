# Carpeta de muestras para calibración (Fase 10)

Coloca aquí **fotografías reales** para calibrar los umbrales del pHash
(`duplicate.perceptual.threshold` y `review-threshold`). Esta carpeta está ignorada por
Git: tus imágenes nunca se subirán al repositorio.

## Cómo organizar las muestras

Usa las subcarpetas preparadas:

```text
data/samples/
├── 01_duplicados_exactos/   → la MISMA foto copiada tal cual (mismos bytes).
├── 02_visuales_similares/   → la misma foto, pero reescalada, recomprimida (JPEG/PNG),
│                              con marca de agua, o con retoque ligero. Deberían acabar
│                              como "posible duplicado visual".
├── 03_revision_humana/      → fotos de la misma escena pero más modificadas (recorte,
│                              filtros, luz muy distinta). Opcional: sirven para la banda
│                              de revisión humana (11-22).
└── 04_distintas/            → fotos claramente diferentes (sin relación).
```

### Reglas simples

1. **Suficiente variedad**: incluye fotos de cámara/móvil, JPEG y PNG, de varios tamaños.
2. **Unos 10-20 grupos** de la misma foto por carpeta es un buen punto de partida.
3. Nombra cada "familia" igual (p. ej. `playa_A.jpg`, `playa_B.jpg` … dentro de
   `02_visuales_similares`) para poder saber qué imágenes se corresponden.
4. Las fotos de `01_duplicados_exactos` deben ser **copias byte a byte** (copia/pega del archivo).

Cuando hayas colocado las fotos, dímelo (por ejemplo: *"ya están las muestras"*) y
ejecutaré el análisis de calibración para medir distancias reales del pHash y ajustar los
umbrales documentando el resultado.
