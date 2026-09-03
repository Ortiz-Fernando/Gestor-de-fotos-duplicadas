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
    review-threshold: 22   # (threshold, review-threshold]: SIMILAR_REVIEW (revisión humana)
```

## ⚠️ Calibración (importante)

- La distancia de Hamming **no equivale a un porcentaje exacto** de similitud visual.
  Por ejemplo, `Hamming = 5` sobre 64 bits **no** significa "92 % similar".
- Los valores 10 y 22 son **iniciales** y deben calibrarse con fotografías reales
  (misma foto recompimida, escalada, con marca de agua, ligeramente editada, etc.).
- Nada se elimina ni se marca prescindible automáticamente por pHash: los resultados
  visuales son siempre candidatos que requieren decisión humana.

## Formatos

| Formato | SHA-256 (exacto) | pHash visual |
|---|---|---|
| JPG, JPEG, PNG, GIF, BMP, WEBP, TIFF | ✅ | ✅ (v1) |
| HEIC, HEIF, RAW | ✅ | ❌ (v1; estudio futuro) |
