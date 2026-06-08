package com.xckevin.android.app.webview.test.ui.scanner

import android.Manifest
import android.content.ClipData
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.NoPhotography
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xckevin.android.app.webview.test.R
import com.xckevin.android.app.webview.test.scanner.BarcodeAnalyzer
import com.xckevin.android.app.webview.test.scanner.ParsedScanResult
import com.xckevin.android.app.webview.test.scanner.ScannerViewModel
import com.xckevin.android.app.webview.test.ui.common.AppScaffold
import java.util.concurrent.Executors
import kotlinx.coroutines.launch

@Composable
fun ScannerScreen(
    onResult: (String) -> Unit,
    onClose: () -> Unit,
) {
    val viewModel: ScannerViewModel = viewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = viewModel::onCameraPermissionResult,
    )

    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA,
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            viewModel.onCameraPermissionResult(true)
        } else {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }

    LaunchedEffect(state.parsedResult) {
        val url = (state.parsedResult as? ParsedScanResult.Url)?.normalizedUrl
        if (url != null) {
            onResult(url)
        }
    }

    AppScaffold { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when {
                state.cameraPermissionGranted == null -> ScannerStatus(
                    icon = Icons.Outlined.CameraAlt,
                    title = stringResource(R.string.scanner_opening_camera),
                    detail = stringResource(R.string.scanner_waiting_permission),
                    onClose = onClose,
                )

                state.cameraPermissionGranted == false -> ScannerStatus(
                    icon = Icons.Outlined.NoPhotography,
                    title = stringResource(R.string.scanner_permission_denied),
                    detail = stringResource(R.string.scanner_permission_required),
                    onClose = onClose,
                )

                state.parsedResult is ParsedScanResult.Text -> TextScanResult(
                    result = state.parsedResult as ParsedScanResult.Text,
                    editableUrl = state.editableUrl,
                    editError = state.editError,
                    onEditableUrlChanged = viewModel::onEditableUrlChanged,
                    onUseAsUrl = viewModel::useEditedTextAsUrl,
                    onClose = onClose,
                    modifier = Modifier.fillMaxSize(),
                )

                state.cameraError != null -> ScannerStatus(
                    icon = Icons.Outlined.ErrorOutline,
                    title = stringResource(R.string.scanner_unavailable),
                    detail = state.cameraError.orEmpty(),
                    onClose = onClose,
                )

                else -> CameraScanner(
                    onBarcode = viewModel::onRawScanValue,
                    onCameraError = viewModel::onCameraError,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            ScannerTopBar(
                onClose = onClose,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun CameraScanner(
    onBarcode: (String) -> Unit,
    onCameraError: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }
    val currentOnBarcode = rememberUpdatedState(onBarcode)
    val analyzer = remember {
        BarcodeAnalyzer(onBarcode = { rawValue -> currentOnBarcode.value(rawValue) })
    }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose {
            analyzer.close()
            cameraExecutor.shutdown()
        }
    }

    CameraBindingEffect(
        previewView = previewView,
        lifecycleOwner = lifecycleOwner,
        analyzer = analyzer,
        cameraExecutor = cameraExecutor,
        onCameraError = onCameraError,
    )

    AndroidView(
        factory = { previewView },
        modifier = modifier.background(Color.Black),
    )
}

@Composable
private fun CameraBindingEffect(
    previewView: PreviewView,
    lifecycleOwner: LifecycleOwner,
    analyzer: BarcodeAnalyzer,
    cameraExecutor: java.util.concurrent.ExecutorService,
    onCameraError: (String) -> Unit,
) {
    val context = LocalContext.current
    val currentOnCameraError by rememberUpdatedState(onCameraError)
    val unableToOpenCamera = stringResource(R.string.scanner_unable_open_camera)
    val unableToBindPreview = stringResource(R.string.scanner_unable_bind_preview)

    DisposableEffect(previewView, lifecycleOwner, analyzer, cameraExecutor) {
        var disposed = false
        var cameraProvider: ProcessCameraProvider? = null
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener(
            {
                val provider = runCatching { cameraProviderFuture.get() }
                    .onFailure { error ->
                        currentOnCameraError(error.message ?: unableToOpenCamera)
                    }
                    .getOrNull() ?: return@addListener
                cameraProvider = provider
                if (disposed) {
                    provider.unbindAll()
                    return@addListener
                }

                val preview = Preview.Builder()
                    .build()
                    .also { it.surfaceProvider = previewView.surfaceProvider }
                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { it.setAnalyzer(cameraExecutor, analyzer) }

                runCatching {
                    provider.unbindAll()
                    provider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageAnalysis,
                    )
                }.onFailure { error ->
                    currentOnCameraError(error.message ?: unableToBindPreview)
                }
            },
            ContextCompat.getMainExecutor(context),
        )

        onDispose {
            disposed = true
            cameraProvider?.unbindAll()
        }
    }
}

@Composable
private fun ScannerTopBar(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.54f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.screen_scanner),
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        IconButton(
            onClick = onClose,
            colors = IconButtonDefaults.iconButtonColors(
                contentColor = Color.White,
            ),
        ) {
            Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.action_close))
        }
    }
}

@Composable
private fun ScannerStatus(
    icon: ImageVector,
    title: String,
    detail: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = title,
                modifier = Modifier.padding(top = 16.dp),
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = detail,
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = onClose,
                modifier = Modifier.padding(top = 24.dp),
            ) {
                Text(stringResource(R.string.action_close))
            }
        }
    }
}

@Composable
private fun TextScanResult(
    result: ParsedScanResult.Text,
    editableUrl: String,
    editError: String?,
    onEditableUrlChanged: (String) -> Unit,
    onUseAsUrl: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val clipboard = LocalClipboard.current
    val coroutineScope = rememberCoroutineScope()
    var copied by remember { mutableStateOf(false) }

    Surface(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 80.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.scanner_scanned_text),
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = result.value,
                style = MaterialTheme.typography.bodyLarge,
            )
            OutlinedTextField(
                value = editableUrl,
                onValueChange = onEditableUrlChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.scanner_use_as_url)) },
                singleLine = true,
                isError = editError != null,
                supportingText = editError?.let { error -> { Text(error.localizedScannerError()) } },
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = {
                        coroutineScope.launch {
                            clipboard.setClipEntry(
                                ClipData.newPlainText("scan-result", result.value).toClipEntry()
                            )
                            copied = true
                        }
                    },
                ) {
                    Icon(
                        Icons.Outlined.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        if (copied) stringResource(R.string.action_copied) else stringResource(R.string.action_copy),
                        modifier = Modifier.padding(start = 4.dp),
                    )
                }
                Button(onClick = onUseAsUrl) {
                    Icon(
                        Icons.Outlined.OpenInBrowser,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(stringResource(R.string.scanner_use_as_url), modifier = Modifier.padding(start = 4.dp))
                }
                OutlinedButton(onClick = onClose) {
                    Text(stringResource(R.string.action_close))
                }
            }
        }
    }
}

@Composable
private fun String.localizedScannerError(): String =
    when (this) {
        "Enter a valid http or https URL" -> stringResource(R.string.error_invalid_http_url)
        else -> this
    }
