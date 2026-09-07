@file:OptIn(ExperimentalMaterial3Api::class)

package com.mochi.tl

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
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
            .background(MaterialTheme.colorScheme.background)
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
                TextButton(
                    onClick = { vm.clearHistory() },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Hapus Semua", fontSize = 13.sp)
                }
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (history.isEmpty()) {
                item {
                    EmptyStateBox(icon = Icons.Default.History, message = "Belum ada riwayat terjemahan tersimpan.")
                }
            }

            items(history) { record ->
                HistoryCard(record = record, onSelect = onSelectHistoryItem, onDelete = { vm.deleteHistoryItem(record.id) }, onCopy = {
                    clipboard.setPrimaryClip(ClipData.newPlainText("MochiTL", record.translatedText))
                    Toast.makeText(context, "Disalin ke clipboard", Toast.LENGTH_SHORT).show()
                })
            }
        }
    }
}

@Composable
private fun HistoryCard(
    record: TranslationRecord,
    onSelect: (String) -> Unit,
    onDelete: () -> Unit,
    onCopy: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            // Source preview
            Text(
                text = "📝 Sumber",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = record.sourcePreview,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            // Translated text
            Text(
                text = "✅ Hasil Terjemahan",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = record.translatedText,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = "Provider: ${record.providerId}  •  Target: ${record.targetLanguage}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                FilledTonalButton(
                    onClick = { onSelect(record.translatedText) },
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Edit", fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(onClick = onCopy, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Salin", modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun EmptyStateBox(icon: androidx.compose.ui.graphics.vector.ImageVector, message: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(40.dp)
            )
        }
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Provider section
        SectionTitle("Provider AI")
        providers.forEach { prov ->
            val isSelected = activeProvider.id == prov.id
            val currentModel = if (isSelected) modelText else (vm.storageModelFor(prov.id) ?: prov.model)
            ProviderSelectorCard(
                provider = prov,
                isSelected = isSelected,
                currentModel = currentModel,
                onClick = {
                    vm.selectProvider(prov)
                    apiKeyText = vm.apiKey.orEmpty()
                    baseUrlText = vm.customBaseUrl.orEmpty()
                    modelText = vm.customModel ?: prov.model
                }
            )
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        // Config section
        SectionTitle("Konfigurasi ${activeProvider.name}")

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
                },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        } else {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Text("Provider ini tidak memerlukan API Key (misal: Local Ollama / LM Studio).", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(12.dp))
            }
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
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            )
        )

        // Model section
        SectionTitle("Pilihan Model AI")
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
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
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
                            fetchModelStatus = "Gagal: ${res.exceptionOrNull()?.message}"
                        }
                    }
                },
                enabled = !isFetchingModels,
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp)
            ) {
                if (isFetchingModels) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Fetch", fontWeight = FontWeight.Bold, fontSize = 13.sp)
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

        // Test connection button
        Button(
            onClick = {
                isTesting = true
                testStatus = null
                scope.launch {
                    val result = vm.testConnection()
                    isTesting = false
                    testStatus = if (result.isSuccess) "Koneksi berhasil ✓" else "Koneksi gagal: ${result.exceptionOrNull()?.message}"
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
                Text("Menguji koneksi...", fontWeight = FontWeight.SemiBold)
            } else {
                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Uji Koneksi API", fontWeight = FontWeight.Bold)
            }
        }

        if (testStatus != null) {
            val isSuccess = testStatus!!.startsWith("Koneksi berhasil")
            Surface(
                color = if (isSuccess) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = testStatus!!,
                    modifier = Modifier.padding(10.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isSuccess) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        // Glossary integration
        SectionTitle("Integrasi Glosarium & Context")
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
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

        // Auto-save toggle
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

        // Generation parameters
        SectionTitle("Parameter Generasi AI")
        Text(
            "Berlaku untuk semua provider (Gemini, OpenAI, OpenRouter, lokal).",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
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

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
}

@Composable
@Composable
private fun ProviderSelectorCard(
    provider: ProviderConfig,
    isSelected: Boolean,
    currentModel: String,
    onClick: () -> Unit
) {
    if (isSelected) {
        OutlinedCard(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
            border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        ) {
            ProviderCardContent(provider, currentModel, true)
        }
    } else {
        OutlinedCard(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.outlinedCardColors()
        ) {
            ProviderCardContent(provider, currentModel, false)
        }
    }
}

@Composable
private fun ProviderCardContent(provider: ProviderConfig, currentModel: String, isSelected: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = isSelected, onClick = null)
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(provider.name, fontWeight = FontWeight.Bold)
            if (isSelected) {
                Text("Model: $currentModel • ${provider.baseUrl}", style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (!provider.requiresApiKey) {
            SuggestionChip(
                onClick = {},
                label = { Text(" Lokal", fontSize = 10.sp) }
            )
        }
    }
}

