"""
Descarga un checkpoint preentrenado de GhostFaceNetV1 (HamadYA/GhostFaceNets)
y lo convierte a TFLite (float16) listo para usar en la app Android.

Uso:
    python convert_to_tflite.py

Salida:
    checkpoints/GhostFaceNet_W1.3_S2_ArcFace.h5   (checkpoint original, descargado)
    output/ghostfacenet.tflite                    (modelo convertido, para copiar a
                                                    app/src/main/assets/)

El checkpoint es un modelo Keras completo (arquitectura + pesos) entrenado con
ArcFace sobre MS1MV3: GhostNetV1, ancho 1.3, stride 2, cabezal GDC, embedding
de 512-D. No requiere reconstruir la arquitectura a mano.
"""

import os

# Debe fijarse ANTES de importar tensorflow: keras_cv_attention_models / el
# checkpoint fueron guardados con Keras 2 (tf.keras clásico), y TensorFlow
# >= 2.16 usa Keras 3 por defecto, que no es 100% compatible con ese formato.
os.environ.setdefault("TF_USE_LEGACY_KERAS", "1")

import numpy as np
import requests
import tensorflow as tf

CHECKPOINT_URL = "https://github.com/HamadYA/GhostFaceNets/releases/download/v1.3/GhostFaceNet_W1.3_S2_ArcFace.h5"
CHECKPOINT_DIR = os.path.join(os.path.dirname(__file__), "checkpoints")
CHECKPOINT_PATH = os.path.join(CHECKPOINT_DIR, "GhostFaceNet_W1.3_S2_ArcFace.h5")
OUTPUT_DIR = os.path.join(os.path.dirname(__file__), "output")
TFLITE_PATH = os.path.join(OUTPUT_DIR, "ghostfacenet.tflite")

INPUT_SIZE = 112


def download_checkpoint():
    if os.path.exists(CHECKPOINT_PATH) and os.path.getsize(CHECKPOINT_PATH) > 0:
        print(f"[1/4] Checkpoint ya existe en {CHECKPOINT_PATH}, se omite descarga.")
        return
    os.makedirs(CHECKPOINT_DIR, exist_ok=True)
    print(f"[1/4] Descargando checkpoint desde {CHECKPOINT_URL} ...")
    response = requests.get(CHECKPOINT_URL, stream=True, allow_redirects=True)
    response.raise_for_status()
    total = 0
    with open(CHECKPOINT_PATH, "wb") as fh:
        for chunk in response.iter_content(chunk_size=1024 * 1024):
            fh.write(chunk)
            total += len(chunk)
    print(f"      Descargados {total / (1024 * 1024):.1f} MB")


def load_model():
    print(f"[2/4] Cargando modelo Keras desde {CHECKPOINT_PATH} ...")
    model = tf.keras.models.load_model(CHECKPOINT_PATH, compile=False)
    print(f"      input_shape={model.input_shape} output_shape={model.output_shape}")
    assert model.output_shape[-1] == 512, "Se esperaba un embedding de 512 dimensiones"
    return model


def convert_to_tflite(model):
    print("[3/4] Convirtiendo a TFLite (float16) ...")
    os.makedirs(OUTPUT_DIR, exist_ok=True)
    converter = tf.lite.TFLiteConverter.from_keras_model(model)
    converter.optimizations = [tf.lite.Optimize.DEFAULT]
    converter.target_spec.supported_types = [tf.float16]
    tflite_model = converter.convert()
    with open(TFLITE_PATH, "wb") as fh:
        fh.write(tflite_model)
    size_mb = os.path.getsize(TFLITE_PATH) / (1024 * 1024)
    print(f"      Guardado {TFLITE_PATH} ({size_mb:.1f} MB)")


def sanity_check():
    """Corre una inferencia dummy para confirmar que el .tflite converted funciona."""
    print("[4/4] Verificando el modelo convertido con una entrada aleatoria ...")
    interpreter = tf.lite.Interpreter(model_path=TFLITE_PATH)
    interpreter.allocate_tensors()
    input_details = interpreter.get_input_details()
    output_details = interpreter.get_output_details()

    dummy = np.random.uniform(-1, 1, size=input_details[0]["shape"]).astype(np.float32)
    interpreter.set_tensor(input_details[0]["index"], dummy)
    interpreter.invoke()
    embedding = interpreter.get_tensor(output_details[0]["index"])
    print(f"      Embedding shape: {embedding.shape}, dtype: {embedding.dtype}")
    print(f"      Norma L2 de ejemplo: {np.linalg.norm(embedding):.4f}")
    print("      OK: el modelo corre inferencia correctamente.")


def main():
    download_checkpoint()
    model = load_model()
    convert_to_tflite(model)
    sanity_check()
    print("\nListo. Copia este archivo a la app Android:")
    print(f"  {TFLITE_PATH}")
    print("  -> app/src/main/assets/ghostfacenet.tflite")


if __name__ == "__main__":
    main()
