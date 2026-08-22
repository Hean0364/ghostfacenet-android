# model_prep — Conversión de GhostFaceNet a TFLite

Convierte el checkpoint preentrenado `GhostFaceNetV1-1.3-2 (ArcFace, MS1MV3)` de
[HamadYA/GhostFaceNets](https://github.com/HamadYA/GhostFaceNets) a un archivo
`.tflite` listo para usar en la app Android (LiteRT).

## Setup (Windows, PowerShell)

```powershell
python -m venv model_prep\.venv
model_prep\.venv\Scripts\python.exe -m pip install -r model_prep\requirements.txt
```

## Ejecutar la conversión

```powershell
model_prep\.venv\Scripts\python.exe model_prep\convert_to_tflite.py
```

Esto hace, en orden:

1. Descarga `GhostFaceNet_W1.3_S2_ArcFace.h5` (~16 MB) a `model_prep/checkpoints/`
   si no existe todavía.
2. Carga el modelo Keras completo (arquitectura + pesos). El checkpoint ya
   incluye la arquitectura, así que no hace falta reconstruirla a mano.
3. Convierte a TFLite con cuantización `float16` y lo guarda en
   `model_prep/output/ghostfacenet.tflite` (~7.8 MB).
4. Corre una inferencia de verificación (entrada aleatoria) para confirmar que
   el modelo convertido funciona.

## Detalles del modelo

- Arquitectura: GhostNetV1 (ancho 1.3, stride 2) + cabezal GDC.
- Entrada: imagen de rostro alineada, `112x112x3`, píxeles normalizados a `[-1, 1]`.
- Salida: embedding de `512` dimensiones (sin normalizar; hay que L2-normalizar
  antes de comparar con similitud coseno).
- Entrenado con ArcFace sobre MS1MV3. Métricas del checkpoint original:
  lfw 99.68%, cfp_fp 93.31%, agedb_30 96.92%.
- Por qué `float16` y no `int8`: las capas `PReLU` del modelo dan problemas
  conocidos al cuantizar a entero completo (ver
  [issue #47](https://github.com/HamadYA/GhostFaceNets/issues/47)). `float16`
  reduce el tamaño a la mitad sin ese riesgo y corre rápido en CPU vía XNNPACK.

## Siguiente paso

Copiar `model_prep/output/ghostfacenet.tflite` a
`app/src/main/assets/ghostfacenet.tflite` una vez creado el proyecto Android.

## Nota sobre `TF_USE_LEGACY_KERAS`

El checkpoint se guardó con Keras 2 clásico. TensorFlow >= 2.16 usa Keras 3 por
defecto, que no carga bien este formato antiguo. El script fija
`TF_USE_LEGACY_KERAS=1` automáticamente antes de importar TensorFlow para usar
`tf-keras` (el paquete de compatibilidad) en su lugar.
