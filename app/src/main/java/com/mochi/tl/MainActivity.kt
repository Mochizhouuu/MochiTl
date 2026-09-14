package com.mochi.tl

import android.content.ContentValues
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mochi.tl.designsystem.MochiAppTheme
import com.mochi.tl.designsystem.components.*
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val vm: MochiViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var isDarkTheme by remember { mutableStateOf(true) }

            MochiAppTheme(darkTheme = isDarkTheme) {
                LaunchedEffect(Unit) {
                    vm.loadInitialData()
                }
                MochiApp(
                    vm = vm,
                    isDarkTheme = isDarkTheme,
                    onToggleTheme = { isDarkTheme = !isDarkTheme }
                )
            }
        }
    }

    fun exportText(name: String, content: String) {
        runCatching {
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
            Toast.makeText(this, "File disimpan di Download/MochiTL/$name", Toast.LENGTH_LONG).show()
        }.onFailure {
            Toast.makeText(this, "Gagal menyimpan file: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }
}

internal enum class Screen { HOME, TEXT, FILE, PROJECTS, PROMPTS, GLOSSARY, HISTORY, SETTINGS, ABOUT }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MochiApp(
    vm: MochiViewModel,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit
) {
    var screen by remember { mutableStateOf(Screen.HOME) }
    var docPageTarget by remember { mutableIntStateOf(0) }
    val activeProject by vm.activeProject.collectAsState()
    val context = LocalContext.current
    var backPressedTime by remember { mutableLongStateOf(0L) }

    BackHandler(enabled = true) {
        if (screen != Screen.HOME) {
            screen = Screen.HOME
        } else {
            val currentTime = System.currentTimeMillis()
            if (currentTime - backPressedTime < 2000) {
                (context as? android.app.Activity)?.finish()
            } else {
                backPressedTime = currentTime
                Toast.makeText(context, "Tekan sekali lagi untuk keluar", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        topBar = {
            MochiTopAppBar(
                title = {
                    Column {
                        Text(
                            text = screenTitle(screen),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                        if (activeProject != null && screen != Screen.HOME) {
                            Text(
                                text = "Proyek: ${activeProject!!.name}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onToggleTheme) {
                        Icon(
                            if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Toggle Theme",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        },
        bottomBar = {
            MochiNavigationBar {
                MochiNavigationBarItem(
                    selected = screen == Screen.HOME || screen == Screen.TEXT || screen == Screen.FILE,
                    onClick = { screen = Screen.TEXT },
                    icon = { Icon(Icons.Default.Translate, contentDescription = "Terjemahkan") },
                    label = { Text("Terjemah", style = MaterialTheme.typography.labelMedium) }
                )
                MochiNavigationBarItem(
                    selected = screen == Screen.PROJECTS,
                    onClick = { screen = Screen.PROJECTS },
                    icon = { Icon(Icons.Default.Book, contentDescription = "Proyek") },
                    label = { Text("Proyek", style = MaterialTheme.typography.labelMedium) }
                )
                MochiNavigationBarItem(
                    selected = screen == Screen.PROMPTS,
                    onClick = { screen = Screen.PROMPTS },
                    icon = { Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Prompt") },
                    label = { Text("Prompt", style = MaterialTheme.typography.labelMedium) }
                )
                MochiNavigationBarItem(
                    selected = screen == Screen.GLOSSARY,
                    onClick = { screen = Screen.GLOSSARY },
                    icon = { Icon(Icons.AutoMirrored.Filled.Comment, contentDescription = "Glosarium") },
                    label = { Text("Glosarium", style = MaterialTheme.typography.labelMedium) }
                )
                MochiNavigationBarItem(
                    selected = screen == Screen.SETTINGS,
                    onClick = { screen = Screen.SETTINGS },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Pengaturan") },
                    label = { Text("Setelan", style = MaterialTheme.typography.labelMedium) }
                )
            }
        }
    ) { padding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            color = MaterialTheme.colorScheme.background
        ) {
            AnimatedContent(
                targetState = screen,
                transitionSpec = {
                    (fadeIn() + slideInVertically { 20 }).togetherWith(fadeOut() + slideOutVertically { -20 })
                },
                label = "screenTransition"
            ) { targetScreen ->
                when (targetScreen) {
                    Screen.HOME, Screen.TEXT -> TextTranslationScreen(vm, onSwitchToFile = { screen = Screen.FILE })
                    Screen.FILE -> FileTranslationScreen(vm, onSwitchToText = { screen = Screen.TEXT })
                    Screen.PROJECTS -> ProjectsScreen(vm)
                    Screen.PROMPTS -> PromptScreen(
                        vm = vm,
                        onNavigateToGlossary = { screen = Screen.GLOSSARY },
                        onNavigateToDocumentation = { page ->
                            docPageTarget = page
                            screen = Screen.ABOUT
                        }
                    )
                    Screen.GLOSSARY -> GlossaryScreen(
                        vm = vm,
                        onNavigateToPrompts = { screen = Screen.PROMPTS }
                    )
                    Screen.HISTORY -> HistoryScreen(vm, onSelectHistoryItem = { text ->
                        vm.setInput(text)
                        screen = Screen.TEXT
                    })
                    Screen.SETTINGS -> SettingsScreen(
                        vm = vm,
                        isDarkTheme = isDarkTheme,
                        onToggleTheme = onToggleTheme
                    )
                    Screen.ABOUT -> DocumentationScreen(initialPage = docPageTarget)
                }
            }
        }
    }
}

@Composable
internal fun ProjectsScreen(vm: MochiViewModel) {
    val projects by vm.projects.collectAsState()
    val activeProject by vm.activeProject.collectAsState()
    val prompts by vm.prompts.collectAsState()
    val providers by vm.providers.collectAsState()
    val glossaryList by vm.glossary.collectAsState()

    var showDialog by remember { mutableStateOf(false) }
    var editingProject by remember { mutableStateOf<TranslationProject?>(null) }

    ProjectsScreenContent(
        projects = projects,
        activeProject = activeProject,
        prompts = prompts,
        providers = providers,
        glossaryList = glossaryList,
        onAddProject = { editingProject = null; showDialog = true },
        onEditProject = { proj -> editingProject = proj; showDialog = true },
        onSelectProject = vm::selectProject,
        onDeleteProject = vm::deleteProject
    )

    if (showDialog) {
        ProjectEditDialog(
            project = editingProject,
            prompts = prompts,
            providers = providers,
            glossaryList = glossaryList,
            onDismiss = { showDialog = false },
            onSave = { newProj ->
                vm.saveProject(newProj)
                showDialog = false
            }
        )
    }
}

@Composable
internal fun ProjectsScreenContent(
    projects: List<TranslationProject>,
    activeProject: TranslationProject?,
    prompts: List<PromptTemplate>,
    providers: List<ProviderConfig>,
    glossaryList: List<GlossaryEntry>,
    onAddProject: () -> Unit,
    onEditProject: (TranslationProject) -> Unit,
    onSelectProject: (TranslationProject?) -> Unit,
    onDeleteProject: (String) -> Unit
) {
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
                text = "Daftar Proyek",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.width(8.dp))
            MochiOutlinedButton(
                onClick = onAddProject
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Tambah Proyek", fontWeight = FontWeight.SemiBold)
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (projects.isEmpty()) {
                item {
                    MochiEmptyState(
                        icon = Icons.Default.Book,
                        title = "Belum Ada Proyek",
                        description = "Belum ada proyek dibuat. Klik Tambah Proyek untuk membuat baru.",
                        action = {
                            MochiButton(onClick = onAddProject) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Tambah Proyek")
                            }
                        }
                    )
                }
            }

            items(projects) { proj ->
                val isActive = activeProject?.id == proj.id
                val promptName = prompts.find { it.id == proj.promptTemplateId }?.name ?: "Default Prompt"
                val providerName = providers.find { it.id == proj.providerId }?.name ?: proj.providerId

                MochiCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = proj.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (isActive) {
                                Spacer(modifier = Modifier.width(8.dp))
                                MochiChip(
                                    text = "AKTIF",
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        if (proj.description.isNotBlank()) {
                            Text(proj.description, style = MaterialTheme.typography.bodyMedium)
                        }

                        val glossaryInfo = if (proj.glossaryIds.isEmpty()) "Semua (${glossaryList.size})" else "${proj.glossaryIds.size} terikat"
                        Text("Prompt: $promptName • Provider: $providerName • Target: ${proj.targetLanguage} • Glosarium: $glossaryInfo", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, softWrap = true)

                        HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (!isActive) {
                                MochiOutlinedButton(
                                    onClick = { onSelectProject(proj) },
                                    modifier = Modifier.height(36.dp)
                                ) { Text("Aktifkan Proyek") }
                            } else {
                                MochiOutlinedButton(
                                    onClick = { onSelectProject(null) },
                                    modifier = Modifier.height(36.dp)
                                ) { Text("Nonaktifkan") }
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = { onEditProject(proj) }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit Proyek")
                                }
                                IconButton(onClick = { onDeleteProject(proj.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Hapus Proyek", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PromptScreen(
    vm: MochiViewModel,
    onNavigateToGlossary: () -> Unit = {},
    onNavigateToDocumentation: (Int) -> Unit = {}
) {
    val prompts by vm.prompts.collectAsState()
    val context = LocalContext.current
    var showEditDialog by remember { mutableStateOf(false) }
    var editingPrompt by remember { mutableStateOf<PromptTemplate?>(null) }
    var viewingSamplePrompt by remember { mutableStateOf<PromptTemplate?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredPrompts = remember(prompts, searchQuery) {
        val q = searchQuery.trim()
        if (q.isEmpty()) prompts else prompts.filter {
            it.name.contains(q, true) || it.description.contains(q, true) || it.content.contains(q, true)
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let { destinationUri ->
            runCatching {
                val json = vm.exportPromptsJson()
                context.contentResolver.openOutputStream(destinationUri)?.use { out ->
                    out.write(json.toByteArray())
                }
                Toast.makeText(context, "Prompt berhasil diekspor", Toast.LENGTH_SHORT).show()
            }.onFailure {
                Toast.makeText(context, "Gagal mengekspor: ${it.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { sourceUri ->
            runCatching {
                val content = context.contentResolver.openInputStream(sourceUri)?.use { input ->
                    input.bufferedReader().readText()
                }.orEmpty()
                val result = vm.importPromptsJson(content)
                if (result.isSuccess) {
                    Toast.makeText(context, "Berhasil mengimpor ${result.getOrDefault(0)} prompt", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "Gagal mengimpor: ${result.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                }
            }.onFailure {
                Toast.makeText(context, "Gagal membaca file: ${it.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

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
                text = "Pengelola Prompt",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.width(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MochiOutlinedButton(onClick = { vm.resetPromptsToDefault() }) {
                    Text("Reset")
                }
                MochiOutlinedButton(onClick = { editingPrompt = null; showEditDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Tambah", fontWeight = FontWeight.SemiBold)
                }
            }
        }

        MochiTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Cari prompt...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MochiOutlinedButton(
                onClick = { exportLauncher.launch("prompt_mochitl.json") },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Ekspor JSON", fontSize = 13.sp)
            }

            MochiOutlinedButton(
                onClick = { importLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Impor JSON", fontSize = 13.sp)
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (filteredPrompts.isEmpty()) {
                item {
                    MochiEmptyState(
                        icon = Icons.AutoMirrored.Filled.Send,
                        title = "Tidak Ada Prompt",
                        description = if (searchQuery.isBlank()) "Belum ada prompt. Tambahkan atau tekan Reset untuk memulai." else "Tidak ada prompt yang cocok dengan pencarian."
                    )
                }
            }
            items(filteredPrompts) { p ->
                MochiCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = p.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            MochiChip(text = if (p.isBuiltIn) "Built-in" else "Kustom")
                        }

                        Text(p.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3, overflow = TextOverflow.Ellipsis)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        Text(p.content, style = MaterialTheme.typography.bodyMedium, maxLines = 4, overflow = TextOverflow.Ellipsis)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            MochiGhostButton(onClick = { viewingSamplePrompt = p }) {
                                Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Lihat Detail Template", fontSize = 12.sp)
                            }

                            if (!p.isBuiltIn) {
                                Row {
                                    IconButton(onClick = { vm.duplicatePrompt(p.id) }) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = "Duplikat Prompt")
                                    }
                                    IconButton(onClick = { editingPrompt = p; showEditDialog = true }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit Prompt")
                                    }
                                    IconButton(onClick = { vm.deletePrompt(p.id) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Hapus Prompt", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            } else {
                                IconButton(onClick = { vm.duplicatePrompt(p.id) }) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Duplikat sebagai kustom")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (viewingSamplePrompt != null) {
        PromptSampleViewerDialog(
            prompt = viewingSamplePrompt!!,
            onDismiss = { viewingSamplePrompt = null }
        )
    }

    if (showEditDialog) {
        PromptEditDialog(
            prompt = editingPrompt,
            onDismiss = { showEditDialog = false },
            onNavigateToDocumentation = onNavigateToDocumentation,
            onSave = { newP ->
                vm.savePrompt(newP)
                showEditDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GlossaryScreen(
    vm: MochiViewModel,
    onNavigateToPrompts: () -> Unit = {}
) {
    val glossaryList by vm.glossary.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var showDialog by remember { mutableStateOf(false) }
    var editingEntry by remember { mutableStateOf<GlossaryEntry?>(null) }
    val context = LocalContext.current

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let { destinationUri ->
            runCatching {
                val json = vm.exportGlossaryJson()
                context.contentResolver.openOutputStream(destinationUri)?.use { out ->
                    out.write(json.toByteArray())
                }
                Toast.makeText(context, "Glosarium berhasil diekspor", Toast.LENGTH_SHORT).show()
            }.onFailure {
                Toast.makeText(context, "Gagal mengekspor: ${it.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { sourceUri ->
            runCatching {
                val content = context.contentResolver.openInputStream(sourceUri)?.use { input ->
                    input.bufferedReader().readText()
                }.orEmpty()
                val result = vm.importGlossaryJson(content)
                if (result.isSuccess) {
                    val count = result.getOrDefault(0)
                    Toast.makeText(context, "Berhasil mengimpor $count istilah glosarium", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "Gagal mengimpor: ${result.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                }
            }.onFailure {
                Toast.makeText(context, "Gagal membaca file: ${it.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val filtered = remember(glossaryList, searchQuery) {
        val q = searchQuery.trim()
        glossaryList
            .filter {
                q.isEmpty() || it.source.contains(q, true) ||
                        it.target.contains(q, true) || it.note.contains(q, true)
            }
            .sortedWith(compareBy({ it.source.lowercase() }, { it.target.lowercase() }))
    }

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
                text = "Glosarium Istilah",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.width(8.dp))
            MochiOutlinedButton(onClick = { editingEntry = null; showDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Tambah Istilah", fontWeight = FontWeight.SemiBold)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MochiOutlinedButton(
                onClick = { exportLauncher.launch("glosarium_mochitl.json") },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Ekspor JSON", fontSize = 13.sp)
            }

            MochiOutlinedButton(
                onClick = { importLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Impor JSON", fontSize = 13.sp)
            }
        }

        MochiTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Cari istilah...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "Bersihkan pencarian")
                    }
                }
            },
            singleLine = true
        )

        Text(
            text = if (searchQuery.isBlank()) "${glossaryList.size} istilah • diurutkan abjad"
            else "${filtered.size} dari ${glossaryList.size} istilah",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (filtered.isEmpty()) {
                item {
                    MochiEmptyState(
                        icon = Icons.AutoMirrored.Filled.Comment,
                        title = "Tidak Ada Istilah",
                        description = if (glossaryList.isEmpty()) "Belum ada istilah glosarium. Tambahkan istilah nama karakter, jurus, atau tempat." else "Tidak ada istilah yang cocok dengan \"$searchQuery\"."
                    )
                }
            }

            items(filtered) { entry ->
                MochiCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = entry.source,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyLarge,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                Text(" ➔ ", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                Text(
                                    text = entry.target,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.bodyLarge,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                            }
                            if (entry.note.isNotBlank()) {
                                Text(
                                    text = "Catatan: ${entry.note}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Row {
                            IconButton(onClick = { editingEntry = entry; showDialog = true }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit Istilah")
                            }
                            IconButton(onClick = { vm.deleteGlossaryItem(entry.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Hapus Istilah", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        GlossaryEditDialog(
            entry = editingEntry,
            onDismiss = { showDialog = false },
            onSave = { newEntry ->
                vm.saveGlossaryItem(newEntry)
                showDialog = false
            }
        )
    }
}

private fun screenTitle(screen: Screen) = when (screen) {
    Screen.HOME -> "MochiTL Workspace"
    Screen.TEXT -> "Terjemahkan Teks"
    Screen.FILE -> "Terjemahkan File"
    Screen.PROJECTS -> "Proyek Terjemahan"
    Screen.PROMPTS -> "Pengelola Prompt"
    Screen.GLOSSARY -> "Glosarium Istilah"
    Screen.HISTORY -> "Riwayat Terjemahan"
    Screen.SETTINGS -> "Pengaturan Provider AI"
    Screen.ABOUT -> "Dokumentasi & Panduan"
}
