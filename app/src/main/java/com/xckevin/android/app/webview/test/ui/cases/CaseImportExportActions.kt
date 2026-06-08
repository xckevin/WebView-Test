package com.xckevin.android.app.webview.test.ui.cases

import android.content.ContentResolver
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.xckevin.android.app.webview.test.R
import com.xckevin.android.app.webview.test.data.CaseImportExport
import com.xckevin.android.app.webview.test.data.TestCaseRepository
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun CaseImportExportActions(
    testCaseRepository: TestCaseRepository,
    contentResolver: ContentResolver,
    onStatusMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val coroutineScope = rememberCoroutineScope()
    val exportSuccess = stringResource(R.string.cases_exported)
    val exportFailed = stringResource(R.string.cases_export_failed)
    val importSuccess = stringResource(R.string.cases_imported_skipped)
    val importFailed = stringResource(R.string.cases_import_failed)
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        coroutineScope.launch {
            val result = runCatching {
                exportCases(contentResolver = contentResolver, uri = uri, repository = testCaseRepository)
            }
            onStatusMessage(
                result.fold(
                    onSuccess = { count -> exportSuccess.format(count) },
                    onFailure = { error -> exportFailed.format(error.message.orEmpty()) },
                )
            )
        }
    }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        coroutineScope.launch {
            val result = runCatching {
                importCases(contentResolver = contentResolver, uri = uri, repository = testCaseRepository)
            }
            onStatusMessage(
                result.fold(
                    onSuccess = { (importedCount, skippedCount) ->
                        importSuccess.format(importedCount, skippedCount)
                    },
                    onFailure = { error -> importFailed.format(error.message.orEmpty()) },
                )
            )
        }
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedButton(onClick = { exportLauncher.launch("webview-test-cases.json") }) {
            Text(stringResource(R.string.cases_export))
        }
        OutlinedButton(onClick = { importLauncher.launch(arrayOf("application/json")) }) {
            Text(stringResource(R.string.cases_import))
        }
    }
}

private suspend fun exportCases(
    contentResolver: ContentResolver,
    uri: Uri,
    repository: TestCaseRepository,
): Int = withContext(Dispatchers.IO) {
    val cases = repository.observeAll().first()
    val raw = CaseImportExport.exportCases(cases)
    contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { writer ->
        writer.write(raw)
    } ?: throw IOException("Unable to open export document")
    cases.size
}

private suspend fun importCases(
    contentResolver: ContentResolver,
    uri: Uri,
    repository: TestCaseRepository,
): Pair<Int, Int> = withContext(Dispatchers.IO) {
    val raw = contentResolver.openInputStream(uri)?.bufferedReader()?.use { reader ->
        reader.readText()
    } ?: throw IOException("Unable to open import document")
    val incoming = CaseImportExport.importCases(raw)
    val existing = repository.observeAll().first()
    val conflictKeys = CaseImportExport.findConflicts(existing, incoming)
        .map { it.incoming.name.trim() to it.incoming.url.trim() }
        .toSet()
    val casesToImport = incoming.filterNot { it.name.trim() to it.url.trim() in conflictKeys }
    casesToImport.forEach { repository.upsert(it) }
    casesToImport.size to conflictKeys.size
}
