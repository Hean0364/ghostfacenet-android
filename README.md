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
- El modelo `ghostfacenet.tflite`, ya incluido en el repositorio, ubicado en:

```text
app/src/main/assets/ghostfacenet.tflite
```

El modelo se empaqueta automáticamente dentro del APK al compilar la aplicación.
No es necesario descargarlo ni generarlo para ejecutar la demo.

### Conversión del modelo (opcional)

Solo se necesitan estas herramientas si se desea regenerar o actualizar el modelo:

- Python 3.
- Conexión a Internet durante la descarga del checkpoint.
- Dependencias definidas en `model_prep/requirements.txt`.



## Inicio rápido



### 1. Abrir y compilar

El modelo ya se encuentra en `app/src/main/assets/ghostfacenet.tflite`.
Desde la raíz del proyecto, en PowerShell:

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat installDebug
```

El APK de depuración se genera en:

```powershell
app/build/outputs/apk/debug/app-debug.apk
```

También se puede abrir el proyecto desde Android Studio y ejecutar la configuración
`app`. No se requiere Python ni conexión a Internet para compilar y ejecutar la
aplicación con el modelo incluido.

### 2. Regenerar el modelo (opcional)

Si se necesita convertir nuevamente el checkpoint:

```powershell
python -m venv model_prep\.venv
model_prep\.venv\Scripts\python.exe -m pip install -r model_prep\requirements.txt
model_prep\.venv\Scripts\python.exe model_prep\convert_to_tflite.py
Copy-Item model_prep\output\ghostfacenet.tflite app\src\main\assets\ghostfacenet.tflite -Force
```

El script descarga el checkpoint si hace falta, lo convierte a TensorFlow Lite
con cuantización `float16` y ejecuta una inferencia de comprobación. Después,
el archivo generado reemplaza el asset incluido.

## Preparar el modelo

El modelo utilizado es `GhostFaceNetV1-1.3-2 (ArcFace, MS1MV3)` del proyecto [GhostFaceNets](https://github.com/HamadYA/GhostFaceNets).
El archivo TFLite correspondiente ya está incluido en el repositorio. Esta
sección describe sus características y el procedimiento opcional para
regenerarlo.

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

Los perfiles deben cargarse manualmente en la base local antes de utilizar el
reconocimiento. La interfaz actual no tiene operaciones de alta, edición o
importación de perfiles.

Para cada persona, inserta un registro en `perfiles` y coloca la fotografía
principal convertida a Base64 en `perfiles.foto_perfil`. Como mínimo, el registro
debe incluir:

- `nombre`.
- `estado`, normalmente `Activo`.
- `foto_perfil` con una imagen JPEG o PNG codificada en Base64.
- `createdAt` y `updateAt` como marcas de tiempo en milisegundos.

Las fotos adicionales deben insertarse manualmente en `perfil_fotos`, usando el
`perfil_id` correspondiente y la imagen Base64 en `foto_base64`. No es necesario
insertar manualmente la tabla `face_embeddings`: al iniciar la aplicación,
`FaceRepository` detecta los perfiles y genera los embeddings a partir de las
imágenes almacenadas.

La carga puede realizarse mediante el mecanismo de distribución de SQLite que
utilice cada instalación, por ejemplo una base precargada o un proceso externo
de importación. Debe respetarse el esquema de Room y actualizar `updateAt` cada
vez que se sustituya una fotografía, para que el embedding se regenere.

Las imágenes deben ser decodificables como JPEG/PNG después de eliminar, si existe, el prefijo `data:image/...;base64,`.

## Evaluación del modelo

Para evaluar el modelo convertido con el benchmark LFW:

```powershell
model_prep\.venv\Scripts\python.exe model_prep\evaluate_lfw.py
```

La evaluación descarga `lfw.bin` la primera vez, genera embeddings con flip TTA y busca el mejor umbral en el conjunto de pares. Las métricas publicadas del checkpoint original no sustituyen una validación del modelo convertido en el entorno y los dispositivos donde se vaya a utilizar.

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

