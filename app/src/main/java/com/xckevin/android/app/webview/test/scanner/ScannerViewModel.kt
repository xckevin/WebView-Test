package com.xckevin.android.app.webview.test.scanner

import androidx.lifecycle.ViewModel
import com.xckevin.android.app.webview.test.util.UrlNormalizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class ScannerUiState(
    val cameraPermissionGranted: Boolean? = null,
    val parsedResult: ParsedScanResult? = null,
    val editableUrl: String = "",
    val editError: String? = null,
    val cameraError: String? = null,
)

class ScannerViewModel : ViewModel() {
    private val _state = MutableStateFlow(ScannerUiState())
    val state: StateFlow<ScannerUiState> = _state.asStateFlow()

    fun onCameraPermissionResult(granted: Boolean) {
        _state.update { it.copy(cameraPermissionGranted = granted) }
    }

    fun onRawScanValue(rawValue: String) {
        val parsedResult = ScanResultParser.parse(rawValue)
        if (parsedResult == ParsedScanResult.Empty) return

        _state.update { currentState ->
            if (currentState.parsedResult != null) {
                currentState
            } else {
                currentState.copy(
                    parsedResult = parsedResult,
                    editableUrl = when (parsedResult) {
                        ParsedScanResult.Empty -> ""
                        is ParsedScanResult.Text -> parsedResult.value
                        is ParsedScanResult.Url -> parsedResult.normalizedUrl
                    },
                    editError = null,
                )
            }
        }
    }

    fun onEditableUrlChanged(value: String) {
        _state.update { it.copy(editableUrl = value, editError = null) }
    }

    fun useEditedTextAsUrl() {
        val normalizedUrl = UrlNormalizer.normalizeRemoteUrl(state.value.editableUrl)
        if (normalizedUrl == null) {
            _state.update { it.copy(editError = "Enter a valid http or https URL") }
            return
        }

        _state.update {
            it.copy(
                parsedResult = ParsedScanResult.Url(normalizedUrl),
                editableUrl = normalizedUrl,
                editError = null,
            )
        }
    }

    fun onCameraError(message: String) {
        _state.update { it.copy(cameraError = message) }
    }
}
