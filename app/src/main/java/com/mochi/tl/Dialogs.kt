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
 * Kumpulan dialog edit (Project, Prompt, Glossary).
 * Diekstrak dari MainActivity.kt sebagai bagian dari pemisahan UI.
 */
@Composable
internal fun ProjectEditDialog(
    project: TranslationProject?,
    prompts: List<PromptTemplate>,
    providers: List<ProviderConfig>,
    glossaryList: List<GlossaryEntry>,
    onDismiss: () -> Unit,
    onSave: (TranslationProject) -> Unit
) {
    var name by remember { mutableStateOf(project?.name.orEmpty()) }
    var description by remember { mutableStateOf(project?.description.orEmpty()) }
    var selectedPromptId by remember { mutableStateOf(project?.promptTemplateId ?: prompts.firstOrNull()?.id.orEmpty()) }
    var selectedProviderId by remember { mutableStateOf(project?.providerId ?: providers.firstOrNull()?.id.orEmpty()) }
    var selectedGlossaryIds by remember { mutableStateOf(project?.glossaryIds?.toSet() ?: emptySet()) }
    var targetLang by remember { mutableStateOf(project?.targetLanguage ?: "Indonesia") }

    var showPromptDropdown by remember { mutableStateOf(false) }
    var showProviderDropdown by remember { mutableStateOf(false) }

    val currentPromptName = prompts.find { it.id == selectedPromptId }?.name ?: "Pilih Prompt"
    val currentProviderName = providers.find { it.id == selectedProviderId }?.name ?: "Pilih Provider"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (project == null) "Tambah Proyek Baru" else "Edit Proyek") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Proyek") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Deskripsi / Catatan Proyek") },
                    modifier = Modifier.fillMaxWidth()
                )

                LanguageDropdownField(
                    label = "Bahasa Target Utama",
                    selectedValue = targetLang,
                    options = LanguageOptions.TARGET_LANGUAGES,
                    onValueChange = { targetLang = it },
                    modifier = Modifier.fillMaxWidth()
                )

                // Prompt Selector
                Text("Prompt Template Proyek:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                ExposedDropdownMenuBox(
                    expanded = showPromptDropdown,
                    onExpandedChange = { showPromptDropdown = !showPromptDropdown },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = currentPromptName,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showPromptDropdown) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = showPromptDropdown,
                        onDismissRequest = { showPromptDropdown = false }
                    ) {
                        prompts.forEach { p ->
                            DropdownMenuItem(
                                text = { Text(p.name) },
                                onClick = {
                                    selectedPromptId = p.id
                                    showPromptDropdown = false
                                }
                            )
                        }
                    }
                }

                // Provider Selector
                Text("Provider AI Proyek:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                ExposedDropdownMenuBox(
                    expanded = showProviderDropdown,
                    onExpandedChange = { showProviderDropdown = !showProviderDropdown },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = currentProviderName,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showProviderDropdown) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = showProviderDropdown,
                        onDismissRequest = { showProviderDropdown = false }
                    ) {
                        providers.forEach { prov ->
                            DropdownMenuItem(
                                text = { Text(prov.name) },
                                onClick = {
                                    selectedProviderId = prov.id
                                    showProviderDropdown = false
                                }
                            )
                        }
                    }
                }

                // Glossary Selection Checklist
                Text("Tautkan Glosarium Spesifik:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                if (glossaryList.isEmpty()) {
                    Text("Belum ada glosarium. Semua istilah umum akan digunakan.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            glossaryList.forEach { item ->
                                val isChecked = selectedGlossaryIds.contains(item.id)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp)
                                ) {
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = { checked ->
                                            selectedGlossaryIds = if (checked) {
                                                selectedGlossaryIds + item.id
                                            } else {
                                                selectedGlossaryIds - item.id
                                            }
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${item.source} ➔ ${item.target}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val proj = TranslationProject(
                            id = project?.id ?: java.util.UUID.randomUUID().toString(),
                            name = name,
                            description = description,
                            promptTemplateId = selectedPromptId,
                            providerId = selectedProviderId,
                            glossaryIds = selectedGlossaryIds.toList(),
                            targetLanguage = targetLang
                        )
                        onSave(proj)
                    }
                }
            ) { Text("Simpan") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}

@Composable
internal fun PromptSampleViewerDialog(
    prompt: PromptTemplate,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboard = remember { context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(text = "Referensi Template: ${prompt.name}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (prompt.description.isNotBlank()) {
                    Text(text = prompt.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = prompt.content,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("Tutup") }
        },
        dismissButton = {
            OutlinedButton(
                onClick = {
                    clipboard.setPrimaryClip(ClipData.newPlainText("Prompt Reference", prompt.content))
                    Toast.makeText(context, "Teks prompt disalin ke clipboard", Toast.LENGTH_SHORT).show()
                }
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Salin Teks")
            }
        }
    )
}

@Composable
internal fun PromptEditDialog(
    prompt: PromptTemplate?,
    onDismiss: () -> Unit,
    onNavigateToDocumentation: (Int) -> Unit,
    onSave: (PromptTemplate) -> Unit
) {
    var name by remember { mutableStateOf(prompt?.name.orEmpty()) }
    var description by remember { mutableStateOf(prompt?.description.orEmpty()) }
    var contentValue by remember {
        mutableStateOf(androidx.compose.ui.text.input.TextFieldValue(text = prompt?.content.orEmpty()))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (prompt == null) "Tambah Prompt Baru" else "Edit Prompt") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Prompt") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Deskripsi Singkat") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Aturan Gaya & Preferensi:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)

                OutlinedTextField(
                    value = contentValue,
                    onValueChange = { contentValue = it },
                    label = { Text("Aturan Gaya Terjemahan") },
                    placeholder = { Text("Contoh: gunakan bahasa gaul, pertahankan honorifik Jepang, jangan terjemahkan nama tempat...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 5
                )

                // Brief caption and documentation link
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Tulis aturan gaya terjemahan yang Anda inginkan di sini, seperti tingkat formalitas, penanganan istilah khusus, atau nada bicara. Bahasa sumber, bahasa target, dan format teks akan diatur otomatis oleh sistem.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(
                        onClick = {
                            onDismiss()
                            onNavigateToDocumentation(3)
                        },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = "Lihat panduan lengkap",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && contentValue.text.isNotBlank()) {
                        val p = PromptTemplate(
                            id = prompt?.id ?: java.util.UUID.randomUUID().toString(),
                            name = name,
                            content = contentValue.text,
                            category = prompt?.category ?: "custom",
                            description = description,
                            isBuiltIn = prompt?.isBuiltIn ?: false
                        )
                        onSave(p)
                    }
                },
                enabled = name.isNotBlank() && contentValue.text.isNotBlank()
            ) { Text("Simpan") }
        },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("Batal") } }
    )
}

@Composable
internal fun GlossaryEditDialog(
    entry: GlossaryEntry?,
    onDismiss: () -> Unit,
    onSave: (GlossaryEntry) -> Unit
) {
    var source by remember { mutableStateOf(entry?.source.orEmpty()) }
    var target by remember { mutableStateOf(entry?.target.orEmpty()) }
    var note by remember { mutableStateOf(entry?.note.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (entry == null) "Tambah Istilah Glosarium" else "Edit Istilah") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = source,
                    onValueChange = { source = it },
                    label = { Text("Teks / Istilah Asli") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = target,
                    onValueChange = { target = it },
                    label = { Text("Terjemahan Baku") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Catatan Context (Opsional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (source.isNotBlank() && target.isNotBlank()) {
                        val item = GlossaryEntry(
                            id = entry?.id ?: java.util.UUID.randomUUID().toString(),
                            source = source.trim(),
                            target = target.trim(),
                            note = note.trim()
                        )
                        onSave(item)
                    }
                },
                enabled = source.isNotBlank() && target.isNotBlank()
            ) { Text("Simpan") }
        },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("Batal") } }
    )
}
