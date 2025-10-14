package com.example.ejercicioenclase2708

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlinx.coroutines.FlowPreview

@OptIn(FlowPreview::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onPhotoClick: (String) -> Unit,
    onProfileClick: () -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    var photos by remember { mutableStateOf<List<PexelsPhoto>>(emptyList()) }
    var isLoading by rememberSaveable { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var currentPage by rememberSaveable { mutableStateOf(1) }
    var canLoadMore by rememberSaveable { mutableStateOf(true) }

    val lazyGridState = rememberLazyGridState()

    // Lógica de debounce
    LaunchedEffect(query) {
        snapshotFlow { query }
            .debounce(500) // Espera 500ms
            .distinctUntilChanged() // Solo emite si el valor es nuevo
            .collect { searchQuery ->
                if (searchQuery.isNotBlank()) {
                    photos = emptyList() // Limpia la lista para nueva búsqueda
                    currentPage = 1
                    canLoadMore = true
                    fetchPhotos(searchQuery, currentPage, { newPhotos ->
                        photos = newPhotos
                        isLoading = false
                    }, { e ->
                        error = e
                        isLoading = false
                    })
                }
            }
    }

    // Lógica de Scroll infinito
    LaunchedEffect(lazyGridState) {
        snapshotFlow { lazyGridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .collect { lastIndex ->
                if (lastIndex != null && lastIndex >= photos.size - 5 && !isLoading && canLoadMore) {
                    val nextPage = currentPage + 1
                    fetchPhotos(query, nextPage, { newPhotos ->
                        if (newPhotos.isNotEmpty()) {
                            photos = photos + newPhotos // Añade nuevos resultados
                            currentPage = nextPage
                        } else {
                            canLoadMore = false // No hay más páginas
                        }
                        isLoading = false
                    }, { e ->
                        error = e
                        isLoading = false
                    })
                }
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Fotos") },
                actions = {
                    IconButton(onClick = onProfileClick) {
                        Icon(Icons.Default.AccountCircle, contentDescription = "Perfil")
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).padding(horizontal = 8.dp)) {
            TextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Buscar fotos...") },
                modifier = Modifier.fillMaxWidth().padding(8.dp)
            )

            if (isLoading && photos.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (error != null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Error: $error")
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 128.dp),
                    state = lazyGridState,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(items = photos, key = { it.id }) { photo ->
                        PhotoGridItem(photo = photo, onClick = { onPhotoClick(photo.id.toString()) })
                    }
                    if (isLoading && photos.isNotEmpty()) {
                        item {
                            Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                }
            }
        }
    }
}

// Función para obtener fotos
private fun fetchPhotos(
    query: String,
    page: Int,
    onSuccess: (List<PexelsPhoto>) -> Unit,
    onError: (String) -> Unit
) {
    val call = PexelsService.api.searchPhotos(query = query, page = page, perPage = 20)
    call.enqueue(object : Callback<PexelsResponse> {
        override fun onResponse(call: Call<PexelsResponse>, response: Response<PexelsResponse>) {
            if (response.isSuccessful) {
                onSuccess(response.body()?.photos.orEmpty())
            } else {
                onError("Error: ${response.code()}")
            }
        }

        override fun onFailure(call: Call<PexelsResponse>, t: Throwable) {
            onError(t.message ?: "Error de red")
        }
    })
}


@Composable
fun PhotoGridItem(photo: PexelsPhoto, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .aspectRatio(1f) // Item cuadrado
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = photo.src.medium,
            contentDescription = photo.alt,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}