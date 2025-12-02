# AplicacionRocola Projecto Final

AplicacionRocola es una app Android pensada para restaurantes que desean ofrecer una "rocola" digital y un menú administrable desde Firebase. La aplicación muestra banners personalizables, recomendaciones musicales en vivo (YouTube Data API), un buscador con filtros por género y una playlist compartida respaldada en Firestore.

## Contenido
- [Características principales](#caracter%C3%ADsticas-principales)
- [Instalación y ejecución](#instalaci%C3%B3n-y-ejecuci%C3%B3n)
- [Arquitectura y organización](#arquitectura-y-organizaci%C3%B3n)
- [Configuración del proyecto](#configuraci%C3%B3n-del-proyecto)
  - [AndroidManifest.xml](#androidmanifestxml)
  - [Gradle de proyecto y módulo](#gradle-de-proyecto-y-m%C3%B3dulo)
  - [Versionado y dependencias](#versionado-y-dependencias)
- [Assets y recursos](#assets-y-recursos)
- [Layouts responsivos](#layouts-responsivos)
- [Build variants](#build-variants)
- [Simulación de error de configuración](#simulaci%C3%B3n-de-error-de-configuraci%C3%B3n)
- [Reflexión: riesgos de un mal manejo](#reflexi%C3%B3n-riesgos-de-un-mal-manejo)
- [Pruebas automatizadas](#pruebas-automatizadas)

## Características principales
- **Inicio dinámico** (`ui/home/FragmentInicio.kt`): banners administrados desde Firestore y carrusel horizontal de recomendaciones musicales provenientes de la API de YouTube.
- **Buscador avanzado** (`ui/search/BuscarFragment.kt` + MVVM): filtros por género, paginación y prioridades para canales oficiales como VEVO.
- **Playlist compartida** (`PlaylistManager`, `ui/playlist/PlaylistFragment.kt`): se sincroniza en tiempo real con Firestore y permite añadir canciones desde cualquier fragmento.
- **Administración del menú** (`ui/admin/AdminActivity.kt`, `ManageDishesActivity.kt`): subida de imágenes a Firebase Storage, formulario con validaciones y listado editable con `RecyclerView`.

## Instalación y ejecución
1. **Requisitos**
   - Android Studio Flamingo o superior.
   - JDK 17 o 21 (Gradle actual no soporta JDK 25).
   - Cuenta Firebase y archivo `google-services.json` (ya presente en `app/`).
2. **Clonar y abrir el proyecto** con Android Studio.
3. **Sincronizar Gradle** y, si es necesario, actualizar la clave de YouTube (`res/values/strings.xml`, `youtube_api_key`).
4. **Compilar**:
   ```powershell
   cd C:\Users\anton\AndroidStudioProjects\AplicacionRocola_ProjectoFinal
   ./gradlew :app:assembleDebug
   ```
5. **Instalar en dispositivo/emulador** desde Android Studio o con `adb install` usando el APK generado.

## Arquitectura y organización
- **Capa UI** (`app/src/main/java/.../ui`): dividida por feature (`home`, `menu`, `playlist`, `search`, `admin`).
- **Capas de datos** (`data/model`, `data/remote`, `data/repository`): modelos compartidos (`SongItem`), cliente YouTube (`YoutubeRemoteDataSource`) y repositorios como `SearchRepository`.
- **Utilidades**: `PlaylistManager` maneja la playlist centralizada; `FirebaseUtils` garantiza inicialización segura de Firebase.

## Configuración del proyecto
### AndroidManifest.xml
Ubicación: `app/src/main/AndroidManifest.xml`.
- Declara permisos de **INTERNET** y (legacy) `READ_EXTERNAL_STORAGE` para administrar imágenes (se recomienda migrar a `READ_MEDIA_IMAGES` en Android 13+).
- Registra actividades clave (`MainActivity`, `ui.admin.AdminActivity`, `ManageDishesActivity`).
- Configura íconos (`@mipmap/ic_launcher`) y reglas de respaldo (`data_extraction_rules`, `backup_rules`).

### Gradle de proyecto y módulo
- `build.gradle.kts` (raíz): aplica los plugins Android y Kotlin de forma centralizada.
- `app/build.gradle.kts` define:
  - `compileSdk = 36`, `minSdk = 27`, `targetSdk = 36`.
  - `defaultConfig` con `applicationId`, `testInstrumentationRunner` y versiones.
  - `buildTypes` con `release` preparado para ProGuard.
  - Dependencias: BOM de Firebase + versiones explícitas para Firestore/Storage, Coil, Coroutines, OkHttp.

### Versionado y dependencias
- `versionCode = 1`, `versionName = "1.0"` en `defaultConfig`.
- Dependencias agrupadas por funcionalidad: UI (`androidx.*`), imágenes (`coil`), backend (`firebase-*`), asincronía (`kotlinx.coroutines`), red (`okhttp`).

## Assets y recursos
- **Drawable** (`res/drawable/`): placeholders vectoriales (`bg_image_placeholder.xml`, `ic_placeholder_menu.xml`) y gradientes.
- **Raster & mipmap**: íconos adaptados a `mdpi`, `hdpi`, `xhdpi`, `xxhdpi`, `xxxhdpi` dentro de `mipmap-*`.
- **Values** (`res/values/`): `colors.xml`, `themes.xml`, `strings.xml` (incluye `youtube_api_key`), `ids.xml`.
- **Otros recursos**: carpetas `raw`, `styles`, `dimens` se pueden incorporar para audio, estilos y tamaños escalables. La app ya maneja `xml/` para reglas de backup.

## Layouts responsivos
- `fragment_inicio.xml` usa `NestedScrollView` para adaptarse verticalmente y `RecyclerView` horizontal para recomendados.
- `activity_main.xml` define `BottomNavigationView` y `FragmentContainerView`, permitiendo navegación responsiva.
- Se pueden añadir variantes `layout-land/` para optimizar landscape; por ahora los layouts funcionan en ambas orientaciones.
- El uso de `include` es viable para banners y headers repetidos (pendiente para futuros refactors).

## Build variants
- Gradle genera `debug` y `release`. `release` desactiva `minifyEnabled` pero incluye `proguard-rules.pro` para endurecer la build cuando se habilite.
- Para múltiples entornos (QA, producción) se pueden añadir `productFlavors` y firmados específicos.

## Simulación de error de configuración
- Si se crea una carpeta mal nombrada (por ejemplo `res/drawalbe/`), los recursos dentro se ignorarán y la compilación fallará o se generarán excepciones `Resources$NotFoundException`. Mantén la nomenclatura oficial (`drawable`, `layout`, `values-xx`, etc.) para evitarlo.

## Reflexión: riesgos del mal manejo de assets/dependencias
- **Assets incorrectos** → UI pixelada o crashes por recursos inexistentes.
- **Dependencias desactualizadas** → conflictos binarios, errores de compilación o vulnerabilidades de seguridad.
- **Claves/API** mal configuradas → características críticas (búsqueda YouTube, banners Firebase) dejan de funcionar.

## Pruebas automatizadas
- Instrumented tests (`app/src/androidTest/...`):
  - `MainActivityNavigationTest`: verifica navegación a Inicio, Menú, Buscar y Playlist.
  - `AdminFlowTest`: asegura que la vista Admin muestre el botón de gestión y permita acceder a `ManageDishesActivity`.
  - `ManageDishesActivityTest`: valida estado inicial y mensajes de validación del formulario.
- Requiere dispositivo/emulador y JDK soportado. Ejecuta:
  ```powershell
  cd C:\Users\anton\AndroidStudioProjects\AplicacionRocola_ProjectoFinal
  ./gradlew :app:connectedAndroidTest
  ```
  > Nota: el build falla con JDK 25; ajusta `JAVA_HOME` a 17/21 antes de correr las pruebas.

---
Para contribuciones o dudas, revisa la estructura en `app/src/main/java` y los layouts en `app/src/main/res`. Cualquier mejora (por ejemplo, variantes landscape, lint baseline o nuevas pruebas) es bienvenida.
