# Laboratorio: Galería de Fotos con Room y Persistencia Offline

Esta es una aplicación de galería de fotos para Android construida con Jetpack Compose. Mediante la API de Pexels busca y muestra imágenes. La característica principal del
proyecto es integrar la base de datos "Room" para proporcionar una experiencia de usuario con capacidades offline, persistencia de favoritos e historial de búsquedas.

## Decisiones de Modelado de Datos (Room)

Se crearon dos entidades principales para estructurar la base de datos local:

1.  `PhotoEntity`: Almacena la información relevante de una foto.
    *   Clave Primaria: `id` de la foto, proviene de la API.
    *   Asociación a Búsqueda: Los campos `queryKey` (consulta en minúsculas) y `pageIndex` son para cachear los resultados de manera paginada por cada término de búsqueda.
    *   Persistencia de Estado: El campo `isFavorite: Boolean` permite guardar el estado de favorito de forma persistente, desacoplado de la API.
    *   Manejo de Caché: El campo `updatedAt` se añadió para futuras implementaciones de invalidación de caché por tiempo.

2.  `RecentSearchEntity`: Modela el historial de búsquedas.
    *   Clave Primaria: `query` (búsqueda), garantiza que no haya duplicados.
    *   Ordenamiento: El campo `lastUsedAt` es un timestamp que se actualiza cada vez que se realiza la búsqueda, permitiendo ordenar el historial por uso más reciente.

## Estrategia de Caché y Paginación

La aplicación sigue un patrón "Network-then-Persist" con la base de datos como Fuente Única de Verdad (Single Source of Truth).

1.  Observación: La UI (`HomeScreen`) observa un `Flow` desde el DAO de Room. Con `collectAsState`, la pantalla se recompone si hay cambios en la base de datos.

2.  Lógica de Red: Cuando el usuario busca o hace scroll, se dispara una petición a la API de Pexels.
    *   Si la red tiene éxito:
        1.  Los nuevos datos se reciben.
        2.  Se realiza una lógica para preservar el estado de `isFavorite`: se consulta a la base de datos para ver si alguna de las fotos recibidas ya estaba marcada como
        favorita y se fusiona ese estado.
        3.  Si es una búsqueda nueva, se limpia el caché *anterior* para esa `queryKey`.
        4.  Finalmente, los datos nuevos (ya fusionados) se insertan en Room con `OnConflictStrategy.REPLACE`. Esto dispara el `Flow` y la UI se actualiza de forma 
        reactiva.
      
    * Si la red falla:
        1.  El bloque `onFailure` se activa.
        2.  No se realiza ninguna acción. No se borra el caché ni se modifican los datos.
        3.  Como resultado, la UI simplemente sigue mostrando los datos que ya estaban en la base de datos, proveyendo una experiencia offline fluida.

3.  Paginación: Se implementó una estrategia manual (Opción B del laboratorio) usando una variable de estado `page` que se incrementa en cada petición de scroll infinito.

## Manejo de Estado (sin ViewModel)

El estado se gestionó siguiendo los principios de Compose:

*   Estado de la UI: `query`, `page`, `isLoading` se manejan con `rememberSaveable` para sobrevivir a cambios de configuración.
*   Estado de los Datos: `photos` y `recentSearches` ya no se guardan en un `remember`. Ahora son provistos por el `Flow` de Room y recolectados con `collectAsState`. Esto
           delega la persistencia del estado de los datos a la base de datos, que sobrevive a la muerte del proceso de forma nativa.
*   State Hoisting: El estado del tema (claro/oscuro) se eleva a `MainActivity` y se pasa hacia abajo a `ProfileScreen` y `AppNavigation`, mientras que el evento de cambio
           (`onThemeChange`) se pasa hacia arriba.

## Consideraciones y Trade-offs

*   Sin ViewModel/Repository: La lógica de datos se implementó directamente en el Composable (`HomeScreen`). Simplificando la arquitectura, pero en otra clase de 
proyectos, esta lógica debería ser extraída a un `ViewModel` y un `Repository`. Un `Repository` abstraería el origen de los datos (Room vs. Retrofit) y el `
'ViewModel` gestionaría el estado y la lógica de negocio, haciendo el código más limpio, escalable y fácil de probar.
*   Paginación Manual vs. Paging 3: La estrategia manual es más simple de implementar, pero `Paging 3` con `RemoteMediator` es más robusta en Android para manejar 
paginación compleja desde una base de datos y una fuente de red.