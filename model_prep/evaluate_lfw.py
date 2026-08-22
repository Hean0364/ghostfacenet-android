"""
Evalua ghostfacenet.tflite contra el benchmark estandar LFW (Labeled Faces in
the Wild), en el mismo formato que usa el repo original de GhostFaceNets para
reportar su precision (99.68% en el checkpoint W1.3_S2).

Si logramos un numero cercano a ese valor, confirma que la conversion a
TFLite (arquitectura + preprocesamiento) se hizo correctamente.

Uso:
    python evaluate_lfw.py

Descarga automaticamente datasets/lfw.bin (~61 MB) la primera vez.
"""

import os
import pickle

os.environ.setdefault("TF_USE_LEGACY_KERAS", "1")

import numpy as np
import requests
import tensorflow as tf

LFW_BIN_URL = "https://github.com/leondgarse/Keras_insightface/releases/download/v1.0.0/lfw.bin"
DATASETS_DIR = os.path.join(os.path.dirname(__file__), "datasets")
LFW_BIN_PATH = os.path.join(DATASETS_DIR, "lfw.bin")
TFLITE_PATH = os.path.join(os.path.dirname(__file__), "output", "ghostfacenet.tflite")

BATCH_SIZE = 128
USE_FLIP_TTA = True  # promedia embedding de la imagen y su espejo horizontal


def download_lfw_bin():
    if os.path.exists(LFW_BIN_PATH) and os.path.getsize(LFW_BIN_PATH) > 0:
        print(f"[1/5] lfw.bin ya existe en {LFW_BIN_PATH}, se omite descarga.")
        return
    os.makedirs(DATASETS_DIR, exist_ok=True)
    print(f"[1/5] Descargando {LFW_BIN_URL} ...")
    r = requests.get(LFW_BIN_URL, stream=True, allow_redirects=True)
    r.raise_for_status()
    with open(LFW_BIN_PATH, "wb") as fh:
        for chunk in r.iter_content(chunk_size=1024 * 1024):
            fh.write(chunk)
    print(f"      OK ({os.path.getsize(LFW_BIN_PATH) / 1024 / 1024:.1f} MB)")


def load_pairs():
    print("[2/5] Cargando pares desde lfw.bin ...")
    with open(LFW_BIN_PATH, "rb") as fh:
        bins, issame_list = pickle.load(fh, encoding="bytes")
    print(f"      {len(bins)} imagenes, {len(issame_list)} pares "
          f"({sum(issame_list)} misma-persona, {len(issame_list) - sum(issame_list)} distinta-persona)")
    return bins, np.array(issame_list, dtype=bool)


def decode_images(bins):
    print("[3/5] Decodificando JPEGs y normalizando ...")
    images = np.empty((len(bins), 112, 112, 3), dtype=np.float32)
    for i, jpg_bytes in enumerate(bins):
        img = tf.io.decode_jpeg(jpg_bytes, channels=3).numpy().astype(np.float32)
        images[i] = (img - 127.5) * 0.0078125  # -> rango aprox [-1, 1]
    return images


def embed_all(images):
    print(f"[4/5] Generando embeddings ({'con' if USE_FLIP_TTA else 'sin'} flip TTA) ...")
    interpreter = tf.lite.Interpreter(model_path=TFLITE_PATH)
    interpreter.resize_tensor_input(0, [BATCH_SIZE, 112, 112, 3])
    interpreter.allocate_tensors()
    input_index = interpreter.get_input_details()[0]["index"]
    output_index = interpreter.get_output_details()[0]["index"]

    def run_batches(imgs):
        n = imgs.shape[0]
        out = np.empty((n, 512), dtype=np.float32)
        for start in range(0, n, BATCH_SIZE):
            end = min(start + BATCH_SIZE, n)
            batch = imgs[start:end]
            if batch.shape[0] < BATCH_SIZE:
                pad = np.zeros((BATCH_SIZE - batch.shape[0], 112, 112, 3), dtype=np.float32)
                batch = np.concatenate([batch, pad], axis=0)
            interpreter.set_tensor(input_index, batch)
            interpreter.invoke()
            out[start:end] = interpreter.get_tensor(output_index)[: end - start]
            if start % (BATCH_SIZE * 10) == 0:
                print(f"      {end}/{n}")
        return out

    embeddings = run_batches(images)
    if USE_FLIP_TTA:
        flipped = images[:, :, ::-1, :]
        embeddings = embeddings + run_batches(flipped)

    norms = np.linalg.norm(embeddings, axis=1, keepdims=True)
    embeddings = embeddings / norms
    return embeddings


def evaluate(embeddings, issame):
    print("[5/5] Calculando similitud coseno por par y buscando el mejor umbral ...")
    emb_a = embeddings[0::2]
    emb_b = embeddings[1::2]
    sims = np.sum(emb_a * emb_b, axis=1)  # cosine similarity (ya normalizados)

    thresholds = np.linspace(-1.0, 1.0, 401)
    best_acc, best_thresh = 0.0, 0.0
    for t in thresholds:
        preds = sims > t
        acc = np.mean(preds == issame)
        if acc > best_acc:
            best_acc, best_thresh = acc, t

    same_mean = sims[issame].mean()
    diff_mean = sims[~issame].mean()

    print("\n===== Resultado LFW =====")
    print(f"Pares evaluados:            {len(sims)}")
    print(f"Similitud media (misma):    {same_mean:.4f}")
    print(f"Similitud media (distinta): {diff_mean:.4f}")
    print(f"Mejor accuracy:             {best_acc * 100:.3f}%  (umbral cos = {best_thresh:.3f})")
    print(f"Referencia del paper (checkpoint original, no-TFLite): 99.68%")
    print("==========================")


def main():
    download_lfw_bin()
    bins, issame = load_pairs()
    images = decode_images(bins)
    embeddings = embed_all(images)
    evaluate(embeddings, issame)


if __name__ == "__main__":
    main()
