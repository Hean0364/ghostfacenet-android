"""
Organiza un subconjunto del dataset LFW (ya extraido en test_data/lfw/) en tres
carpetas pensadas para probar la app GhostFaceNet de punta a punta:

- enroll/<Persona>/...jpg       -> importar estas con la pantalla "Importar"
- probe_known/<Persona>/...jpg  -> fotos DE LAS MISMAS personas, no importadas.
                                    Deberian reconocerse correctamente.
- probe_unknown/<Persona>/...jpg -> personas que NUNCA se importan.
                                    Deberian salir como "Persona no reconocida".

Uso:
    python prepare_test_set.py
"""

import os
import random
import shutil

random.seed(42)

BASE_DIR = os.path.dirname(__file__)
LFW_DIR = os.path.join(BASE_DIR, "lfw")
ENROLL_DIR = os.path.join(BASE_DIR, "enroll")
PROBE_KNOWN_DIR = os.path.join(BASE_DIR, "probe_known")
PROBE_UNKNOWN_DIR = os.path.join(BASE_DIR, "probe_unknown")

NUM_KNOWN_PEOPLE = 12       # personas que SI se importan
MIN_IMAGES_KNOWN = 6        # minimo de fotos para separar enroll/probe
ENROLL_IMAGES_PER_PERSON = 3

NUM_UNKNOWN_PEOPLE = 6      # personas que NUNCA se importan


def reset_dir(path):
    if os.path.exists(path):
        shutil.rmtree(path)
    os.makedirs(path)


def main():
    people = sorted(os.listdir(LFW_DIR))
    counts = []
    for name in people:
        folder = os.path.join(LFW_DIR, name)
        if not os.path.isdir(folder):
            continue
        n = len([f for f in os.listdir(folder) if f.lower().endswith(".jpg")])
        counts.append((name, n))

    with_many = [c for c in counts if c[1] >= MIN_IMAGES_KNOWN]
    with_one = [c for c in counts if c[1] == 1]

    random.shuffle(with_many)
    random.shuffle(with_one)

    known_people = with_many[:NUM_KNOWN_PEOPLE]
    unknown_people = with_one[:NUM_UNKNOWN_PEOPLE]

    reset_dir(ENROLL_DIR)
    reset_dir(PROBE_KNOWN_DIR)
    reset_dir(PROBE_UNKNOWN_DIR)

    print(f"Personas conocidas (se importan): {len(known_people)}")
    for name, n in known_people:
        src = os.path.join(LFW_DIR, name)
        images = sorted(f for f in os.listdir(src) if f.lower().endswith(".jpg"))
        enroll_imgs = images[:ENROLL_IMAGES_PER_PERSON]
        probe_imgs = images[ENROLL_IMAGES_PER_PERSON:]

        enroll_dst = os.path.join(ENROLL_DIR, name)
        probe_dst = os.path.join(PROBE_KNOWN_DIR, name)
        os.makedirs(enroll_dst, exist_ok=True)
        os.makedirs(probe_dst, exist_ok=True)

        for img in enroll_imgs:
            shutil.copy(os.path.join(src, img), os.path.join(enroll_dst, img))
        for img in probe_imgs:
            shutil.copy(os.path.join(src, img), os.path.join(probe_dst, img))

        print(f"  - {name}: {len(enroll_imgs)} para enrolar, {len(probe_imgs)} para probar")

    print(f"\nPersonas desconocidas (NUNCA se importan): {len(unknown_people)}")
    for name, n in unknown_people:
        src = os.path.join(LFW_DIR, name)
        images = [f for f in os.listdir(src) if f.lower().endswith(".jpg")]
        dst = os.path.join(PROBE_UNKNOWN_DIR, name)
        os.makedirs(dst, exist_ok=True)
        for img in images:
            shutil.copy(os.path.join(src, img), os.path.join(dst, img))
        print(f"  - {name}: {len(images)} foto(s)")

    print("\nListo. Carpetas generadas en test_data/: enroll/, probe_known/, probe_unknown/")


if __name__ == "__main__":
    main()
