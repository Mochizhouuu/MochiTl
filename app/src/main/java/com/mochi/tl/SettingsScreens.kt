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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mochi.tl.designsystem.MochiAppTheme
import com.mochi.tl.designsystem.components.*
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

    HistoryScreenContent(
        history = history,
        onClearHistory = vm::clearHistory,
        onSelectHistoryItem = onSelectHistoryItem,
        onDeleteHistoryItem = vm::deleteHistoryItem,
        onCopyText = { text ->
            clipboard.setPrimaryClip(ClipData.newPlainText("MochiTL", text))
            Toast.makeText(context, "Disalin ke clipboard", Toast.LENGTH_SHORT).show()
        }
    )
}

@Composable
internal fun HistoryScreenContent(
    history: List<TranslationRecord>,
    onClearHistory: () -> Unit,
    onSelectHistoryItem: (String) -> Unit,
    onDeleteHistoryItem: (String) -> Unit,
    onCopyText: (String) -> Unit
) {
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
                MochiGhostButton(onClick = onClearHistory) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Hapus Semua", fontSize = 13.sp, color = MaterialTheme.colorScheme.error)
                }
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (history.isEmpty()) {
                item {
                    MochiEmptyState(
                        icon = Icons.Default.History,
                        title = "Belum Ada Riwayat",
                        description = "Hasil terjemahan yang disimpan akan muncul di sini."
                    )
                }
            }

            items(history) { record ->
                HistoryCard(
                    record = record,
                    onSelect = onSelectHistoryItem,
                    onDelete = { onDeleteHistoryItem(record.id) },
                    onCopy = { onCopyText(record.translatedText) }
                )
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
    MochiCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "Sumber",
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

            Text(
                text = "Hasil Terjemahan",
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
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MochiOutlinedButton(
                    onClick = { onSelect(record.translatedText) },
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Edit", fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.weight(1f))
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
internal fun SettingsScreen(
    vm: MochiViewModel,
    isDarkTheme: Boolean = false,
    isOledTheme: Boolean = false,
    onToggleTheme: () -> Unit = {},
    onToggleOled: () -> Unit = {}
) {
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
        showModelDropdown = false
        vm.availableModels.value = emptyList()
    }

    SettingsScreenContent(
        providers = providers,
        activeProvider = activeProvider,
        availableModels = availableModels,
        glossaryCount = glossaryList.size,
        activeProjectName = activeProject?.name,
        activeProjectGlossaryCount = activeProject?.glossaryIds?.size ?: 0,
        isDarkTheme = isDarkTheme,
        isOledTheme = isOledTheme,
        apiKeyText = apiKeyText,
        baseUrlText = baseUrlText,
        modelText = modelText,
        isKeyVisible = isKeyVisible,
        autoSaveHistory = autoSaveHistory,
        temperature = temperature,
        maxTokens = maxTokens,
        testStatus = testStatus,
        isTesting = isTesting,
        isFetchingModels = isFetchingModels,
        fetchModelStatus = fetchModelStatus,
        showModelDropdown = showModelDropdown,
        onToggleTheme = onToggleTheme,
        onToggleOled = onToggleOled,
        onSelectProvider = { prov ->
            vm.selectProvider(prov)
            apiKeyText = vm.apiKey.orEmpty()
            baseUrlText = vm.customBaseUrl.orEmpty()
            modelText = vm.customModel ?: prov.model
        },
        onApiKeyChange = {
            apiKeyText = it
            vm.apiKey = it
        },
        onBaseUrlChange = {
            baseUrlText = it
            vm.customBaseUrl = it
        },
        onModelChange = {
            modelText = it
            vm.setModelForActiveProvider(it)
        },
        onToggleKeyVisible = { isKeyVisible = !isKeyVisible },
        onAutoSaveChange = {
            autoSaveHistory = it
            vm.setAutoSave(it)
        },
        onTemperatureChange = {
            temperature = it
            vm.generationTemperature = it
        },
        onMaxTokensChange = {
            maxTokens = it
            vm.generationMaxTokens = it
        },
        onFetchModels = {
            isFetchingModels = true
            fetchModelStatus = null
            scope.launch {
                val res = vm.fetchModelsForActiveProvider()
                isFetchingModels = false
                if (res.isSuccess) {
                    val list = res.getOrDefault(emptyList())
                    fetchModelStatus = "Berhasil memuat ${list.size} model"
                    if (list.isNotEmpty()) showModelDropdown = true
                } else {
                    fetchModelStatus = "Gagal memuat model"
                }
            }
        },
        onTestConnection = {
            isTesting = true
            testStatus = null
            scope.launch {
                val result = vm.testConnection()
                isTesting = false
                testStatus = if (result.isSuccess) "Connected" else "Disconnected"
            }
        },
        onDismissModelDropdown = { showModelDropdown = false },
        onToggleModelDropdown = { if (availableModels.isNotEmpty()) showModelDropdown = !showModelDropdown },
        getStorageModelForProvider = vm::storageModelFor
    )
}

@Composable
internal fun SettingsScreenContent(
    providers: List<ProviderConfig>,
    activeProvider: ProviderConfig,
    availableModels: List<String>,
    glossaryCount: Int,
    activeProjectName: String?,
    activeProjectGlossaryCount: Int,
    isDarkTheme: Boolean,
    isOledTheme: Boolean,
    apiKeyText: String,
    baseUrlText: String,
    modelText: String,
    isKeyVisible: Boolean,
    autoSaveHistory: Boolean,
    temperature: Float,
    maxTokens: Int,
    testStatus: String?,
    isTesting: Boolean,
    isFetchingModels: Boolean,
    fetchModelStatus: String?,
    showModelDropdown: Boolean,
    onToggleTheme: () -> Unit,
    onToggleOled: () -> Unit,
    onSelectProvider: (ProviderConfig) -> Unit,
    onApiKeyChange: (String) -> Unit,
    onBaseUrlChange: (String) -> Unit,
    onModelChange: (String) -> Unit,
    onToggleKeyVisible: () -> Unit,
    onAutoSaveChange: (Boolean) -> Unit,
    onTemperatureChange: (Float) -> Unit,
    onMaxTokensChange: (Int) -> Unit,
    onFetchModels: () -> Unit,
    onTestConnection: () -> Unit,
    onDismissModelDropdown: () -> Unit,
    onToggleModelDropdown: () -> Unit,
    getStorageModelForProvider: (String) -> String?
) {
    val isLocalProvider = !activeProvider.requiresApiKey || activeProvider.id in listOf("ollama", "lmstudio")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Theme Settings
        SectionTitle("Tampilan & Tema")
        MochiCard {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Mode Gelap (Dark Mode)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text("Aktifkan tema gelap berdesaturasi rendah", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = isDarkTheme,
                        onCheckedChange = { onToggleTheme() }
                    )
                }

                if (isDarkTheme) {
                    HorizontalDivider()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("OLED / AMOLED True Black", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text("Latar belakang hitam murni (#000000) hemat baterai", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = isOledTheme,
                            onCheckedChange = { onToggleOled() }
                        )
                    }
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        // Provider section
        SectionTitle("Provider AI")
        providers.forEach { prov ->
            val isSelected = activeProvider.id == prov.id
            val currentModel = if (isSelected) modelText else (getStorageModelForProvider(prov.id) ?: prov.model)
            ProviderSelectorCard(
                provider = prov,
                isSelected = isSelected,
                currentModel = currentModel,
                onClick = { onSelectProvider(prov) }
            )
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        // Config section
        SectionTitle("Konfigurasi ${activeProvider.name}")

        if (activeProvider.requiresApiKey) {
            MochiTextField(
                value = apiKeyText,
                onValueChange = onApiKeyChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("API Key (${activeProvider.name})") },
                singleLine = true,
                visualTransformation = if (isKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = onToggleKeyVisible) {
                        Icon(
                            if (isKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (isKeyVisible) "Sembunyikan API key" else "Tampilkan API key"
                        )
                    }
                }
            )
        } else {
            MochiCard {
                Text("Provider lokal ini tidak memerlukan API Key.", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(12.dp))
            }
        }

        // Custom Base URL is strictly hidden for cloud providers and only shown when Ollama or LM Studio is selected
        if (isLocalProvider) {
            MochiTextField(
                value = baseUrlText,
                onValueChange = onBaseUrlChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Base URL Kustom (Opsional)") },
                placeholder = { Text("Contoh: ${activeProvider.baseUrl}") },
                singleLine = true
            )
        }

        // Model section
        SectionTitle("Pilihan Model AI")
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ExposedDropdownMenuBox(
                expanded = showModelDropdown && availableModels.isNotEmpty(),
                onExpandedChange = { onToggleModelDropdown() },
                modifier = Modifier.weight(1f)
            ) {
                MochiTextField(
                    value = modelText,
                    onValueChange = onModelChange,
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
                        onDismissRequest = onDismissModelDropdown
                    ) {
                        availableModels.forEach { modelName ->
                            DropdownMenuItem(
                                text = { Text(modelName, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                onClick = {
                                    onModelChange(modelName)
                                    onDismissModelDropdown()
                                }
                            )
                        }
                    }
                }
            }

            MochiOutlinedButton(
                onClick = onFetchModels,
                enabled = !isFetchingModels,
                modifier = Modifier.height(52.dp)
            ) {
                if (isFetchingModels) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Fetch", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }

        if (fetchModelStatus != null) {
            Text(
                text = fetchModelStatus,
                style = MaterialTheme.typography.bodySmall,
                color = if (fetchModelStatus.startsWith("Berhasil")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.SemiBold
            )
        }

        // Test Connection Button (Outlined, not solid CTA)
        MochiOutlinedButton(
            onClick = onTestConnection,
            enabled = !isTesting,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            if (isTesting) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Checking...", fontWeight = FontWeight.SemiBold)
            } else {
                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Tes Koneksi", fontWeight = FontWeight.Bold)
            }
        }

        if (testStatus != null) {
            val isConnected = testStatus == "Connected"
            MochiChip(
                text = if (isConnected) "Status: Connected ✓" else "Status: Disconnected ✗",
                containerColor = if (isConnected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
                contentColor = if (isConnected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
            )
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        // Glossary integration
        SectionTitle("Integrasi Glosarium & Context")
        MochiCard {
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
                    MochiChip(text = "$glossaryCount Istilah")
                }
                Text(
                    text = if (activeProjectName != null && activeProjectGlossaryCount > 0) {
                        "Proyek Aktif ('$activeProjectName') menautkan $activeProjectGlossaryCount istilah spesifik."
                    } else {
                        "Seluruh $glossaryCount istilah glosarium umum akan otomatis disuntikkan ke dalam instruksi terjemahan AI."
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
                onCheckedChange = onAutoSaveChange
            )
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        // Generation parameters
        SectionTitle("Parameter Generasi AI")
        Text(
            "Temperature: ${String.format(java.util.Locale.US, "%.1f", temperature)} " +
                    if (temperature <= 0.3f) "(konsisten/presisi)" else "(kreatif/ekspresif)",
            style = MaterialTheme.typography.bodySmall
        )
        Slider(
            value = temperature,
            onValueChange = onTemperatureChange,
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
                val v = ((it.toInt() + 127) / 256) * 256
                onMaxTokensChange(v.coerceIn(256, 32768))
            },
            valueRange = 256f..32768f
        )

        Text(
            "Pengaturan tersimpan otomatis saat diubah.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
}

@Composable
private fun ProviderSelectorCard(
    provider: ProviderConfig,
    isSelected: Boolean,
    currentModel: String,
    onClick: () -> Unit
) {
    MochiCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(selected = isSelected, onClick = null)
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(provider.name, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (isSelected) {
                    Text("Model: $currentModel • ${provider.baseUrl}", style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (!provider.requiresApiKey) {
                MochiChip(text = "Lokal")
            }
        }
    }
}

@Preview(name = "Settings Screen - Light Theme", showBackground = true)
@Composable
fun PreviewSettingsScreenLight() {
    MochiAppTheme(darkTheme = false, dynamicColor = false) {
        SettingsScreenContent(
            providers = BuiltIns.providers,
            activeProvider = BuiltIns.providers.first(),
            availableModels = emptyList(),
            glossaryCount = 12,
            activeProjectName = null,
            activeProjectGlossaryCount = 0,
            isDarkTheme = false,
            isOledTheme = false,
            apiKeyText = "••••••••",
            baseUrlText = "",
            modelText = "gemini-1.5-pro-latest",
            isKeyVisible = false,
            autoSaveHistory = true,
            temperature = 0.3f,
            maxTokens = 4096,
            testStatus = "Connected",
            isTesting = false,
            isFetchingModels = false,
            fetchModelStatus = null,
            showModelDropdown = false,
            onToggleTheme = {},
            onToggleOled = {},
            onSelectProvider = {},
            onApiKeyChange = {},
            onBaseUrlChange = {},
            onModelChange = {},
            onToggleKeyVisible = {},
            onAutoSaveChange = {},
            onTemperatureChange = {},
            onMaxTokensChange = {},
            onFetchModels = {},
            onTestConnection = {},
            onDismissModelDropdown = {},
            onToggleModelDropdown = {},
            getStorageModelForProvider = { null }
        )
    }
}

@Preview(name = "Settings Screen - Dark Theme", showBackground = true)
@Composable
fun PreviewSettingsScreenDark() {
    MochiAppTheme(darkTheme = true, dynamicColor = false) {
        SettingsScreenContent(
            providers = BuiltIns.providers,
            activeProvider = BuiltIns.providers.first(),
            availableModels = emptyList(),
            glossaryCount = 12,
            activeProjectName = null,
            activeProjectGlossaryCount = 0,
            isDarkTheme = true,
            isOledTheme = false,
            apiKeyText = "••••••••",
            baseUrlText = "",
            modelText = "gemini-1.5-pro-latest",
            isKeyVisible = false,
            autoSaveHistory = true,
            temperature = 0.3f,
            maxTokens = 4096,
            testStatus = "Connected",
            isTesting = false,
            isFetchingModels = false,
            fetchModelStatus = null,
            showModelDropdown = false,
            onToggleTheme = {},
            onToggleOled = {},
            onSelectProvider = {},
            onApiKeyChange = {},
            onBaseUrlChange = {},
            onModelChange = {},
            onToggleKeyVisible = {},
            onAutoSaveChange = {},
            onTemperatureChange = {},
            onMaxTokensChange = {},
            onFetchModels = {},
            onTestConnection = {},
            onDismissModelDropdown = {},
            onToggleModelDropdown = {},
            getStorageModelForProvider = { null }
        )
    }
}
