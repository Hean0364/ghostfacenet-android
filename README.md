# GhostFaceNet Android

Aplicación Android para reconocimiento facial **local**, basada en GhostFaceNet y TensorFlow Lite. La aplicación recibe una fotografía desde la cámara, la galería o el selector de archivos, detecta el rostro principal y lo compara con los perfiles disponibles en una base de datos Room/SQLite.

El procesamiento de reconocimiento se realiza en el dispositivo:

1. Se corrige la orientación EXIF de la imagen.
2. Se detecta el rostro de mayor tamaño mediante ML Kit.
3. Se alinean los ojos y se recorta el rostro a `112 × 112` píxeles.
4. GhostFaceNet genera un embedding facial de 512 dimensiones.
5. El embedding se normaliza con norma L2.
6. Se calcula la similitud coseno contra los embeddings almacenados.
7. Se muestran la mejor coincidencia y las candidatas más cercanas.

> Estado actual: la aplicación permite consultar perfiles y reconocer rostros. Los perfiles y sus fotografías deben estar disponibles previamente en la base local; en la interfaz actual no existe un formulario para crear o editar perfiles.



## Funcionalidades

- Reconocimiento desde:
  - Cámara del dispositivo.
  - Photo Picker de Android.
  - Selector de archivos como alternativa.
- Corrección automática de la orientación EXIF.
- Detección del rostro más grande cuando una imagen contiene varios rostros.
- Alineación y recorte facial antes de la inferencia.
- Inferencia on-device con un modelo `.tflite`.
- Comparación 1:N contra todos los embeddings de personas activas.
- Soporte para varias fotografías por persona.
- Lista de perfiles almacenados y pantalla de detalle.
- Visualización de las fotografías asociadas a cada perfil.
- Umbral de similitud configurable desde **Ajustes**.
- Persistencia del umbral mediante `SharedPreferences`.
- Persistencia local mediante Room sobre SQLite.
- Procesamiento de imágenes y reconocimiento sin enviar fotografías a un servidor desde la aplicación Android.



## Requisitos



### Android

- Android Studio compatible con Android Gradle Plugin `8.13.2`.
- JDK `17`.
- Android SDK `34`.
- Dispositivo o emulador con API `26` o superior.
- Permiso de cámara para utilizar la captura directa.
- Modelo `ghostfacenet.tflite` ubicado en:

```text
app/src/main/assets/ghostfacenet.tflite
```

El modelo no está incluido en el código fuente por defecto. Debe generarse o conseguirse siguiendo la sección [Preparar el modelo](#preparar-el-modelo).

### Conversión del modelo

Para generar el modelo se necesita:

- Python 3.
- Conexión a Internet durante la descarga del checkpoint y, opcionalmente, del benchmark LFW.
- Dependencias definidas en `model_prep/requirements.txt`.



## Inicio rápido



### 1. Preparar el entorno Python

Desde la raíz del proyecto, en PowerShell:

```powershell
python -m venv model_prep\.venv
model_prep\.venv\Scripts\python.exe -m pip install -r model_prep\requirements.txt
```



### 2. Generar el modelo TFLite

```powershell
model_prep\.venv\Scripts\python.exe model_prep\convert_to_tflite.py
```

El script:

1. Descarga el checkpoint si todavía no existe.
2. Carga la arquitectura y los pesos de GhostFaceNet.
3. Convierte el modelo a TensorFlow Lite con cuantización `float16`.
4. Ejecuta una inferencia de comprobación.

Después, copia el resultado a los assets de Android:

```powershell
New-Item -ItemType Directory -Force app\src\main\assets | Out-Null
Copy-Item model_prep\output\ghostfacenet.tflite app\src\main\assets\ghostfacenet.tflite -Force
```



### 3. Compilar e instalar

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat installDebug
```

El APK de depuración se genera en:

```text
app/build/outputs/apk/debug/app-debug.apk
```

También se puede abrir el proyecto desde Android Studio y ejecutar la configuración `app`.

## Preparar el modelo

El modelo utilizado es `GhostFaceNetV1-1.3-2 (ArcFace, MS1MV3)` del proyecto [GhostFaceNets](https://github.com/HamadYA/GhostFaceNets).

### Especificaciones


| Propiedad        | Valor                                            |
| ---------------- | ------------------------------------------------ |
| Arquitectura     | GhostNetV1, ancho 1.3, stride 2, con cabezal GDC |
| Entrada          | Rostro alineado de `112 × 112 × 3`               |
| Preprocesamiento | `(píxel - 127.5) / 128`                          |
| Rango de entrada | Aproximadamente `[-1, 1]`                        |
| Salida           | Embedding de 512 valores `Float`                 |
| Comparación      | Similitud coseno                                 |
| Conversión       | TensorFlow Lite con `float16`                    |


La aplicación normaliza cada embedding con L2 antes de almacenarlo o compararlo. Como consecuencia, el producto punto entre dos embeddings equivale a su similitud coseno.

El script de conversión fija automáticamente `TF_USE_LEGACY_KERAS=1`, porque el checkpoint utiliza el formato de Keras 2 y TensorFlow 2.16 utiliza Keras 3 por defecto.

## Flujo de reconocimiento

```text
Imagen
  │
  ├─ Corrección EXIF
  │
  ├─ ML Kit: detección del rostro más grande
  │
  ├─ Alineación según la posición de los ojos
  │
  ├─ Recorte y escalado a 112 × 112
  │
  ├─ EmbeddingExtractor: GhostFaceNet/TFLite
  │
  ├─ Normalización L2
  │
  ├─ FaceMatcher: ranking por similitud coseno
  │
  └─ Resultado según el umbral configurado
```

El repositorio genera hasta cinco candidatas ordenadas por similitud. Para cada persona se conserva la mejor similitud entre todas sus fotografías de enrolamiento.

## Umbral de reconocimiento

El umbral predeterminado es:

```text
0.30
```

Se puede modificar desde **Ajustes** en el rango `0.0` a `1.0`. Un valor más alto reduce la probabilidad de falsos positivos, pero puede rechazar coincidencias válidas. Un valor más bajo acepta más coincidencias, aunque aumenta el riesgo de identificar incorrectamente a una persona.

El valor de `0.30` es un punto de partida empírico. Debe calibrarse con fotografías, iluminación y dispositivos representativos del uso real.

## Base de datos local

La base Room se crea con el nombre:

```text
ghostfacenet.db
```



### Tablas principales



#### `perfiles`

Contiene la información principal de cada persona:

- `id`: identificador autogenerado.
- `nombre`: nombre mostrado en la interfaz.
- `estado`: actualmente se contemplan `Activo` y `Fallecido`.
- `foto_perfil`: fotografía principal codificada en Base64 o como data URI.
- `createdAt` y `updateAt`: marcas de tiempo en milisegundos.



#### `perfil_fotos`

Contiene fotografías adicionales sin crear perfiles duplicados:

- `id`: identificador de la fotografía.
- `perfil_id`: relación con `perfiles`.
- `foto_base64`: imagen codificada en Base64.
- `createdAt` y `updateAt`: marcas de tiempo.

La relación utiliza borrado en cascada cuando se elimina el perfil.

#### `face_embeddings`

Contiene los vectores calculados por el modelo:

- `perfil_id`: persona asociada.
- `embedding`: embedding de 512 valores serializado como `BLOB`.
- `perfil_update_at`: versión de la fotografía principal o del perfil usada para generarlo.
- `foto_id` y `foto_update_at`: referencia a una fotografía adicional, si corresponde.

Los embeddings se regeneran cuando faltan o cuando las marcas de tiempo indican que las imágenes cambiaron. Durante el reconocimiento solo se consultan embeddings vigentes de perfiles cuyo estado sea `Activo`.

### Carga de perfiles

La aplicación crea y actualiza internamente los embeddings, pero no expone operaciones de alta, edición o importación de perfiles en la UI actual. Por ello, la base local debe prepararse mediante el mecanismo de distribución de datos que utilice cada instalación, respetando el esquema definido en las entidades Room.

Las imágenes deben ser decodificables como JPEG/PNG después de eliminar, si existe, el prefijo `data:image/...;base64,`.

## Evaluación del modelo

Para evaluar el modelo convertido con el benchmark LFW:

```powershell
model_prep\.venv\Scripts\python.exe model_prep\evaluate_lfw.py
```

La evaluación descarga `lfw.bin` la primera vez, genera embeddings con flip TTA y busca el mejor umbral en el conjunto de pares. Las métricas publicadas del checkpoint original no sustituyen una validación del modelo convertido en el entorno y los dispositivos donde se vaya a utilizar.

## Datos de prueba

`test_data/prepare_test_set.py` organiza un dataset LFW ya extraído en tres grupos:

- `enroll/`: fotografías que se usarían para preparar perfiles.
- `probe_known/`: fotografías de personas que sí aparecen en el conjunto de enrolamiento.
- `probe_unknown/`: fotografías de personas que no aparecen en el conjunto de enrolamiento.

Ejecutar:

```powershell
python test_data\prepare_test_set.py
```

El script espera encontrar el dataset fuente en:

```text
test_data/lfw/
```

Los conjuntos generados están excluidos del control de versiones porque pueden ocupar mucho espacio.

## Estructura del proyecto

```text
.
├── app/
│   └── src/main/
│       ├── java/com/example/ghostfacenet/
│       │   ├── data/          # Repositorio, preferencias y codecs de imágenes
│       │   ├── data/db/       # Entidades, DAO y base Room
│       │   ├── ml/            # Detección, alineación, embeddings y matching
│       │   └── ui/            # Compose, navegación, pantallas y ViewModels
│       ├── res/               # Recursos Android
│       └── assets/            # Modelo ghostfacenet.tflite
├── model_prep/
│   ├── convert_to_tflite.py   # Descarga y conversión del checkpoint
│   ├── evaluate_lfw.py        # Evaluación contra LFW
│   └── requirements.txt       # Dependencias Python
├── test_data/
│   └── prepare_test_set.py    # Organización de datos de prueba
├── build.gradle.kts
├── settings.gradle.kts
└── gradlew.bat
```



## Dependencias principales

- Kotlin `1.9.24`.
- Java `17`.
- Jetpack Compose y Material 3.
- AndroidX Navigation Compose.
- AndroidX Room `2.6.1`.
- Kotlin Coroutines.
- Google ML Kit Face Detection `16.1.7`.
- TensorFlow Lite `2.16.1`.
- Coil para carga de imágenes en Compose.
- AndroidX ExifInterface.



## Privacidad y permisos

- La cámara se solicita únicamente al pulsar **Tomar foto**.
- Las fotografías de cámara se guardan temporalmente en la caché mediante `FileProvider` y el archivo temporal se elimina después de procesarlo.
- La inferencia facial de la aplicación Android se ejecuta localmente con ML Kit y TensorFlow Lite.
- La conversión del modelo y la evaluación LFW sí descargan archivos externos desde los scripts de preparación.
- El almacenamiento local de perfiles y embeddings debe protegerse de acuerdo con el entorno de despliegue y la sensibilidad de los datos.



## Limitaciones conocidas

- Solo se procesa el rostro de mayor tamaño de cada imagen.
- Los perfiles inactivos no participan en el matching.
- El reconocimiento requiere que exista al menos un embedding válido.
- Si no se encuentra un rostro, la aplicación informa `No se detectó ningún rostro en la imagen`.
- Si no existe el modelo en `app/src/main/assets/`, la inferencia no podrá inicializarse correctamente.
- La calidad depende de la iluminación, pose, resolución, oclusiones y calidad de las fotografías de referencia.
- El proyecto no incorpora actualmente una interfaz de enrolamiento o administración de perfiles.
- El umbral predeterminado debe validarse para cada conjunto de datos y caso de uso.



## Solución de problemas



### La aplicación no reconoce ninguna persona

Comprueba que:

1. `app/src/main/assets/ghostfacenet.tflite` existe.
2. La base local contiene perfiles con estado `Activo`.
3. Las fotografías están correctamente codificadas en Base64.
4. ML Kit detecta un rostro en la imagen.
5. Los embeddings se pudieron generar a partir de las fotografías.



### La conversión falla al cargar el checkpoint

Recrea el entorno virtual e instala las versiones fijadas:

```powershell
Remove-Item -Recurse -Force model_prep\.venv
python -m venv model_prep\.venv
model_prep\.venv\Scripts\python.exe -m pip install -r model_prep\requirements.txt
```



### Gradle no encuentra Java

Configura Android Studio o `JAVA_HOME` para utilizar un JDK 17 y vuelve a ejecutar:

```powershell
.\gradlew.bat assembleDebug
```



## Referencias

- [GhostFaceNets](https://github.com/HamadYA/GhostFaceNets)
- [Google ML Kit Face Detection](https://developers.google.com/ml-kit/vision/face-detection)
- [TensorFlow Lite](https://www.tensorflow.org/lite)
- [Jetpack Compose](https://developer.android.com/jetpack/compose)

