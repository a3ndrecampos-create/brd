package com.tapago.feature.photoshare.presentation

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera

/**
 * Tela de captura: mostra o preview da câmera com os stickers de estatística
 * já sobrepostos ao vivo (aproximação visual — o overlay definitivo é
 * "queimado" no bitmap após a captura, ver [StatsOverlayComposer]).
 */
@Composable
fun PhotoShareScreen(
    sessionId: String,
    onDone: () -> Unit,
    viewModel: PhotoShareViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    if (state.composedImageUri != null) {
        SharePreview(
            imageUri = state.composedImageUri!!,
            isSharing = state.isSharing,
            onShareClick = viewModel::onShareToInstagramClicked,
            onDone = onDone,
        )
    } else {
        CameraCaptureContent(onPhotoCaptured = viewModel::onPhotoCaptured)
    }
}

@Composable
private fun CameraCaptureContent(onPhotoCaptured: (Bitmap) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val imageCapture = remember { ImageCapture.Builder().build() }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageCapture,
                    )
                }, ContextCompat.getMainExecutor(ctx))
                previewView
            },
        )

        FloatingActionButton(
            onClick = { capturePhoto(imageCapture, context, onPhotoCaptured) },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(32.dp),
        ) {
            Icon(imageVector = Icons.Filled.PhotoCamera, contentDescription = "Tirar foto")
        }
    }
}

private fun capturePhoto(
    imageCapture: ImageCapture,
    context: android.content.Context,
    onPhotoCaptured: (Bitmap) -> Unit,
) {
    imageCapture.takePicture(
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                onPhotoCaptured(image.toBitmap())
                image.close()
            }

            override fun onError(exception: androidx.camera.core.ImageCaptureException) {
                // TODO: exibir erro de captura na UI
            }
        },
    )
}

private fun ImageProxy.toBitmap(): Bitmap {
    val buffer = planes[0].buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    val bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    val matrix = Matrix().apply { postRotate(imageInfo.rotationDegrees.toFloat()) }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}

@Composable
private fun SharePreview(
    imageUri: android.net.Uri,
    isSharing: Boolean,
    onShareClick: () -> Unit,
    onDone: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        AsyncImage(
            model = imageUri,
            contentDescription = "Foto com estatísticas da atividade",
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 16.dp),
        )
        Button(
            onClick = onShareClick,
            enabled = !isSharing,
            modifier = Modifier.padding(16.dp),
        ) {
            Text(if (isSharing) "Abrindo Instagram..." else "Compartilhar no Instagram Stories")
        }
        Button(onClick = onDone, modifier = Modifier.padding(horizontal = 16.dp)) {
            Text("Concluir")
        }
    }
}
