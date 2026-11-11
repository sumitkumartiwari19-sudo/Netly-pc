package com.skt.netly.pc.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.skt.netly.pc.ui.theme.Neu
import java.net.URL
import javax.imageio.ImageIO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Loads a JPEG/PNG thumbnail from the web (no extra library needed). Shows a flat placeholder until it arrives. */
@Composable
fun NetThumbnail(url: String, modifier: Modifier = Modifier, contentDescription: String? = null) {
    val c = Neu.colors
    val bitmap by produceState<ImageBitmap?>(initialValue = null, url) {
        value = withContext(Dispatchers.IO) { loadImage(url) }
    }
    Box(modifier.background(c.shadowDark)) {
        bitmap?.let {
            Image(
                bitmap = it,
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
    }
}

private fun loadImage(url: String): ImageBitmap? = runCatching {
    val connection = URL(url).openConnection().apply {
        connectTimeout = 8_000
        readTimeout = 8_000
    }
    connection.getInputStream().use { stream -> ImageIO.read(stream)?.toComposeImageBitmap() }
}.getOrNull()
