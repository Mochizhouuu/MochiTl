package com.mochi.tl

import android.app.Application
import android.content.ContentValues
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader

class MainActivity : ComponentActivity() {
    private val vm: MochiViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MochiTheme { MochiApp(vm) } }
    }

    fun exportText(name: String, content: String) {
        // MediaStore.Downloads only exists on API 29+ (Android 10). minSdk
        // for this app is 26, so devices on Android 8/9 must fall back to a
        // legacy direct-file write into the public Downloads directory, or
        // this crashes with a runtime ClassNotFoundException/NoSuchFieldError
        // the moment this code path is hit.
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, name)
                put(MediaStore.Downloads.MIME_TYPE, "text/plain")
                put(MediaStore.Downloads.RELATIVE_PATH, "Download/MochiTL")
            }
            contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)?.let { uri: Uri ->
                contentResolver.openOutputStream(uri)?.use { it.write(content.toByteArray()) }
            }
        } else {
            @Suppress("DEPRECATION")
            val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(
                android.os.Environment.DIRECTORY_DOWNLOADS
            )
            val targetDir = java.io.File(downloadsDir, "MochiTL").apply { mkdirs() }
            java.io.File(targetDir, name).writeText(content)
        }
    }
}

@Composable
fun MochiTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = androidx.compose.material3.lightColorScheme(primary = Color(0xFFC65F7A), background = Color(0xFFFFF8FA)), content = content)
}

private enum class Screen { HOME, TEXT, FILE, PROJECTS, PROMPTS, GLOSSARY, HISTORY, SETTINGS, ABOUT }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MochiApp(vm: MochiViewModel) {
    var screen by remember { mutableStateOf(Screen.HOME) }
    val drawer = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    ModalNavigationDrawer(
        drawerState = drawer,
        drawerContent = {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(8.dp)) {
                    Image(painterResource(com.mochi.tl.R.drawable.mochitl_mascot), "MochiTL", Modifier.size(48.dp))
                    Text("MochiTL", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(12.dp))
                }
                HorizontalDivider()
                listOf(Screen.HOME to "Beranda", Screen.TEXT to "Terjemahkan Teks", Screen.FILE to "Terjemahkan File", Screen.PROJECTS to "Proyek", Screen.PROMPTS to "Pengelola Prompt", Screen.GLOSSARY to "Glosarium", Screen.HISTORY to "Riwayat", Screen.SETTINGS to "Pengaturan", Screen.ABOUT to "Dokumentasi").forEach { (target, label) ->
                    NavigationDrawerItem(label = { Text(label) }, selected = screen == target, onClick = { screen = target; scope.launch { drawer.close() } }, icon = { Icon(iconFor(target), null) })
                }
            }
        }
    ) {
        Scaffold(topBar = { TopAppBar(title = { Text(screenTitle(screen)) }, navigationIcon = { IconButton(onClick = { scope.launch { drawer.open() } }) { Icon(Icons.Default.Home, "Menu") } }) }) { padding ->
            Surface(Modifier.fillMaxSize().padding(padding)) {
                when (screen) {
                    Screen.HOME -> HomeScreen { screen = it }
                    Screen.TEXT -> TextTranslationScreen(vm)
                    Screen.FILE -> FileTranslationScreen(vm)
                    Screen.PROJECTS -> ProjectsScreen(vm)
                    Screen.PROMPTS -> PromptScreen(vm)
                    Screen.GLOSSARY -> GlossaryScreen()
                    Screen.HISTORY -> HistoryScreen(vm)
                    Screen.SETTINGS -> SettingsScreen(vm)
                    Screen.ABOUT -> DocumentationScreen()
                }
            }
        }
    }
}

private fun iconFor(screen: Screen) = when (screen) { Screen.TEXT -> Icons.Default.Language; Screen.FILE -> Icons.Default.Description; Screen.PROJECTS -> Icons.Default.Book; Screen.PROMPTS -> Icons.Default.Send; Screen.GLOSSARY -> Icons.Default.Book; Screen.HISTORY -> Icons.Default.History; Screen.SETTINGS -> Icons.Default.Settings; Screen.ABOUT -> Icons.Default.Info; else -> Icons.Default.Home }
private fun screenTitle(screen: Screen) = when (screen) { Screen.HOME -> "MochiTL"; Screen.TEXT -> "Terjemahkan Teks"; Screen.FILE -> "Terjemahkan File"; Screen.PROJECTS -> "Proyek"; Screen.PROMPTS -> "Pengelola Prompt"; Screen.GLOSSARY -> "Glosarium"; Screen.HISTORY -> "Riwayat"; Screen.SETTINGS -> "Pengaturan"; Screen.ABOUT -> "Dokumentasi MochiTL" }

@Composable
private fun HomeScreen(navigate: (Screen) -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Image(painterResource(R.drawable.mochitl_mascot), "MochiTL mascot", Modifier.size(86.dp).align(Alignment.CenterHorizontally))
        Text("Ruang kerja terjemahan AI", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Atur provider, prompt, glosarium, dan proyek sebelum menerjemahkan teks atau file.")
        Button(onClick = { navigate(Screen.TEXT) }, Modifier.fillMaxWidth()) { Icon(Icons.Default.Language, null); Text("  Mulai terjemahkan teks") }
        OutlinedButton(onClick = { navigate(Screen.FILE) }, Modifier.fillMaxWidth()) { Icon(Icons.Default.Description, null); Text("  Terjemahkan file") }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedButton({ navigate(Screen.PROJECTS) }, Modifier.weight(1f)) { Text("Proyek") }; OutlinedButton({ navigate(Screen.SETTINGS) }, Modifier.weight(1f)) { Text("Provider") } }
    }
}

@Composable
private fun TextTranslationScreen(vm: MochiViewModel) {
    val state by vm.state.collectAsState(); val context = LocalContext.current; var sourceLanguage by remember { mutableStateOf("Deteksi Otomatis") }; var targetLanguage by remember { mutableStateOf("Indonesia") }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedTextField(sourceLanguage, { sourceLanguage = it }, Modifier.weight(1f), label = { Text("Sumber") }); OutlinedTextField(targetLanguage, { targetLanguage = it }, Modifier.weight(1f), label = { Text("Target") }) }
        OutlinedTextField(state.input, vm::setInput, Modifier.fillMaxWidth().weight(1f), label = { Text("Teks sumber") }, placeholder = { Text("Tempel teks yang ingin diterjemahkan") })
        if (state.error != null) Text(state.error!!, color = MaterialTheme.colorScheme.error)
        if (state.isTranslating) LinearProgressIndicator({ state.progress }, Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Button(onClick = { vm.translate(target = targetLanguage) }, enabled = state.input.isNotBlank() && !state.isTranslating) { Icon(Icons.Default.PlayArrow, null); Text(" Terjemahkan") }; if (state.isTranslating) { IconButton({ if (state.isPaused) vm.resume() else vm.pause() }) { Icon(if (state.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause, "Jeda") }; IconButton(vm::cancel) { Icon(Icons.Default.Stop, "Batal") } } }
        OutlinedTextField(state.output, {}, Modifier.fillMaxWidth().weight(1f), readOnly = true, label = { Text("Hasil") })
        Button(onClick = { (context as? MainActivity)?.exportText("mochitl-translation.txt", state.output) }, enabled = state.output.isNotBlank()) { Text("Simpan ke Download/MochiTL") }
    }
}

@Composable
private fun FileTranslationScreen(vm: MochiViewModel) {
    var fileName by remember { mutableStateOf<String?>(null) }; var fileText by remember { mutableStateOf("") }; val state by vm.state.collectAsState()
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let { fileName = it.lastPathSegment; fileText = FileParser.readText(context, it) } }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) { Text("Alur file", fontWeight = FontWeight.Bold); Text("TXT → parsing → chunk translation → preview → export TXT. Format lain tidak diiklankan sebelum parser native-nya ditambahkan.") } }
        Button({ picker.launch(arrayOf("text/plain")) }) { Text(fileName ?: "Pilih file") }
        Text("Prompt file mengikuti Prompt proyek. Atur melalui Proyek → Pengelola Prompt.")
        if (fileText.isNotBlank()) Button({ vm.translate(source = fileText) }) { Text("Mulai terjemahkan file") }
        if (state.isTranslating) { LinearProgressIndicator({ state.progress }, Modifier.fillMaxWidth()); Row { OutlinedButton({ if (state.isPaused) vm.resume() else vm.pause() }) { Text(if (state.isPaused) "Lanjutkan" else "Jeda") }; OutlinedButton(vm::cancel) { Text("Batal") } } }
        if (state.output.isNotBlank()) Text("Preview hasil:\n${state.output.take(1200)}")
    }
}

@Composable
private fun ProjectsScreen(vm: MochiViewModel) {
    val projects by vm.projects.collectAsState()
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Project menghubungkan prompt, glossary, provider, model, translation, history, dan file.")
        projects.forEach { project -> Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) { Text(project.name, fontWeight = FontWeight.Bold); Text("Prompt: ${project.promptTemplateId.ifBlank { "Default Prompt MochiTL" }}"); Text("Provider: ${project.providerId} • Model: ${project.modelId}") } } }
        Button(onClick = { vm.saveProject(TranslationProject(java.util.UUID.randomUUID().toString(), "Proyek baru", promptTemplateId = BuiltIns.defaultPrompt.id)) }) { Icon(Icons.Default.Add, null); Text("  Tambah proyek") }
    }
}

@Composable
private fun PromptScreen(vm: MochiViewModel) {
    val prompts by vm.prompts.collectAsState()
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Prompt Manager menyimpan instruksi AI. Pilih prompt di Proyek agar berlaku untuk teks dan file.")
        prompts.forEach { prompt -> Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) { Text(prompt.name, fontWeight = FontWeight.Bold); Text(prompt.description); Text(prompt.content.take(180)) } } }
    }
}

@Composable
private fun GlossaryScreen() {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { Text("Glosarium", style = MaterialTheme.typography.headlineSmall); Text("Simpan pasangan istilah sumber → target dan gunakan sebagai konteks AI di proyek."); OutlinedTextField("", {}, Modifier.fillMaxWidth(), label = { Text("Istilah sumber") }); OutlinedTextField("", {}, Modifier.fillMaxWidth(), label = { Text("Terjemahan istilah") }); Button({}) { Icon(Icons.Default.Add, null); Text("  Tambah istilah") } }
}

@Composable
private fun HistoryScreen(vm: MochiViewModel) {
    val history by vm.history.collectAsState()
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { if (history.isEmpty()) item { Text("Belum ada riwayat. Aktifkan simpan otomatis di Pengaturan.") }; items(history) { item -> Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp)) { Text(item.sourcePreview); Text(item.translatedText.take(180)); Text("${item.providerId} • ${item.targetLanguage}") } } } }
}

@Composable
private fun SettingsScreen(vm: MochiViewModel) {
    val providers by vm.providers.collectAsState(); var key by remember { mutableStateOf(vm.apiKey.orEmpty()) }; var selected by remember { mutableStateOf(vm.activeProvider.value) }; var autoSave by remember { mutableStateOf(vm.autoSave()) }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Pengaturan AI", style = MaterialTheme.typography.headlineSmall)
        providers.forEach { provider -> OutlinedButton(onClick = { selected = provider; vm.selectProvider(provider); key = vm.apiKey.orEmpty() }, Modifier.fillMaxWidth()) { Text(if (selected.id == provider.id) "✓ ${provider.name}" else provider.name) } }
        OutlinedTextField(key, { key = it; vm.apiKey = it }, Modifier.fillMaxWidth(), label = { Text("API key (tersimpan terenkripsi)") })
        Text("OpenAI-compatible: base URL server tanpa /v1; MochiTL menggunakan /v1/models dan /v1/chat/completions.")
        OutlinedButton(onClick = { autoSave = !autoSave; vm.setAutoSave(autoSave) }) { Text(if (autoSave) "✓ Simpan riwayat otomatis" else "Simpan riwayat otomatis") }
        Text("API key tidak dicatat di source code dan tidak termasuk dalam arsip sumber.")
    }
}

@Composable
private fun DocumentationScreen() {
    val topics = listOf("Memulai" to "Atur provider dan API key, buat prompt, buat glossary, lalu pilih semuanya di Proyek.", "Terjemahkan Teks" to "Terjemahan chunked dengan prompt dan konteks glossary.", "Terjemahkan File" to "TXT, PDF, EPUB, dan DOCX diproses melalui parsing, chunk translation, preview, lalu export TXT/EPUB.", "Provider OpenAI-compatible" to "Masukkan base URL tanpa /v1 dan model yang didukung server.", "OCR Cleanup" to "Hanya membersihkan teks OCR; tidak membaca gambar dan tidak mengklaim OCR gambar.", "Keamanan" to "API key disimpan melalui EncryptedSharedPreferences berbasis Android Keystore.")
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { item { Text("Pusat Dokumentasi", style = MaterialTheme.typography.headlineSmall) }; items(topics) { (title, body) -> Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) { Text(title, fontWeight = FontWeight.Bold); Text(body) } } } }
}
