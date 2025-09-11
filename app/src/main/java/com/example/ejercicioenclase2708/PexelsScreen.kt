package com.example.ejercicioenclase2708

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zezziapp.testapp.ui.features.photos.models.PexelsPhoto
import com.zezziapp.testapp.ui.features.photos.models.PexelsResponse
import com.zezziapp.testapp.ui.features.photos.models.PexelsService
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PexelsScreen(
    initialPage: Int = 1,
    perPage: Int = 10
) {
    var loading by rememberSaveable { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var page by rememberSaveable { mutableStateOf(initialPage) }

    var photos by remember { mutableStateOf<List<PexelsPhoto>>(emptyList()) }
    var currentCall by remember { mutableStateOf<Call<PexelsResponse>?>(null) }

    // Cancel any in-flight request if the composable leaves composition
    DisposableEffect(Unit) {
        onDispose { currentCall?.cancel() }
    }

    fun fetch(newPage: Int = page) {
        currentCall?.cancel()

        loading = true
        error = null

        val call = PexelsService.api.getCurated(page = newPage, perPage = perPage)
        currentCall = call

        call.enqueue(object : Callback<PexelsResponse> {
            override fun onResponse(
                call: Call<PexelsResponse>,
                response: Response<PexelsResponse>
            ) {
                loading = false
                if (response.isSuccessful) {
                    val list = response.body()?.photos.orEmpty()
                    photos = list
                    page = newPage
                } else {
                    error = "HTTP ${response.code()}"
                }
            }

            override fun onFailure(call: Call<PexelsResponse>, t: Throwable) {
                if (call.isCanceled) return
                loading = false
                error = t.message ?: "Network error"
            }
        })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Pexels + Retrofit + Coil")
                },
                actions = {
                    IconButton(onClick = {
                        if (!loading)
                            fetch(page)
                    }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    enabled = !loading,
                    onClick = { fetch(initialPage) }
                ) { Text(if (loading) "Loading…" else "Load photos") }

                if (error != null) {
                    AssistChip(
                        onClick = { fetch(page) },
                        label = {
                            Text(
                                "Error: ${error}",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            when {
                loading && photos.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                photos.isEmpty() -> {
                    // Empty state (not loading & no results)
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("No photos yet")
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(onClick = { fetch(initialPage) }) { Text("Try loading") }
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(
                            items = photos,
                            key = { it.id }
                        ) { p ->
                            PhotoCard(
                                title = p.alt ?: p.photographer,
                                url = p.src.medium ?: p.src.large ?: p.src.original
                            )
                        }
                    }
                }
            }
        }
    }
}
