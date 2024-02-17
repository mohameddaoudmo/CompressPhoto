package com.example.compressphotowithworkmanager

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import coil.compose.AsyncImage
import com.example.compressphotowithworkmanager.Worker.Companion.KEY_COMPRESSION_THRESHOLD
import com.example.compressphotowithworkmanager.Worker.Companion.KEY_CONTENT_URI
import com.example.compressphotowithworkmanager.Worker.Companion.KEY_RESULT_PATH
import com.example.compressphotowithworkmanager.ui.theme.CompressPhotoWithWorkManagerTheme

class MainActivity : ComponentActivity() {
    private lateinit var workManager: WorkManager
    private val viewModel by viewModels<MainViewModel>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CompressPhotoWithWorkManagerTheme {
                val workerResult = viewModel.workId?.let { id ->
                    workManager.getWorkInfoByIdLiveData(id).observeAsState().value
                }
                LaunchedEffect(key1 = workerResult?.outputData) {
                    if (workerResult?.outputData != null) {
                        val filePath = workerResult.outputData.getString(
                            KEY_RESULT_PATH
                        )
                        filePath?.let {
                            val bitmap = BitmapFactory.decodeFile(it)
                            viewModel.updateCompressedBitmap(bitmap)
                        }
                    }
                }
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var scale by remember {
                        mutableStateOf(1f)
                    }
                    var offset by remember {
                        mutableStateOf(Offset.Zero)
                    }

                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        viewModel.uncompressedUri?.let {
                            Text(text = "Uncompressed photo:")
                            BoxWithConstraints(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1280f / 959f)
                            ) {
                                val state =
                                    rememberTransformableState { zoomChange, panChange, rotationChange ->
                                        scale = (scale * zoomChange).coerceIn(1f, 5f)

                                        val extraWidth = (scale - 1) * constraints.maxWidth
                                        val extraHeight = (scale - 1) * constraints.maxHeight

                                        val maxX = extraWidth / 2
                                        val maxY = extraHeight / 2

                                        offset = Offset(
                                            x = (offset.x + scale * panChange.x).coerceIn(
                                                -maxX,
                                                maxX
                                            ),
                                            y = (offset.y + scale * panChange.y).coerceIn(
                                                -maxY,
                                                maxY
                                            ),
                                        )
                                    }
                                AsyncImage(model = it, contentDescription = null, modifier = Modifier.graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                    translationX = offset.x
                                    translationY = offset.y
                                }
                                    .transformable(state))
                            }}
                            Spacer(modifier = Modifier.height(16.dp))
                            viewModel.compressedBitmap?.let {
                                Text(text = "Uncompressed photo:")
                                Image(bitmap = it.asImageBitmap(), contentDescription = null)
                            }
                        }
                    }
                }
            }
        }

        override fun onNewIntent(intent: Intent?) {
            super.onNewIntent(intent)
            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent?.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
            } else {
                intent?.getParcelableExtra(Intent.EXTRA_STREAM)
            } ?: return
            viewModel.updateUncompressUri(uri)

            val request = OneTimeWorkRequestBuilder<Worker>()
                .setInputData(
                    workDataOf(
                        KEY_CONTENT_URI to uri.toString(),
                        KEY_COMPRESSION_THRESHOLD to 1024 * 20L
                    )
                )
                .build()
            viewModel.updateWorkId(request.id)
            workManager.enqueue(request)
        }
    }


    @Composable
    fun Greeting(name: String, modifier: Modifier = Modifier) {
        Text(
            text = "Hello $name!",
            modifier = modifier
        )
    }

    @Preview(showBackground = true)
    @Composable
    fun GreetingPreview() {
        CompressPhotoWithWorkManagerTheme {
            Greeting("Android")
        }
    }