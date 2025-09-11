package com.example.ejercicioenclase2708

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import androidx.compose.material.icons.automirrored.filled.ArrowBack

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailsScreen(photoId: String, onNavigateBack: () -> Unit) {
    var photo by remember { mutableStateOf<PexelsPhoto?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    // Llama a la API cuando el Composable entra en la composición
    LaunchedEffect(photoId) {
        PexelsService.api.getPhoto(photoId).enqueue(object : Callback<PexelsPhoto> {
            override fun onResponse(call: Call<PexelsPhoto>, response: Response<PexelsPhoto>) {
                isLoading = false
                if (response.isSuccessful) {
                    photo = response.body()
                } else {
                    error = "Error: ${response.code()}"
                }
            }

            override fun onFailure(call: Call<PexelsPhoto>, t: Throwable) {
                isLoading = false
                error = t.message ?: "Error de red"
            }
        })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(photo?.photographer ?: "Detalles") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            when {
                isLoading -> CircularProgressIndicator()
                error != null -> Text("Error: $error")
                photo != null -> PhotoDetails(photo!!)
            }
        }
    }
}

@Composable
fun PhotoDetails(photo: PexelsPhoto) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AsyncImage(
            model = photo.src.large2x ?: photo.src.original,
            contentDescription = photo.alt,
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentScale = ContentScale.Fit
        )
        Spacer(Modifier.height(16.dp))
        Text("Fotógrafo: ${photo.photographer}", style = MaterialTheme.typography.headlineSmall)
        Text("Descripción: ${photo.alt ?: "No disponible"}", style = MaterialTheme.typography.bodyLarge)
        Text("Tamaño: ${photo.width}x${photo.height}", style = MaterialTheme.typography.bodyMedium)
    }
}