package com.example.ejercicioenclase2708.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.ejercicioenclase2708.data.local.AppDatabase
import com.example.ejercicioenclase2708.data.local.RecentSearchEntity
import com.example.ejercicioenclase2708.data.local.toEntity
import com.example.ejercicioenclase2708.data.remote.PexelsResponse
import com.example.ejercicioenclase2708.data.remote.PexelsService
import com.example.ejercicioenclase2708.ui.components.PhotoCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController) {
    // Estados
    var query by rememberSaveable { mutableStateOf("Nature") }
    var page by rememberSaveable { mutableStateOf(1) }
    var isLoading by rememberSaveable { mutableStateOf(false) }
    var canLoadMore by rememberSaveable { mutableStateOf(true) }
    var showFavoritesOnly by rememberSaveable { mutableStateOf(false) }

    // Lógica de datos
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val db = remember { AppDatabase.getDatabase(context) }
    val photoDao = remember { db.photoDao() }
    val recentSearchDao = remember { db.recentSearchDao() }

    val photos by remember(showFavoritesOnly, query) {
        if (showFavoritesOnly) {
            photoDao.getFavoritePhotos()
        } else {
            photoDao.getPhotosByQuery(query.lowercase().trim())
        }
    }.collectAsState(initial = emptyList())
    val recentSearches by recentSearchDao.getRecentSearches().collectAsState(initial = emptyList())

    val lazyGridState = rememberLazyGridState()

    fun fetchAndPersistPhotos(searchQuery: String, isNewSearch: Boolean) {
        if (isLoading) return
        isLoading = true
        val pageToFetch = if (isNewSearch) 1 else page + 1
        val normalizedQuery = searchQuery.lowercase().trim()

        coroutineScope.launch {
            if (normalizedQuery.isNotBlank()) {
                withContext(Dispatchers.IO) {
                    recentSearchDao.insertSearch(RecentSearchEntity(query = searchQuery))
                }
            }
        }

        val call = PexelsService.api.searchPhotos(query = searchQuery, page = pageToFetch, perPage = 20)
        call.enqueue(object : Callback<PexelsResponse> {
            override fun onResponse(call: Call<PexelsResponse>, response: Response<PexelsResponse>) {
                isLoading = false
                if (response.isSuccessful) {
                    val newPhotosFromNetwork = response.body()?.photos.orEmpty()
                    canLoadMore = response.body()?.nextPage != null
                    page = pageToFetch

                    coroutineScope.launch(Dispatchers.IO) {
                        val networkIds = newPhotosFromNetwork.map { it.id }.toSet()
                        val existingFavorites = photoDao.getFavoritePhotosByIds(networkIds)
                        val favoriteIds = existingFavorites.map { it.id }.toSet()

                        val entities = newPhotosFromNetwork.map { photo ->
                            val entity = photo.toEntity(normalizedQuery, pageToFetch)
                            if (favoriteIds.contains(entity.id)) {
                                entity.isFavorite = true
                            }
                            entity
                        }

                        if (isNewSearch) {
                            photoDao.clearPhotosByQuery(normalizedQuery)
                        }

                        photoDao.insertPhotos(entities)
                    }
                }
            }

            override fun onFailure(call: Call<PexelsResponse>, t: Throwable) {
                isLoading = false
            }
        })
    }

    LaunchedEffect(query) {
        snapshotFlow { query }
            .debounce(500)
            .distinctUntilChanged()
            .collect { searchQuery ->
                if (!showFavoritesOnly) { // Busca en la red si no estamos en modo favoritos
                    fetchAndPersistPhotos(searchQuery, isNewSearch = true)
                }
            }
    }

    LaunchedEffect(lazyGridState) {
        snapshotFlow { lazyGridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .collect { lastIndex ->
                if (lastIndex != null && lastIndex >= photos.size - 5 && !isLoading && canLoadMore && !showFavoritesOnly) {
                    fetchAndPersistPhotos(query, isNewSearch = false)
                }
            }
    }

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
        Column(Modifier.padding(padding).fillMaxSize()) {
            TextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Buscar fotos...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                enabled = !showFavoritesOnly // Desactivar la búsqueda en modo favoritos
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LazyRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(items = recentSearches, key = { it.query }) { search ->
                        AssistChip(
                            onClick = { query = search.query },
                            label = { Text(search.query) },
                            enabled = !showFavoritesOnly
                        )
                    }
                }

                FilterChip(
                    selected = showFavoritesOnly,
                    onClick = { showFavoritesOnly = !showFavoritesOnly },
                    label = { Text("Favoritos") },
                    leadingIcon = {
                        Icon(
                            imageVector = if (showFavoritesOnly) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favoritos"
                        )
                    }
                )
            }

            when {
                isLoading && photos.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                !isLoading && photos.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            if(showFavoritesOnly) "No tienes fotos favoritas."
                            else "No se encontraron fotos para \"$query\"."
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
                                modifier = Modifier.clickable {
                                    navController.navigate("details/${photo.id}")
                                }
                            )
                        }

                        if (isLoading && photos.isNotEmpty()) {
                            item {
                                Box(
                                    Modifier
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