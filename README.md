# Alejandra Avilés - 24722
# Fecha de entrega: 10 de septiembre del 2025
# Laboratorio: Búsqueda de Fotos con Jetpack Compose

Este proyecto es una aplicación de Android nativa desarrollada en Kotlin con Jetpack Compose, como parte del Laboratorio para el Ciclo 2-2025 de la Universidad del Valle de Guatemala.

La aplicación permite a los usuarios buscar fotos utilizando la API de Pexels, mostrando los resultados en una grilla infinita y permitiendo ver los detalles de cada foto, así como navegar a una pantalla de perfil.

## Funcionalidades Implementadas

La aplicación cumple con todos los requisitos funcionales y técnicos solicitados:

### Pantalla Principal (Home)
-   **TopAppBar:** Muestra el título "Fotos" y un icono de perfil que navega a la pantalla de `Profile`.
-   **Barra de Búsqueda:** Un `TextField` permite al usuario escribir una consulta.
-   **Debounce:** La búsqueda se dispara automáticamente 500ms después de que el usuario deja de escribir para evitar llamadas excesivas a la API.
-   **Grilla de Fotos:** Los resultados se muestran en una `LazyVerticalGrid` adaptativa.
-   **Scroll Infinito:** A medida que el usuario se desplaza hacia el final de la grilla, la app carga automáticamente la siguiente página de resultados.
-   **Navegación a Detalles:** Al tocar una foto, se navega a la pantalla de `Details`, pasando el ID de la foto.

### Pantalla de Detalles (Details)
-   Muestra la imagen en un tamaño más grande.
-   Presenta metadatos como el nombre del fotógrafo, descripción y dimensiones de la imagen.
-   Incluye un **botón de "Compartir"** que utiliza un `Intent` de Android para enviar la URL de la foto a otras aplicaciones.

### Pantalla de Perfil (Profile)
-   Muestra datos estáticos (mock) de un usuario: avatar, nombre y correo electrónico.
-   Incluye un `Switch` que permite al usuario cambiar entre el tema claro y oscuro de la aplicación en tiempo real.

### Características Técnicas
-   **UI:** Construida con Jetpack Compose.
-   **Gestión de Estado:** Manejado directamente en los Composables usando `remember` y `rememberSaveable`, sin `ViewModel`, como lo requiere el laboratorio.
-   **Carga de Imágenes:** Se utiliza la librería **Coil 3** (`io.coil-kt.coil3`) para cargar las imágenes desde la red de forma asíncrona.
-   **Networking:** Las llamadas a la API de Pexels se realizan con **Retrofit** y **OkHttp**.
-   **Diseño:** Se adhiere a los principios de **Material 3** y utiliza `MaterialTheme.colorScheme` para soportar correctamente los temas claro y oscuro sin colores hardcodeados.
-   **Navegación:** Se utiliza **Navigation Compose** para gestionar el flujo entre las tres pantallas.

## Configuración del Proyecto

Para poder ejecutar este proyecto, es necesario obtener una clave de API gratuita de Pexels.

1.  Ve a [https://www.pexels.com/api/](https://www.pexels.com/api/).
2.  Regístrate o inicia sesión.
3.  Solicita una clave de API.
4.  Crea un archivo llamado `local.properties` en la raíz del proyecto.
5.  Añade tu clave de API a ese archivo con el siguiente formato (sin comillas):

    ```properties
    PEXELS_API_KEY=AQUI_VA_TU_CLAVE_DE_API_DE_PEXELS
    ```
6.  Reconstruye el proyecto en Android Studio para que Gradle genere el `BuildConfig` con la clave.

## Preguntas de Reflexión

A continuación se presentan las respuestas a las preguntas de reflexión del laboratorio.

#### 1. ¿Qué ventajas y limitaciones viste al no usar ViewModel?

*   **Ventajas:**
    *   **Simplicidad Inicial:** Para pantallas simples, manejar el estado con `remember` y `rememberSaveable` es más rápido y directo, evitando la creación de una clase extra y reduciendo el *boilerplate*.
    *   **Co-localización:** El estado vive junto a la UI que lo utiliza, lo que puede hacer que el código sea más fácil de seguir en componentes pequeños y aislados.

*   **Limitaciones:**
    *   **Pérdida de Estado:** El estado en `remember` se pierde con los cambios de configuración (como la rotación). Aunque `rememberSaveable` mitiga esto para datos simples, no es una solución robusta para estados complejos o llamadas de red en curso.
    *   **Acoplamiento:** La lógica de negocio (llamadas a la API, debounce) queda fuertemente acoplada a la UI. Esto dificulta la reutilización de la lógica y hace que las pruebas unitarias sean casi imposibles.
    *   **Complejidad en Composables:** Las funciones `@Composable` se vuelven muy grandes y difíciles de mantener al mezclar la definición de la UI con la gestión de estado y los efectos secundarios (`LaunchedEffect`).

#### 2. ¿Qué pasaría si la app rota o se suspende en background?

*   **Al Rotar (Cambio de Configuración):**
    *   El estado guardado en `remember { mutableStateOf(...) }`, como la lista de fotos (`photos`), se reiniciaría a su valor inicial. La lista se vaciaría y el usuario perdería su scroll.
    *   El estado guardado en `rememberSaveable { ... }`, como la consulta de búsqueda (`query`) y la página actual (`currentPage`), se restauraría correctamente.
    *   Una llamada de red en curso iniciada desde un `LaunchedEffect` se cancelaría y se relanzaría, provocando una nueva carga de datos.

*   **Al Suspenderse (App en Background):**
    *   Si el sistema operativo destruye el proceso por falta de memoria, al volver, la app se reiniciaría. El estado de `rememberSaveable` ayudaría a restaurar la última búsqueda, pero la lista de fotos se habría perdido, forzando una recarga desde la primera página.

#### 3. ¿Cuándo sí convendría usar ViewModel en proyectos grandes?

Conviene usar `ViewModel` en **casi todos los proyectos de Android que no sean triviales**, y es una práctica estándar por las siguientes razones:

*   **Supervivencia a Cambios de Configuración:** `ViewModel` está diseñado para sobrevivir a la recreación de la `Activity`, manteniendo el estado de la UI intacto sin esfuerzo y evitando recargas de datos innecesarias.
*   **Separación de Responsabilidades:** Desacopla la lógica de negocio de la UI. Los Composables se dedican a mostrar el estado que el `ViewModel` les provee, haciendo el código más limpio, modular y fácil de testear.
*   **Ciclo de Vida Consciente:** `ViewModel` proporciona un `viewModelScope` para lanzar corutinas que se cancelan automáticamente cuando el `ViewModel` ya no es necesario, previniendo fugas de memoria y tareas en segundo plano "zombis".
*   **Compartir Estado:** Un `ViewModel` puede ser compartido fácilmente entre varios Composables o incluso entre diferentes pantallas (Fragments/Destinos de Nav) para mantener un estado consistente.

Para cualquier proyecto que se espere mantener, escalar o probar, el uso de `ViewModel` es la arquitectura recomendada y la más robusta.