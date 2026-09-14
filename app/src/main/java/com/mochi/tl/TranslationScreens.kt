@file:OptIn(ExperimentalMaterial3Api::class)

package com.mochi.tl

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mochi.tl.designsystem.MochiAppTheme
import com.mochi.tl.designsystem.components.*
import com.mochi.tl.designsystem.typography.MonospaceBodyMedium

@Composable
internal fun TextTranslationScreen(
    vm: MochiViewModel,
    onSwitchToFile: () -> Unit = {}
) {
    val state by vm.state.collectAsState()
    val providers by vm.providers.collectAsState()
    val activeProvider by vm.activeProvider.collectAsState()
    val prompts by vm.prompts.collectAsState()
    val activePrompt by vm.activePrompt.collectAsState()
    val glossaryList by vm.glossary.collectAsState()

    val context = LocalContext.current
    val clipboard = remember { context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager }

    var sourceLanguage by remember { mutableStateOf(LanguageOptions.AUTO_DETECT) }
    var targetLanguage by remember { mutableStateOf("Indonesia") }

    TextTranslationScreenContent(
        state = state,
        providers = providers,
        activeProvider = activeProvider,
        prompts = prompts,
        activePrompt = activePrompt,
        glossaryList = glossaryList,
        customModel = vm.customModel,
        sourceLanguage = sourceLanguage,
        targetLanguage = targetLanguage,
        onSourceLanguageChange = { sourceLanguage = it },
        onTargetLanguageChange = { targetLanguage = it },
        onSelectProvider = vm::selectProvider,
        onSelectPrompt = vm::selectPrompt,
        onSetInput = vm::setInput,
        onTranslate = { vm.translate(sourceLanguage = sourceLanguage, target = targetLanguage) },
        onPause = vm::pause,
        onResume = vm::resume,
        onCancel = vm::cancel,
        onSwitchToFile = onSwitchToFile,
        onExportText = { name, content -> (context as? MainActivity)?.exportText(name, content) },
        onCopyText = { text ->
            clipboard.setPrimaryClip(ClipData.newPlainText("MochiTL", text))
            Toast.makeText(context, "Disalin ke clipboard", Toast.LENGTH_SHORT).show()
        },
        onPasteText = {
            clipboard.primaryClip?.getItemAt(0)?.text?.let { vm.setInput(it.toString()) }
        },
        onShareText = { text ->
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, text)
                type = "text/plain"
            }
            context.startActivity(Intent.createChooser(sendIntent, "Bagikan Terjemahan"))
        }
    )
}

@Composable
internal fun TextTranslationScreenContent(
    state: TranslationState,
    providers: List<ProviderConfig>,
    activeProvider: ProviderConfig,
    prompts: List<PromptTemplate>,
    activePrompt: PromptTemplate,
    glossaryList: List<GlossaryEntry>,
    customModel: String?,
    sourceLanguage: String,
    targetLanguage: String,
    onSourceLanguageChange: (String) -> Unit,
    onTargetLanguageChange: (String) -> Unit,
    onSelectProvider: (ProviderConfig) -> Unit,
    onSelectPrompt: (PromptTemplate) -> Unit,
    onSetInput: (String) -> Unit,
    onTranslate: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    onSwitchToFile: () -> Unit,
    onExportText: (String, String) -> Unit,
    onCopyText: (String) -> Unit,
    onPasteText: () -> Unit,
    onShareText: (String) -> Unit
) {
    var showProviderMenu by remember { mutableStateOf(false) }
    var showPromptMenu by remember { mutableStateOf(false) }

    val matchedGlossaryTerms = remember(state.input, state.output, glossaryList) {
        if (state.input.isBlank() && state.output.isBlank()) emptyList()
        else glossaryList.filter {
            it.source.isNotBlank() && (
                state.input.contains(it.source, ignoreCase = true) ||
                state.output.contains(it.source, ignoreCase = true)
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Mode Switcher Segmented Button Row
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier.fillMaxWidth()
        ) {
            SegmentedButton(
                selected = true,
                onClick = {},
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
            ) {
                Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Teks Direct", style = MaterialTheme.typography.labelMedium)
            }
            SegmentedButton(
                selected = false,
                onClick = onSwitchToFile,
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
            ) {
                Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Dokumen File", style = MaterialTheme.typography.labelMedium)
            }
        }

        // Configuration Selector Bar (Provider & Prompt)
        MochiCard {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Provider Dropdown
                ExposedDropdownMenuBox(
                    expanded = showProviderMenu,
                    onExpandedChange = { showProviderMenu = !showProviderMenu },
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable, true),
                        onClick = { showProviderMenu = true },
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f, fill = false)
                            ) {
                                Icon(
                                    Icons.Default.Settings,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = activeProvider.name,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = customModel ?: activeProvider.model,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = showProviderMenu)
                        }
                    }
                    ExposedDropdownMenu(
                        expanded = showProviderMenu,
                        onDismissRequest = { showProviderMenu = false }
                    ) {
                        providers.forEach { prov ->
                            DropdownMenuItem(
                                text = { Text(prov.name, fontWeight = if (prov.id == activeProvider.id) FontWeight.Bold else FontWeight.Normal) },
                                onClick = {
                                    onSelectProvider(prov)
                                    showProviderMenu = false
                                }
                            )
                        }
                    }
                }

                // Prompt Selector
                ExposedDropdownMenuBox(
                    expanded = showPromptMenu,
                    onExpandedChange = { showPromptMenu = !showPromptMenu },
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable, true),
                        onClick = { showPromptMenu = true },
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f, fill = false)
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.Send,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = "Prompt Mode",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = activePrompt.name,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = showPromptMenu)
                        }
                    }
                    ExposedDropdownMenu(
                        expanded = showPromptMenu,
                        onDismissRequest = { showPromptMenu = false }
                    ) {
                        prompts.forEach { p ->
                            DropdownMenuItem(
                                text = { Text(p.name, fontWeight = if (p.id == activePrompt.id) FontWeight.Bold else FontWeight.Normal) },
                                onClick = {
                                    onSelectPrompt(p)
                                    showPromptMenu = false
                                }
                            )
                        }
                    }
                }
            }
        }

        // Language Pair Bar
        MochiCard {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                LanguageDropdownField(
                    label = "Sumber",
                    selectedValue = sourceLanguage,
                    options = LanguageOptions.SOURCE_LANGUAGES,
                    onValueChange = onSourceLanguageChange,
                    modifier = Modifier.weight(1f)
                )

                FilledTonalIconButton(
                    onClick = {
                        val temp = sourceLanguage
                        val newSource = targetLanguage
                        val newTarget = if (temp == LanguageOptions.AUTO_DETECT) {
                            if (newSource == "Indonesia") "Jepang" else "Indonesia"
                        } else {
                            temp
                        }
                        onSourceLanguageChange(newSource)
                        onTargetLanguageChange(newTarget)
                    },
                    modifier = Modifier.size(36.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        Icons.Default.SwapHoriz,
                        contentDescription = "Tukar Bahasa",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                LanguageDropdownField(
                    label = "Target",
                    selectedValue = targetLanguage,
                    options = LanguageOptions.TARGET_LANGUAGES,
                    onValueChange = onTargetLanguageChange,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Matched Glossary Terminology Chips (rendered ONLY when there is a real runtime match)
        if (matchedGlossaryTerms.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                matchedGlossaryTerms.take(3).forEach { term ->
                    MochiChip(
                        text = "${term.source} → ${term.target}",
                        icon = {
                            Icon(
                                Icons.Default.Book,
                                contentDescription = null,
                                modifier = Modifier
                                    .padding(end = 4.dp)
                                    .size(12.dp)
                            )
                        }
                    )
                }
            }
        }

        // Main Editor Stacks
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Source Text Input Box
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val charCount = state.input.length
                    Text(
                        text = "Teks Sumber ($charCount / 5.000)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f, fill = false),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (state.input.isNotBlank()) {
                            MochiGhostButton(onClick = { onSetInput("") }) {
                                Text("Bersihkan", fontSize = 11.sp, maxLines = 1)
                            }
                        }
                        MochiOutlinedButton(onClick = onPasteText) {
                            Text("Tempel", fontSize = 11.sp, maxLines = 1)
                        }
                    }
                }

                MochiTextField(
                    value = state.input,
                    onValueChange = onSetInput,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    textStyle = MonospaceBodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                    placeholder = {
                        Text(
                            "Tempel atau ketik teks mentah di sini...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
            }

            // Error Banner
            if (state.error != null) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = state.error.orEmpty(),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            // Progress Bar
            if (state.isTranslating) {
                MochiCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (state.isPaused) "Penerjemahan dijeda..." else "Menerjemahkan per chunk...",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${(state.progress * 100).toInt()}%",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        LinearProgressIndicator(
                            progress = { state.progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    }
                }
            }

            // Single Solid Emerald Primary CTA Button (Max 1 per screen)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!state.isTranslating) {
                    MochiButton(
                        onClick = onTranslate,
                        enabled = state.input.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Terjemahkan Teks", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    }
                } else {
                    MochiOutlinedButton(
                        onClick = { if (state.isPaused) onResume() else onPause() },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                    ) {
                        Icon(if (state.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (state.isPaused) "Lanjutkan" else "Jeda")
                    }

                    MochiOutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Batal", color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            // Translation Output Box
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Hasil Terjemahan AI",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f, fill = false),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (state.output.isNotBlank()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                            MochiOutlinedButton(onClick = { onCopyText(state.output) }) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Salin Teks", modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Salin", fontSize = 11.sp, maxLines = 1)
                            }
                            MochiOutlinedButton(onClick = { onShareText(state.output) }) {
                                Icon(Icons.Default.Share, contentDescription = "Bagikan", modifier = Modifier.size(14.dp))
                            }
                            MochiOutlinedButton(onClick = { onExportText("terjemahan_${System.currentTimeMillis()}.txt", state.output) }) {
                                Icon(Icons.Default.FileDownload, contentDescription = "Simpan File", modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }

                MochiCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    if (state.output.isNotBlank()) {
                        SelectionContainer {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = state.output,
                                    style = MaterialTheme.typography.bodyLarge,
                                    lineHeight = 22.sp
                                )
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.Translate,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Hasil terjemahan akan tampil di sini",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun FileTranslationScreen(
    vm: MochiViewModel,
    onSwitchToText: () -> Unit = {}
) {
    val state by vm.state.collectAsState()
    val context = LocalContext.current

    var fileName by remember { mutableStateOf<String?>(null) }
    var fileText by remember { mutableStateOf("") }
    var sourceLanguage by remember { mutableStateOf(LanguageOptions.AUTO_DETECT) }
    var targetLanguage by remember { mutableStateOf("Indonesia") }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            fileName = it.lastPathSegment ?: "Dokumen"
            fileText = FileParser.readText(context, it)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(12.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Mode Switcher Tab Bar
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier.fillMaxWidth()
        ) {
            SegmentedButton(
                selected = false,
                onClick = onSwitchToText,
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
            ) {
                Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Teks Direct", style = MaterialTheme.typography.labelMedium)
            }
            SegmentedButton(
                selected = true,
                onClick = {},
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
            ) {
                Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Dokumen File", style = MaterialTheme.typography.labelMedium)
            }
        }

        // Header Card
        MochiCard {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Icon(
                            Icons.Default.Description,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Penerjemah Dokumen", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Dukungan format TXT, EPUB, DOCX, & PDF.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        // Language Selector
        MochiCard {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                LanguageDropdownField(
                    label = "Sumber",
                    selectedValue = sourceLanguage,
                    options = LanguageOptions.SOURCE_LANGUAGES,
                    onValueChange = { sourceLanguage = it },
                    modifier = Modifier.weight(1f)
                )
                FilledTonalIconButton(
                    onClick = {
                        val temp = sourceLanguage
                        sourceLanguage = targetLanguage
                        targetLanguage = if (temp == LanguageOptions.AUTO_DETECT) {
                            if (sourceLanguage == "Indonesia") "Jepang" else "Indonesia"
                        } else {
                            temp
                        }
                    },
                    modifier = Modifier.size(36.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        Icons.Default.SwapHoriz,
                        contentDescription = "Tukar Bahasa",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                LanguageDropdownField(
                    label = "Target",
                    selectedValue = targetLanguage,
                    options = LanguageOptions.TARGET_LANGUAGES,
                    onValueChange = { targetLanguage = it },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // File picker button
        MochiOutlinedButton(
            onClick = {
                filePicker.launch(
                    arrayOf(
                        "text/plain",
                        "application/epub+zip",
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                        "application/pdf",
                        "*/*"
                    )
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (fileName == null) "Pilih Dokumen Teks" else "📄 $fileName", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }

        if (fileText.isNotBlank()) {
            MochiCard {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Info File Dokumen", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    }
                    Text("Ukuran: ${fileText.length} karakter", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Text("Pratinjau teks asli:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(fileText.take(300) + if (fileText.length > 300) "..." else "", style = MaterialTheme.typography.bodySmall)
                }
            }

            // Single solid emerald CTA button for File Translation
            MochiButton(
                onClick = { vm.translate(source = fileText, sourceLanguage = sourceLanguage, target = targetLanguage) },
                enabled = !state.isTranslating,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Terjemahkan Dokumen", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Progress indicator
        if (state.isTranslating) {
            MochiCard {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Progres Terjemahan", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        Text("${(state.progress * 100).toInt()}%", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    LinearProgressIndicator(
                        progress = { state.progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.outlineVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MochiOutlinedButton(
                            onClick = { if (state.isPaused) vm.resume() else vm.pause() },
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                        ) {
                            Text(if (state.isPaused) "Lanjutkan" else "Jeda")
                        }
                        MochiOutlinedButton(
                            onClick = vm::cancel,
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                        ) {
                            Text("Batal", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }

        // Result output
        if (state.output.isNotBlank()) {
            MochiCard {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Hasil Terjemahan", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    SelectionContainer {
                        Text(
                            text = state.output.take(800) + if (state.output.length > 800) "\n..." else "",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    HorizontalDivider()
                    MochiOutlinedButton(
                        onClick = {
                            val name = "terjemahan_${fileName?.removeSuffix(".txt") ?: "file"}.txt"
                            (context as? MainActivity)?.exportText(name, state.output)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Simpan File Dokumen", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Preview(name = "Translator Screen - Light Theme", showBackground = true)
@Composable
fun PreviewTextTranslationScreenLight() {
    MochiAppTheme(darkTheme = false, dynamicColor = false) {
        TextTranslationScreenContent(
            state = TranslationState(input = "その少年は、静まり返った森の奥深くで...", output = "Anak laki-laki itu menatap lempengan batu kuno..."),
            providers = BuiltIns.providers,
            activeProvider = BuiltIns.providers.first(),
            prompts = BuiltIns.prompts,
            activePrompt = BuiltIns.defaultPrompt,
            glossaryList = emptyList(),
            customModel = null,
            sourceLanguage = "Jepang",
            targetLanguage = "Indonesia",
            onSourceLanguageChange = {},
            onTargetLanguageChange = {},
            onSelectProvider = {},
            onSelectPrompt = {},
            onSetInput = {},
            onTranslate = {},
            onPause = {},
            onResume = {},
            onCancel = {},
            onSwitchToFile = {},
            onExportText = { _, _ -> },
            onCopyText = {},
            onPasteText = {},
            onShareText = {}
        )
    }
}

@Preview(name = "Translator Screen - Dark Theme", showBackground = true)
@Composable
fun PreviewTextTranslationScreenDark() {
    MochiAppTheme(darkTheme = true, dynamicColor = false) {
        TextTranslationScreenContent(
            state = TranslationState(input = "その少年は、静まり返った森の奥深くで...", output = "Anak laki-laki itu menatap lempengan batu kuno..."),
            providers = BuiltIns.providers,
            activeProvider = BuiltIns.providers.first(),
            prompts = BuiltIns.prompts,
            activePrompt = BuiltIns.defaultPrompt,
            glossaryList = emptyList(),
            customModel = null,
            sourceLanguage = "Jepang",
            targetLanguage = "Indonesia",
            onSourceLanguageChange = {},
            onTargetLanguageChange = {},
            onSelectProvider = {},
            onSelectPrompt = {},
            onSetInput = {},
            onTranslate = {},
            onPause = {},
            onResume = {},
            onCancel = {},
            onSwitchToFile = {},
            onExportText = { _, _ -> },
            onCopyText = {},
            onPasteText = {},
            onShareText = {}
        )
    }
}
