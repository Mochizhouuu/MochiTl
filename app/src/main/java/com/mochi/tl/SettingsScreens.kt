@file:OptIn(ExperimentalMaterial3Api::class)

package com.mochi.tl

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.app.Activity
import android.net.Uri
import android.os.Bundle
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
/**
 * Layar riwayat terjemahan dan pengaturan AI.
 */
@Composable
internal fun HistoryScreen(
    vm: MochiViewModel,
    onSelectHistoryItem: (String) -> Unit
) {
    val history by vm.history.collectAsState()
    val context = LocalContext.current
    val clipboard = remember { context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Riwayat Terjemahan",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (history.isNotEmpty()) {
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(
                    onClick = { vm.clearHistory() },
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Bersihkan")
                }
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (history.isEmpty()) {
                item {
                    Text("Belum ada riwayat terjemahan tersimpan.", style = MaterialTheme.typography.bodyMedium)
                }
            }

            items(history) { record ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = record.sourcePreview,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                        Text(
                            text = record.translatedText,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Provider: ${record.providerId} • Target: ${record.targetLanguage}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilledTonalButton(
                                onClick = { onSelectHistoryItem(record.translatedText) },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Buka di Editor")
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                IconButton(
                                    onClick = {
                                        clipboard.setPrimaryClip(ClipData.newPlainText("MochiTL", record.translatedText))
                                        Toast.makeText(context, "Disalin ke clipboard", Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Salin")
                                }
                                IconButton(onClick = { vm.deleteHistoryItem(record.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun SettingsScreen(vm: MochiViewModel) {
    val providers by vm.providers.collectAsState()
    val activeProvider by vm.activeProvider.collectAsState()
    val availableModels by vm.availableModels.collectAsState()
    val glossaryList by vm.glossary.collectAsState()
    val activeProject by vm.activeProject.collectAsState()
    val scope = rememberCoroutineScope()

    var apiKeyText by remember { mutableStateOf(vm.apiKey.orEmpty()) }
    var baseUrlText by remember { mutableStateOf(vm.customBaseUrl.orEmpty()) }
    var modelText by remember { mutableStateOf(vm.customModel ?: activeProvider.model) }
    var isKeyVisible by remember { mutableStateOf(false) }
    var autoSaveHistory by remember { mutableStateOf(vm.autoSave()) }
    var temperature by remember { mutableStateOf(vm.generationTemperature) }
    var maxTokens by remember { mutableStateOf(vm.generationMaxTokens) }

    var testStatus by remember { mutableStateOf<String?>(null) }
    var isTesting by remember { mutableStateOf(false) }
    var isFetchingModels by remember { mutableStateOf(false) }
    var fetchModelStatus by remember { mutableStateOf<String?>(null) }
    var showModelDropdown by remember { mutableStateOf(false) }

    LaunchedEffect(activeProvider.id) {
        apiKeyText = vm.apiKey.orEmpty()
        baseUrlText = vm.customBaseUrl.orEmpty()
        modelText = vm.customModel ?: activeProvider.model
        testStatus = null
        fetchModelStatus = null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Pilih Provider AI", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        providers.forEach { prov ->
            val isSelected = activeProvider.id == prov.id
            val currentModel = if (isSelected) modelText else (vm.storageModelFor(prov.id) ?: prov.model)
            OutlinedCard(
                onClick = {
                    vm.selectProvider(prov)
                    apiKeyText = vm.apiKey.orEmpty()
                    baseUrlText = vm.customBaseUrl.orEmpty()
                    modelText = vm.customModel ?: prov.model
                },
                modifier = Modifier.fillMaxWidth(),
                colors = if (isSelected) CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer) else CardDefaults.outlinedCardColors()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = isSelected, onClick = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(prov.name, fontWeight = FontWeight.Bold)
                        Text("Model: $currentModel • ${prov.baseUrl}", style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        Text("Konfigurasi ${activeProvider.name}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        if (activeProvider.requiresApiKey) {
            OutlinedTextField(
                value = apiKeyText,
                onValueChange = {
                    apiKeyText = it
                    vm.apiKey = it
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("API Key (${activeProvider.name})") },
                singleLine = true,
                visualTransformation = if (isKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { isKeyVisible = !isKeyVisible }) {
                        Icon(if (isKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = "Toggle key")
                    }
                }
            )
        } else {
            Text("Provider ini tidak memerlukan API Key (misal: Local Ollama / LM Studio).", style = MaterialTheme.typography.bodyMedium)
        }

        OutlinedTextField(
            value = baseUrlText,
            onValueChange = {
                baseUrlText = it
                vm.customBaseUrl = it
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Base URL Kustom (Opsional)") },
            placeholder = { Text("Contoh: ${activeProvider.baseUrl}") },
            singleLine = true
        )

        // Model Selection & Fetch Button
        Text("Pilihan Model AI", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ExposedDropdownMenuBox(
                expanded = showModelDropdown && availableModels.isNotEmpty(),
                onExpandedChange = { if (availableModels.isNotEmpty()) showModelDropdown = !showModelDropdown },
                modifier = Modifier.weight(1f)
            ) {
                OutlinedTextField(
                    value = modelText,
                    onValueChange = {
                        modelText = it
                        vm.setModelForActiveProvider(it)
                    },
                    modifier = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryEditable, true)
                        .fillMaxWidth(),
                    label = { Text("Nama Model AI") },
                    singleLine = true,
                    trailingIcon = {
                        if (availableModels.isNotEmpty()) {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = showModelDropdown)
                        }
                    }
                )
                if (availableModels.isNotEmpty()) {
                    ExposedDropdownMenu(
                        expanded = showModelDropdown,
                        onDismissRequest = { showModelDropdown = false }
                    ) {
                        availableModels.forEach { modelName ->
                            DropdownMenuItem(
                                text = { Text(modelName, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                onClick = {
                                    modelText = modelName
                                    vm.setModelForActiveProvider(modelName)
                                    showModelDropdown = false
                                }
                            )
                        }
                    }
                }
            }

            Button(
                onClick = {
                    isFetchingModels = true
                    fetchModelStatus = null
                    scope.launch {
                        val res = vm.fetchModelsForActiveProvider()
                        isFetchingModels = false
                        if (res.isSuccess) {
                            val list = res.getOrDefault(emptyList())
                            fetchModelStatus = "Berhasil memuat ${list.size} model!"
                            if (list.isNotEmpty()) showModelDropdown = true
                        } else {
                            fetchModelStatus = "Gagal memuat model: ${res.exceptionOrNull()?.message}"
                        }
                    }
                },
                enabled = !isFetchingModels,
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp)
            ) {
                if (isFetchingModels) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Fetch", fontWeight = FontWeight.Bold)
                }
            }
        }

        if (fetchModelStatus != null) {
            Text(
                text = fetchModelStatus!!,
                style = MaterialTheme.typography.bodySmall,
                color = if (fetchModelStatus!!.startsWith("Berhasil")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.SemiBold
            )
        }

        Button(
            onClick = {
                isTesting = true
                testStatus = null
                scope.launch {
                    val result = vm.testConnection()
                    isTesting = false
                    testStatus = if (result.isSuccess) "Koneksi Berhasil Disambungkan! ✓" else "Koneksi Gagal: ${result.exceptionOrNull()?.message}"
                }
            },
            enabled = !isTesting,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (isTesting) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Menguji Koneksi...", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            } else {
                Icon(Icons.Default.CheckCircle, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Uji Koneksi API", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (testStatus != null) {
            Text(
                text = testStatus!!,
                color = if (testStatus!!.startsWith("Koneksi Berhasil")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold
            )
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        Text("Integrasi Glosarium & Context", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Total Istilah Glosarium", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                    SuggestionChip(
                        onClick = {},
                        label = { Text("${glossaryList.size} Istilah", fontWeight = FontWeight.Bold) }
                    )
                }
                Text(
                    text = if (activeProject != null && activeProject!!.glossaryIds.isNotEmpty()) {
                        "Proyek Aktif ('${activeProject!!.name}') menautkan ${activeProject!!.glossaryIds.size} istilah spesifik."
                    } else {
                        "Seluruh ${glossaryList.size} istilah glosarium umum akan otomatis disuntikkan ke dalam instruksi terjemahan AI."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Simpan Riwayat Otomatis", fontWeight = FontWeight.Bold)
                Text("Menyimpan hasil terjemahan ke tab Riwayat", style = MaterialTheme.typography.bodySmall)
            }
            Switch(
                checked = autoSaveHistory,
                onCheckedChange = {
                    autoSaveHistory = it
                    vm.setAutoSave(it)
                }
            )
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        // ===== Parameter Generasi AI =====
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("Parameter Generasi AI", fontWeight = FontWeight.Bold)
            Text(
                "Berlaku untuk semua provider (Gemini, OpenAI, OpenRouter, lokal).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "Temperature: ${String.format(java.util.Locale.US, "%.1f", temperature)} " +
                        if (temperature <= 0.3f) "(konsisten/presisi)" else "(kreatif/ekspresif)",
                style = MaterialTheme.typography.bodySmall
            )
            Slider(
                value = temperature,
                onValueChange = {
                    temperature = it
                    vm.generationTemperature = it
                },
                valueRange = 0f..1.5f
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Maks token per chunk: $maxTokens",
                style = MaterialTheme.typography.bodySmall
            )
            Slider(
                value = maxTokens.toFloat(),
                onValueChange = {
                    val v = (it.toInt() / 256) * 256
                    maxTokens = v
                    vm.generationMaxTokens = v
                },
                valueRange = 1024f..16384f
            )
        }
    }
}
