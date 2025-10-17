package com.example.ejercicioenclase2708.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.ejercicioenclase2708.data.local.AppDatabase
import com.example.ejercicioenclase2708.data.local.PhotoEntity
import com.example.ejercicioenclase2708.data.local.RecentSearchEntity
import com.example.ejercicioenclase2708.data.local.toEntity
import com.example.ejercicioenclase2708.data.remote.PexelsResponse
import com.example.ejercicioenclase2708.data.remote.PexelsService
import com.example.ejercicioenclase2708.ui.components.PhotoCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController, authorToFilter: String?) {
    // Estado

    // authorToFilter hace que el estado se recalcule por si cambiara de alguna forma
    var filterState by rememberSaveable(authorToFilter, stateSaver = filterStateSaver) {
        mutableStateOf(
            authorToFilter?.let { FilterState.ByAuthor(it) } ?: FilterState.None
        )
    }
    // Inicialización de query dependiendo del estado del filtro
    var query by rememberSaveable(authorToFilter) {
        mutableStateOf(
            if (authorToFilter != null) "" else "Nature"
        )
    }

    var page by rememberSaveable { mutableStateOf(1) }
    var isLoading by rememberSaveable { mutableStateOf(false) }
    var canLoadMore by rememberSaveable { mutableStateOf(true) }

    // Lógica de datos
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val db = remember { AppDatabase.getDatabase(context) }
    val photoDao = remember { db.photoDao() }
    val recentSearchDao = remember { db.recentSearchDao() }

    // Fuente de datos reactiva
    // `remember` recalcula el Flow cuando las claves (`filterState`, `query`) cambian.
    val photosFlow: Flow<List<PhotoEntity>> = remember(filterState, query) {
        when (val currentFilter = filterState) {
            is FilterState.ByAuthor -> photoDao.getPhotosByAuthor(currentFilter.authorName)
            FilterState.FavoritesOnly -> photoDao.getFavoritePhotos()
            FilterState.None -> photoDao.getPhotosByQuery(query.lowercase().trim())
        }
    }
    val photos by photosFlow.collectAsState(initial = emptyList())

    val recentSearches by recentSearchDao.getRecentSearches().collectAsState(initial = emptyList())
    val lazyGridState = rememberLazyGridState()

    // Manejo de efectos

    // Aplicación del filtro de autor cuando se recibe como argumento PRUEBA 1
    /*LaunchedEffect(authorToFilter) {
        authorToFilter?.let { authorName ->
            if (authorName.isNotEmpty() && (filterState as? FilterState.ByAuthor)?.authorName != authorName) {
                // Actualización del estado del filtro Y limpiamos la query
                filterState = FilterState.ByAuthor(authorName)
                query = "" // Limpieza de query
            }
        }
    }*/

    fun fetchAndPersistPhotos(searchQuery: String, isNewSearch: Boolean, author: String? = null) {
        if (isLoading) return
        isLoading = true
        val pageToFetch = if (isNewSearch) 1 else page + 1
        val effectiveQuery = author ?: searchQuery // Autor como query sí está, de lo contrario, usa searchQuery
        val normalizedQuery = effectiveQuery.lowercase().trim()

        coroutineScope.launch {
            if (normalizedQuery.isNotBlank() && author == null) { // Guarda búsquedas por texto (no autor)
                withContext(Dispatchers.IO) { recentSearchDao.insertSearch(RecentSearchEntity(query = searchQuery)) }
            }
        }

        val call = PexelsService.api.searchPhotos(query = effectiveQuery, page = pageToFetch, perPage = 20)
        call.enqueue(object : Callback<PexelsResponse> {
            override fun onResponse(call: Call<PexelsResponse>, response: Response<PexelsResponse>) {
                if (!response.isSuccessful) {
                    isLoading = false; return
                }
                val newPhotosFromNetwork = response.body()?.photos.orEmpty()
                canLoadMore = response.body()?.nextPage != null
                page = pageToFetch

                coroutineScope.launch(Dispatchers.IO) {
                    val networkIds = newPhotosFromNetwork.map { it.id }.toSet()
                    val existingFavorites = photoDao.getFavoritePhotosByIds(networkIds)
                    val favoriteIds = existingFavorites.map { it.id }.toSet()
                    val entities = newPhotosFromNetwork.map { photo ->
                        val entity = photo.toEntity(normalizedQuery, pageToFetch)
                        if (favoriteIds.contains(entity.id)) { entity.isFavorite = true }
                        entity
                    }
                    if (isNewSearch) {
                        photoDao.clearPhotosByQuery(normalizedQuery)
                    }
                    photoDao.insertPhotos(entities)
                    withContext(Dispatchers.Main) { isLoading = false }
                }
            }
            override fun onFailure(call: Call<PexelsResponse>, t: Throwable) { isLoading = false }
        })
    }

    // Efecto para cambios en el estado del filtro cambia a 'ByAuthor'
    LaunchedEffect(filterState) {
        // Captura de estado actual en una variable local inmutable
        val currentFilter = filterState

        // Comprobación y operación sobre la variable local
        if (currentFilter is FilterState.ByAuthor) {
            coroutineScope.launch {
                // Limpieza de la base de datos en el hilo de IO
                withContext(Dispatchers.IO) {
                    photoDao.clearPhotosByQuery(currentFilter.authorName.lowercase().trim())
                }
                // Inicialización de la nueva búsqueda
                fetchAndPersistPhotos(
                    searchQuery = "", // No se necesita query de texto
                    isNewSearch = true,
                    author = currentFilter.authorName // Usamos el nombre del autor para la búsqueda (NO SE LOGRÓ IMPLEMENTAR CORRECTAMENTE)
                )
            }
        }
    }

    // Efecto para búsquedas con debounce (solo se activa si no hay filtro)
    LaunchedEffect(query, filterState) {
        if (filterState == FilterState.None) {
            snapshotFlow { query }
                .debounce(500)
                .distinctUntilChanged()
                .collect { currentQuery ->
                    if (currentQuery.isNotEmpty()) {
                        fetchAndPersistPhotos(currentQuery, isNewSearch = true)
                    }
                }
        }
    }

    // Efecto para scroll infinito (solo se activa si no hay filtro)
    LaunchedEffect(lazyGridState, photos.size, filterState) {
        snapshotFlow { lazyGridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .collect { lastIndex ->
                val isAuthorFilter = filterState is FilterState.ByAuthor
                val isNoFilter = filterState == FilterState.None

                // Permite scroll infinito en búsqueda normal como en filtro por autor
                if ((isNoFilter || isAuthorFilter) && lastIndex != null && lastIndex >= photos.size - 5 && !isLoading && canLoadMore) {
                    val queryToUse = if (isNoFilter) query else ""
                    val authorToUse = if (isAuthorFilter) (filterState as FilterState.ByAuthor).authorName else null

                    fetchAndPersistPhotos(
                        searchQuery = queryToUse,
                        isNewSearch = false, // Para cargar más
                        author = authorToUse
                    )
                }
            }
    }

    // UI
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Fotos") },
                actions = {
                    IconButton(onClick = { navController.navigate("profile") }) {
                        Icon(Icons.Default.AccountCircle, contentDescription = "Perfil")
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier
            .padding(padding)
            .fillMaxSize()) {
            TextField(
                value = if (filterState is FilterState.ByAuthor) "" else query,
                onValueChange = { query = it },
                label = { Text("Buscar fotos...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                enabled = filterState == FilterState.None,
                readOnly = filterState != FilterState.None
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (filterState == FilterState.None) {
                    LazyRow(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(items = recentSearches, key = { it.query }) { search ->
                            AssistChip(onClick = { query = search.query }, label = { Text(search.query) })
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }

                FilterChip(
                    selected = filterState == FilterState.FavoritesOnly,
                    onClick = {
                        filterState = if (filterState == FilterState.FavoritesOnly) FilterState.None else FilterState.FavoritesOnly
                    },
                    label = { Text("Favoritos") },
                    leadingIcon = {
                        Icon(
                            imageVector = if (filterState == FilterState.FavoritesOnly) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favoritos"
                        )
                    }
                )
            }

            if (filterState is FilterState.ByAuthor) {
                InputChip(
                    selected = false,
                    onClick = { filterState = FilterState.None },
                    label = { Text("Autor: ${(filterState as FilterState.ByAuthor).authorName}") },
                    trailingIcon = { Icon(Icons.Default.Close, contentDescription = "Quitar filtro") },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            when {
                isLoading && photos.isEmpty() -> {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 128.dp),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(10) {
                            Box(modifier = Modifier
                                .aspectRatio(1f)
                                .clip(MaterialTheme.shapes.medium)
                                .background(MaterialTheme.colorScheme.surfaceVariant))
                        }
                    }
                }
                photos.isEmpty() && !isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            when(val state = filterState) {
                                is FilterState.ByAuthor -> "No hay fotos de '${state.authorName}' en el caché."
                                FilterState.FavoritesOnly -> "No tienes fotos favoritas."
                                FilterState.None -> "No se encontraron fotos para \"$query\"."
                            }
                        )
                    }
                }
                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 128.dp),
                        state = lazyGridState,
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(items = photos, key = { it.id }) { photo ->
                            PhotoCard(
                                title = photo.alt,
                                url = photo.thumbnailUrl,
                                isFavorite = photo.isFavorite,
                                onFavoriteClick = {
                                    coroutineScope.launch(Dispatchers.IO) {
                                        photoDao.setFavorite(photo.id, !photo.isFavorite)
                                    }
                                },
                                modifier = Modifier.clickable { navController.navigate("details/${photo.id}") }
                            )
                        }
                        if (isLoading && photos.isNotEmpty()) {
                            item {
                                Box(Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}