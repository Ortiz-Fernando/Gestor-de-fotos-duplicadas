# Detección de duplicados — algoritmo y calibración

> **Estado:** documento de referencia del algoritmo v1 (plan v1.3). Se ampliará con
> resultados empíricos en Fases 7 y 10.

## Flujo v1

1. **Enumeración** recursiva (sin seguir enlaces), filtro por extensiones soportadas.
2. **Agrupar por tamaño** de archivo (solo tamaños repetidos pasan a SHA-256).
3. **SHA-256 por streaming** → duplicado exacto si los hashes coinciden (`EXACT`).
4. **pHash DCT-64** (solo formatos visualmente analizables) → comparación Hamming.
5. **Clasificación** según umbrales configurables.
6. **Agrupación** de resultados.

## Umbrales

```yaml
duplicate:
  perceptual:
    threshold: 10          # ≤ : POSIBLE_DUPLICADO_VISUAL
    review-threshold: 17   # (threshold, review-threshold]: SIMILAR_REVIEW (revisión humana)
```

## ⚠️ Calibración (importante)

- La distancia de Hamming **no equivale a un porcentaje exacto** de similitud visual.
  Por ejemplo, `Hamming = 5` sobre 64 bits **no** significa "92 % similar".
- Nada se elimina ni se marca prescindible automáticamente por pHash: los resultados
  visuales son siempre candidatos que requieren decisión humana.

### Medición con fotos reales (2026-09-04, catálogo de muebles)

Conjunto de prueba real de 40 imágenes (5 categorías de producto). Resultados:

| Distancia | Pares | Interpretación |
|---|---:|---|
| 0 | 7 | Misma foto copiada / reescalada → duplicado real |
| 4 | 1 | Misma foto con variación → duplicado real |
| 6 | 1 | Misma foto con variación → duplicado real |
| 7–17 | 0 | Zona vacía |
| 18–24 | 83 | Productos distintos (mismo estilo de fotografía) |

**Conclusión:** `threshold = 10` captura todos los duplicados reales de la muestra y no
produce falsos positivos (el primer par de productos distintos está en 18).
`review-threshold = 17` deja la banda de revisión justo por debajo del primer par de
productos distintos, evitando ruido en catálogos fotografiados con fondo similar.
Los valores son configurables y deberán revisarse si el tipo de colección cambia.

## Formatos

| Formato | SHA-256 (exacto) | pHash visual |
|---|---|---|
| JPG, JPEG, PNG, GIF, BMP, WEBP, TIFF | ✅ | ✅ (v1) |
| HEIC, HEIF, RAW | ✅ | ❌ (v1; estudio futuro) |
