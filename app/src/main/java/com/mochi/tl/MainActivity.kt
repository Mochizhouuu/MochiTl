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

class MainActivity : ComponentActivity() {
    private val vm: MochiViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var isDarkTheme by remember { mutableStateOf(false) }
            MochiAppTheme(darkTheme = isDarkTheme) {
                MochiApp(vm, isDarkTheme = isDarkTheme, onToggleTheme = { isDarkTheme = !isDarkTheme })
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
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val activeProject by vm.activeProject.collectAsState()
    val context = LocalContext.current
    var backPressedTime by remember { mutableLongStateOf(0L) }

    BackHandler(enabled = true) {
        if (drawerState.isOpen) {
            scope.launch { drawerState.close() }
        } else if (screen != Screen.HOME) {
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

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        Image(
                            painter = painterResource(R.drawable.mochitl_mascot),
                            contentDescription = "MochiTL Mascot",
                            modifier = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "MochiTL",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "AI Translation Workspace • v1.0.0",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (activeProject != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Book,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Proyek Aktif:",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                    )
                                    Text(
                                        text = activeProject!!.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    val menuItems = listOf(
                        Screen.HOME to ("Beranda" to Icons.Default.Home),
                        Screen.TEXT to ("Terjemahkan Teks" to Icons.Default.Language),
                        Screen.FILE to ("Terjemahkan File" to Icons.Default.Description),
                        Screen.PROJECTS to ("Proyek" to Icons.Default.Book),
                        Screen.PROMPTS to ("Pengelola Prompt" to Icons.AutoMirrored.Filled.Send),
                        Screen.GLOSSARY to ("Glosarium" to Icons.AutoMirrored.Filled.Comment),
                        Screen.HISTORY to ("Riwayat" to Icons.Default.History),
                        Screen.SETTINGS to ("Pengaturan AI" to Icons.Default.Settings),
                        Screen.ABOUT to ("Dokumentasi" to Icons.Default.Info)
                    )

                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(menuItems) { (targetScreen, pair) ->
                            val (label, icon) = pair
                            NavigationDrawerItem(
                                label = { Text(label) },
                                selected = screen == targetScreen,
                                onClick = {
                                    screen = targetScreen
                                    scope.launch { drawerState.close() }
                                },
                                icon = { Icon(icon, contentDescription = null) },
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isDarkTheme) "Mode Gelap" else "Mode Terang",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        IconButton(onClick = onToggleTheme) {
                            Icon(
                                if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = "Toggle Theme"
                            )
                        }
                    }
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    title = {
                        Column {
                            Text(
                                text = screenTitle(screen),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
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
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu Drawer")
                        }
                    },
                    actions = {
                        IconButton(onClick = onToggleTheme) {
                            Icon(
                                if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = "Toggle Theme"
                            )
                        }
                    }
                )
            }
        ) { padding ->
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                when (screen) {
                    Screen.HOME -> HomeScreen(vm) { screen = it }
                    Screen.TEXT -> TextTranslationScreen(vm)
                    Screen.FILE -> FileTranslationScreen(vm)
                    Screen.PROJECTS -> ProjectsScreen(vm)
                    Screen.PROMPTS -> PromptScreen(
                        vm = vm,
                        onNavigateToDocumentation = { page ->
                            docPageTarget = page
                            screen = Screen.ABOUT
                        }
                    )
                    Screen.GLOSSARY -> GlossaryScreen(vm)
                    Screen.HISTORY -> HistoryScreen(vm, onSelectHistoryItem = { text ->
                        vm.setInput(text)
                        screen = Screen.TEXT
                    })
                    Screen.SETTINGS -> SettingsScreen(vm)
                    Screen.ABOUT -> DocumentationScreen(initialPage = docPageTarget)
                }
            }
        }
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


@OptIn(ExperimentalMaterial3Api::class)

@OptIn(ExperimentalMaterial3Api::class)

@Composable
private fun ProjectsScreen(vm: MochiViewModel) {
    val projects by vm.projects.collectAsState()
    val activeProject by vm.activeProject.collectAsState()
    val prompts by vm.prompts.collectAsState()
    val providers by vm.providers.collectAsState()
    val glossaryList by vm.glossary.collectAsState()

    var showDialog by remember { mutableStateOf(false) }
    var editingProject by remember { mutableStateOf<TranslationProject?>(null) }

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
            Button(
                onClick = { editingProject = null; showDialog = true },
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
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
                    Text("Belum ada proyek dibuat. Klik 'Tambah Proyek' untuk membuat baru.", style = MaterialTheme.typography.bodyMedium)
                }
            }

            items(projects) { proj ->
                val isActive = activeProject?.id == proj.id
                val promptName = prompts.find { it.id == proj.promptTemplateId }?.name ?: "Default Prompt"
                val providerName = providers.find { it.id == proj.providerId }?.name ?: proj.providerId

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = if (isActive) CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer) else CardDefaults.cardColors()
                ) {
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
                                SuggestionChip(onClick = {}, label = { Text("AKTIF", fontWeight = FontWeight.Bold) })
                            }
                        }

                        if (proj.description.isNotBlank()) {
                            Text(proj.description, style = MaterialTheme.typography.bodyMedium)
                        }

                        val glossaryInfo = if (proj.glossaryIds.isEmpty()) "Semua (${glossaryList.size})" else "${proj.glossaryIds.size} terikat"
                        Text("Prompt: $promptName • Provider: $providerName • Target: ${proj.targetLanguage} • Glosarium: $glossaryInfo", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                        HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (!isActive) {
                                FilledTonalButton(
                                    onClick = { vm.selectProject(proj) },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) { Text("Aktifkan Proyek") }
                            } else {
                                OutlinedButton(
                                    onClick = { vm.selectProject(null) },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) { Text("Nonaktifkan") }
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = { editingProject = proj; showDialog = true }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit Proyek")
                                }
                                IconButton(onClick = { vm.deleteProject(proj.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Hapus Proyek", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

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

@OptIn(ExperimentalMaterial3Api::class)

@Composable
private fun PromptScreen(
    vm: MochiViewModel,
    onNavigateToDocumentation: (Int) -> Unit
) {
    val prompts by vm.prompts.collectAsState()
    var showEditDialog by remember { mutableStateOf(false) }
    var editingPrompt by remember { mutableStateOf<PromptTemplate?>(null) }
    var viewingSamplePrompt by remember { mutableStateOf<PromptTemplate?>(null) }

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
                OutlinedButton(
                    onClick = { vm.resetPromptsToDefault() },
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text("Reset")
                }
                Button(
                    onClick = { editingPrompt = null; showEditDialog = true },
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Tambah", fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // Compact Banner to navigate to Documentation
        OutlinedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            onClick = { onNavigateToDocumentation(3) }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.HelpOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Panduan Menulis Prompt Custom",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                TextButton(
                    onClick = { onNavigateToDocumentation(3) },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text("Lihat Panduan", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(prompts) { p ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
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
                            SuggestionChip(
                                onClick = {},
                                label = { Text(if (p.isBuiltIn) "Built-in" else "Kustom", fontWeight = FontWeight.Medium) }
                            )
                        }

                        Text(p.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        Text(p.content, style = MaterialTheme.typography.bodyMedium, maxLines = 4, overflow = TextOverflow.Ellipsis)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = { viewingSamplePrompt = p },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Lihat Detail Template", fontSize = 12.sp)
                            }

                            if (!p.isBuiltIn) {
                                Row {
                                    IconButton(onClick = { editingPrompt = p; showEditDialog = true }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit Prompt")
                                    }
                                    IconButton(onClick = { vm.deletePrompt(p.id) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Hapus Prompt", tint = MaterialTheme.colorScheme.error)
                                    }
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



@Composable
private fun GlossaryScreen(vm: MochiViewModel) {
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

    val filtered = glossaryList.filter {
        it.source.contains(searchQuery, ignoreCase = true) || it.target.contains(searchQuery, ignoreCase = true)
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
            Button(
                onClick = { editingEntry = null; showDialog = true },
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Tambah Istilah", fontWeight = FontWeight.SemiBold)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { exportLauncher.launch("glosarium_mochitl.json") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Ekspor JSON", fontSize = 13.sp)
            }

            OutlinedButton(
                onClick = { importLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Impor JSON", fontSize = 13.sp)
            }
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            placeholder = { Text("Cari istilah glosarium...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (filtered.isEmpty()) {
                item {
                    Text("Belum ada istilah glosarium. Tambahkan istilah nama karakter, jurus, atau tempat.", style = MaterialTheme.typography.bodyMedium)
                }
            }

            items(filtered) { entry ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
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



@OptIn(ExperimentalMaterial3Api::class)
