@file:OptIn(ExperimentalMaterial3Api::class)

package com.mochi.tl

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Layar penerjemahan teks langsung dan penerjemahan file dokumen.
 */
@Composable
internal fun TextTranslationScreen(vm: MochiViewModel) {
    val state by vm.state.collectAsState()
    val providers by vm.providers.collectAsState()
    val activeProvider by vm.activeProvider.collectAsState()
    val prompts by vm.prompts.collectAsState()
    val activePrompt by vm.activePrompt.collectAsState()
    val projects by vm.projects.collectAsState()
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
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Top Toolbar: Quick Provider & Prompt selectors with improved dropdown cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Provider Dropdown Card
            ExposedDropdownMenuBox(
                expanded = showProviderMenu,
                onExpandedChange = { showProviderMenu = !showProviderMenu },
                modifier = Modifier.weight(1f)
            ) {
                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable, true),
                    onClick = { showProviderMenu = true },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
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
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = activeProvider.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
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

            // Prompt Dropdown Card
            ExposedDropdownMenuBox(
                expanded = showPromptMenu,
                onExpandedChange = { showPromptMenu = !showPromptMenu },
                modifier = Modifier.weight(1f)
            ) {
                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable, true),
                    onClick = { showPromptMenu = true },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
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
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = activePrompt.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
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

        // Language Selector Dropdowns with Swap Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LanguageDropdownField(
                label = "Sumber",
                selectedValue = sourceLanguage,
                options = LanguageOptions.SOURCE_LANGUAGES,
                onValueChange = { sourceLanguage = it },
                modifier = Modifier.weight(1f)
            )

            FilledIconButton(
                onClick = {
                    val temp = sourceLanguage
                    sourceLanguage = targetLanguage
                    targetLanguage = if (temp == LanguageOptions.AUTO_DETECT) {
                        if (sourceLanguage == "Indonesia") "Jepang" else "Indonesia"
                    } else {
                        temp
                    }
                },
                modifier = Modifier.size(44.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Icon(
                    Icons.Default.SwapHoriz,
                    contentDescription = "Tukar Bahasa",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
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

        // Source Text Input Field
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val charCount = state.input.length
                val wordCount = if (state.input.isBlank()) 0 else state.input.trim().split("\\s+".toRegex()).size

                Text(
                    text = "Teks Sumber ($charCount kar / $wordCount kata)",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (state.input.isNotBlank()) {
                        IconButton(
                            onClick = { vm.setInput("") }
                        ) {
                            Icon(
                                Icons.Default.Clear,
                                contentDescription = "Hapus Input",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    IconButton(
                        onClick = {
                            clipboard.primaryClip?.getItemAt(0)?.text?.let { vm.setInput(it.toString()) }
                        }
                    ) {
                        Icon(
                            Icons.Default.ContentPaste,
                            contentDescription = "Tempel Clipboard",
                            tint = MaterialTheme.colorScheme.primary
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
                placeholder = { Text("Tempel atau ketik teks yang ingin diterjemahkan di sini...") }
            )
        }

        // Progress or Error Message
        if (state.error != null) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Error: ${state.error}",
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        if (state.isTranslating) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (state.isPaused) "Penerjemahan dijeda..." else "Menerjemahkan per chunk...",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${(state.progress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                LinearProgressIndicator(
                    progress = { state.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                )
            }
        }

        // Action Controls (Translate, Pause, Cancel) with responsive layout
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (!state.isTranslating) {
                Button(
                    onClick = { vm.translate(sourceLanguage = sourceLanguage, target = targetLanguage) },
                    enabled = state.input.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Terjemahkan Teks", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                FilledTonalButton(
                    onClick = { if (state.isPaused) vm.resume() else vm.pause() },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(if (state.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (state.isPaused) "Lanjutkan" else "Jeda", fontWeight = FontWeight.SemiBold)
                }

                OutlinedButton(
                    onClick = vm::cancel,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Stop, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Batal", fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // Translated Result Output Field
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Hasil Terjemahan",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (state.output.isNotBlank()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(
                            onClick = {
                                clipboard.setPrimaryClip(ClipData.newPlainText("MochiTL", state.output))
                                Toast.makeText(context, "Teks disalin ke clipboard", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Salin Hasil", tint = MaterialTheme.colorScheme.primary)
                        }

                        IconButton(
                            onClick = {
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, state.output)
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Bagikan Terjemahan"))
                            }
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Bagikan Hasil", tint = MaterialTheme.colorScheme.primary)
                        }

                        IconButton(
                            onClick = {
                                (context as? MainActivity)?.exportText("terjemahan_${System.currentTimeMillis()}.txt", state.output)
                            }
                        ) {
                            Icon(Icons.Default.FileDownload, contentDescription = "Simpan File", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            OutlinedTextField(
                value = state.output,
                onValueChange = vm::setOutput,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(12.dp),
                readOnly = false,
                placeholder = { Text("Hasil terjemahan AI akan muncul di sini...") }
            )
        }
    }
}

@Composable
internal fun FileTranslationScreen(vm: MochiViewModel) {
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
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Description, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Penerjemah Dokumen", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Text(
                    text = "Pilih dokumen (TXT, EPUB, DOCX, PDF) untuk diterjemahkan secara otomatis per-chunk dengan context prompt & glossary aktif.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Language Selector Dropdowns with Swap Button for Document Translation
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LanguageDropdownField(
                label = "Sumber",
                selectedValue = sourceLanguage,
                options = LanguageOptions.SOURCE_LANGUAGES,
                onValueChange = { sourceLanguage = it },
                modifier = Modifier.weight(1f)
            )

            FilledIconButton(
                onClick = {
                    val temp = sourceLanguage
                    sourceLanguage = targetLanguage
                    targetLanguage = if (temp == LanguageOptions.AUTO_DETECT) {
                        if (sourceLanguage == "Indonesia") "Jepang" else "Indonesia"
                    } else {
                        temp
                    }
                },
                modifier = Modifier.size(44.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Icon(
                    Icons.Default.SwapHoriz,
                    contentDescription = "Tukar Bahasa",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
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
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.FolderOpen, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (fileName == null) "Buka Dokumen (TXT, EPUB, DOCX, PDF)" else "File: $fileName", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }

        if (fileText.isNotBlank()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Info File Loaded:", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    Text("Ukuran Karakter: ${fileText.length} karakter", style = MaterialTheme.typography.bodyMedium)
                    Text("Estimasi Chunk API: ~${(fileText.length / 4000) + 1} bagian", style = MaterialTheme.typography.bodyMedium)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Text("Pratinjau Teks Asli:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Proses Terjemahkan File", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (state.isTranslating) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Progres Terjemahan File", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                    Text("${(state.progress * 100).toInt()}%", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                LinearProgressIndicator(
                    progress = { state.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilledTonalButton(
                        onClick = { if (state.isPaused) vm.resume() else vm.pause() },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(if (state.isPaused) "Lanjutkan" else "Jeda")
                    }
                    OutlinedButton(
                        onClick = vm::cancel,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Batal")
                    }
                }
            }
        }

        if (state.output.isNotBlank()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Pratinjau Hasil Terjemahan Dokumen:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text(state.output.take(800) + if (state.output.length > 800) "\n..." else "", style = MaterialTheme.typography.bodyMedium)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Button(
                        onClick = {
                            val name = "terjemahan_${fileName?.removeSuffix(".txt") ?: "file"}.txt"
                            (context as? MainActivity)?.exportText(name, state.output)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Simpan File Terjemahan Lengkap", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
