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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Layar penerjemahan teks langsung dan file dokumen untuk power user.
 */
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
    val activeProject by vm.activeProject.collectAsState()

    val context = LocalContext.current
    val clipboard = remember { context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager }

    var sourceLanguage by remember { mutableStateOf(LanguageOptions.AUTO_DETECT) }
    var targetLanguage by remember { mutableStateOf("Indonesia") }

    var showProviderMenu by remember { mutableStateOf(false) }
    var showPromptMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Mode Switcher Tab Bar (Direct Text vs File Document)
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

        // Configuration Bar (Provider, Model, Prompt selector)
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Provider Selector
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
                                        text = vm.customModel ?: activeProvider.model,
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
                                    vm.selectProvider(prov)
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
                                    vm.selectPrompt(p)
                                    showPromptMenu = false
                                }
                            )
                        }
                    }
                }
            }
        }

        // Language Selector Row
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
        ) {
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

        // Power User Stats Badge Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val boundGlossaryCount = if (activeProject != null && activeProject!!.glossaryIds.isNotEmpty()) {
                activeProject!!.glossaryIds.size
            } else {
                glossaryList.size
            }

            SuggestionChip(
                onClick = {},
                label = { Text("Glosarium: $boundGlossaryCount istilah", style = MaterialTheme.typography.labelSmall) },
                icon = { Icon(Icons.Default.Book, contentDescription = null, modifier = Modifier.size(12.dp)) }
            )

            val estimatedTokens = (state.input.length / 4).coerceAtLeast(0)
            SuggestionChip(
                onClick = {},
                label = { Text("Est. Token: ~$estimatedTokens", style = MaterialTheme.typography.labelSmall) },
                icon = { Icon(Icons.Default.Analytics, contentDescription = null, modifier = Modifier.size(12.dp)) }
            )
        }

        // Stacked Split Editor Section
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Source Text Editor Box
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val charCount = state.input.length
                    val wordCount = if (state.input.isBlank()) 0 else state.input.trim().split("\\s+".toRegex()).size

                    Text(
                        text = "Teks Sumber ($charCount kar • $wordCount kata)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        if (state.input.isNotBlank()) {
                            IconButton(
                                onClick = { vm.setInput("") },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    Icons.Default.Clear,
                                    contentDescription = "Hapus Input",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        IconButton(
                            onClick = {
                                clipboard.primaryClip?.getItemAt(0)?.text?.let { vm.setInput(it.toString()) }
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Default.ContentPaste,
                                contentDescription = "Tempel Clipboard",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = state.input,
                    onValueChange = vm::setInput,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    placeholder = {
                        Text(
                            "Tempel atau ketik teks mentah di sini...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
            }

            // Error Message Banner
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

            // Translation Progress Indicator
            if (state.isTranslating) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
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
                                color = MaterialTheme.colorScheme.onPrimaryContainer
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
                            trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
                        )
                    }
                }
            }

            // Action Controls Toolbar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!state.isTranslating) {
                    Button(
                        onClick = { vm.translate(sourceLanguage = sourceLanguage, target = targetLanguage) },
                        enabled = state.input.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Terjemahkan Teks", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    }
                } else {
                    FilledTonalButton(
                        onClick = { if (state.isPaused) vm.resume() else vm.pause() },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(if (state.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (state.isPaused) "Lanjutkan" else "Jeda", fontWeight = FontWeight.SemiBold)
                    }

                    OutlinedButton(
                        onClick = vm::cancel,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Batal", fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Output Translation Box
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
                        color = MaterialTheme.colorScheme.primary
                    )

                    if (state.output.isNotBlank()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            IconButton(
                                onClick = {
                                    clipboard.setPrimaryClip(ClipData.newPlainText("MochiTL", state.output))
                                    Toast.makeText(context, "Disalin ke clipboard", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Salin", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            }
                            IconButton(
                                onClick = {
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, state.output)
                                        type = "text/plain"
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, "Bagikan Terjemahan"))
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = "Bagikan", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            }
                            IconButton(
                                onClick = {
                                    (context as? MainActivity)?.exportText("terjemahan_${System.currentTimeMillis()}.txt", state.output)
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.FileDownload, contentDescription = "Simpan File", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))
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
    var fileName by remember { mutableStateOf<String?>(null) }
    var fileText by remember { mutableStateOf("") }
    val state by vm.state.collectAsState()
    val context = LocalContext.current

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
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
        ) {
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
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
        ) {
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
        Button(
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
                .height(48.dp),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (fileName == null) "Pilih Dokumen Teks" else "📄 ${fileName}", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }

        if (fileText.isNotBlank()) {
            // File info card
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Info File Dokumen", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    }
                    Text("Ukuran: ${fileText.length} karakter", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Estimasi chunk API: ~${(fileText.length / 4000) + 1} bagian", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Text("Pratinjau teks asli:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(fileText.take(300) + if (fileText.length > 300) "..." else "", style = MaterialTheme.typography.bodySmall)
                }
            }

            Button(
                onClick = { vm.translate(source = fileText, sourceLanguage = sourceLanguage, target = targetLanguage) },
                enabled = !state.isTranslating,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Terjemahkan Dokumen", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Progress indicator
        if (state.isTranslating) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
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
                        trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalButton(
                            onClick = { if (state.isPaused) vm.resume() else vm.pause() },
                            modifier = Modifier.weight(1f).height(40.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(if (state.isPaused) "Lanjutkan" else "Jeda")
                        }
                        OutlinedButton(
                            onClick = vm::cancel,
                            modifier = Modifier.weight(1f).height(40.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Batal")
                        }
                    }
                }
            }
        }

        // Result output
        if (state.output.isNotBlank()) {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Pratinjau Hasil", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    SelectionContainer {
                        Text(
                            text = state.output.take(800) + if (state.output.length > 800) "\n..." else "",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    HorizontalDivider()
                    Button(
                        onClick = {
                            val name = "terjemahan_${fileName?.removeSuffix(".txt") ?: "file"}.txt"
                            (context as? MainActivity)?.exportText(name, state.output)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(10.dp)
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
