package com.example.ejercicioenclase2708.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.ejercicioenclase2708.data.local.AppDatabase
import com.example.ejercicioenclase2708.data.local.toEntity
import com.example.ejercicioenclase2708.data.remote.PexelsPhoto
import com.example.ejercicioenclase2708.data.remote.PexelsService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailsScreen(navController: NavController, photoId: Long) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val db = remember { AppDatabase.getDatabase(context) }
    val photoDao = remember { db.photoDao() }
    val photo by photoDao.getPhotoById(photoId).collectAsState(initial = null)

    LaunchedEffect(photoId) {
        if (photo == null) {
            PexelsService.api.getPhotoById(photoId).enqueue(object : Callback<PexelsPhoto> {
                override fun onResponse(call: Call<PexelsPhoto>, response: Response<PexelsPhoto>) {
                    if (response.isSuccessful) {
                        response.body()?.let { photoFromNet ->
                            coroutineScope.launch(Dispatchers.IO) {
                                photoDao.insertPhotos(listOf(photoFromNet.toEntity("detail", 1)))
                            }
                        }
                    }
                }

                override fun onFailure(call: Call<PexelsPhoto>, t: Throwable) {
                    // Manejar error
                }
            })
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(photo?.alt ?: "Detalles") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver") // <-- EL USO CORRECTO
                    }
                },
                actions = {
                    photo?.let { p ->
                        IconButton(onClick = {
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, "¡Mira esta foto de Pexels! ${p.url}")
                                type = "text/plain"
                            }
                            val shareIntent = Intent.createChooser(sendIntent, null)
                            context.startActivity(shareIntent)
                        }) {
                            Icon(Icons.Default.Share, contentDescription = "Compartir")
                        }
                    }
                }
            )
        }
    ) { padding ->
        photo?.let { p ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(16.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                AsyncImage(
                    model = p.url,
                    contentDescription = p.alt,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(p.width.toFloat() / p.height.toFloat())
                        .clip(MaterialTheme.shapes.medium),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.height(16.dp))
                Text(p.alt, style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(8.dp))
                Text("Fotógrafo: ${p.photographer}", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = {
                        coroutineScope.launch(Dispatchers.IO) {
                            photoDao.setFavorite(p.id, !p.isFavorite)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = if (p.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorito"
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(if (p.isFavorite) "Quitar de favoritos" else "Marcar como favorito")
                }
            }
        } ?: Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
            Text("Cargando foto...")
        }
    }
}