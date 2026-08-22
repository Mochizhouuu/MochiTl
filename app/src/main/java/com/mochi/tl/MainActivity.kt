package com.mochi.tl

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Comment
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

@Composable
fun MochiAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val lightColors = lightColorScheme(
        primary = Color(0xFF6750A4),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFEADDFF),
        onPrimaryContainer = Color(0xFF21005D),
        secondary = Color(0xFF625B71),
        onSecondary = Color.White,
        secondaryContainer = Color(0xEFE7F0),
        onSecondaryContainer = Color(0xFF1D192B),
        tertiary = Color(0xFF7D5260),
        background = Color(0xFFFEF7FF),
        surface = Color(0xFFFEF7FF),
        surfaceVariant = Color(0xFFE7E0EC),
        onSurfaceVariant = Color(0xFF49454F)
    )

    val darkColors = darkColorScheme(
        primary = Color(0xD0BCFF),
        onPrimary = Color(0xFF381E72),
        primaryContainer = Color(0xFF4F378B),
        onPrimaryContainer = Color(0xFFEADDFF),
        secondary = Color(0xFFCCC2DC),
        onSecondary = Color(0xFF332D41),
        secondaryContainer = Color(0xFF4A4458),
        onSecondaryContainer = Color(0xFFE8DEF8),
        tertiary = Color(0xFFEFB8C8),
        background = Color(0xFF141218),
        surface = Color(0xFF141218),
        surfaceVariant = Color(0xFF49454F),
        onSurfaceVariant = Color(0xFFCAC4D0)
    )

    MaterialTheme(
        colorScheme = if (darkTheme) darkColors else lightColors,
        content = content
    )
}

private enum class Screen { HOME, TEXT, FILE, PROJECTS, PROMPTS, GLOSSARY, HISTORY, SETTINGS, ABOUT }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MochiApp(
    vm: MochiViewModel,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit
) {
    var screen by remember { mutableStateOf(Screen.HOME) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val activeProject by vm.activeProject.collectAsState()

    // Pressing back on any screen returns to HOME instead of exiting the app
    BackHandler(enabled = screen != Screen.HOME) {
        screen = Screen.HOME
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
                                text = "AI Translation Workspace • v1.0.6",
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
                        if (screen != Screen.HOME) {
                            IconButton(onClick = { screen = Screen.HOME }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali ke Beranda")
                            }
                        } else {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = "Menu Drawer")
                            }
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
                    Screen.PROMPTS -> PromptScreen(vm)
                    Screen.GLOSSARY -> GlossaryScreen(vm)
                    Screen.HISTORY -> HistoryScreen(vm, onSelectHistoryItem = { text ->
                        vm.setInput(text)
                        screen = Screen.TEXT
                    })
                    Screen.SETTINGS -> SettingsScreen(vm)
                    Screen.ABOUT -> DocumentationScreen()
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

@Composable
private fun HomeScreen(vm: MochiViewModel, navigate: (Screen) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(R.drawable.mochitl_mascot),
                    contentDescription = "MochiTL Mascot",
                    modifier = Modifier
                        .size(90.dp)
                        .clip(RoundedCornerShape(16.dp))
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "MochiTL Workspace",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Aplikasi Penerjemah AI untuk Novel, Manga, Manhwa & File Dokumen",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
        }

        // Action Quick Access Buttons
        Button(
            onClick = { navigate(Screen.TEXT) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Language, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Mulai Terjemahkan Teks", fontSize = 16.sp)
        }

        OutlinedButton(
            onClick = { navigate(Screen.FILE) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Description, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Terjemahkan Dokumen / File TXT", fontSize = 16.sp)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = { navigate(Screen.PROJECTS) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Book, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Proyek")
            }
            OutlinedButton(
                onClick = { navigate(Screen.SETTINGS) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Settings AI")
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = { navigate(Screen.PROMPTS) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Prompts")
            }
            OutlinedButton(
                onClick = { navigate(Screen.GLOSSARY) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.Comment, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Glosarium")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TextTranslationScreen(vm: MochiViewModel) {
    val state by vm.state.collectAsState()
    val providers by vm.providers.collectAsState()
    val activeProvider by vm.activeProvider.collectAsState()
    val prompts by vm.prompts.collectAsState()
    val activePrompt by vm.activePrompt.collectAsState()

    val context = LocalContext.current
    val clipboard = remember { context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager }

    var sourceLanguage by remember { mutableStateOf("Jepang") }
    var targetLanguage by remember { mutableStateOf("Indonesia") }

    var showProviderMenu by remember { mutableStateOf(false) }
    var showPromptMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Top Toolbar: Quick Provider & Prompt selectors
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ExposedDropdownMenuBox(
                expanded = showProviderMenu,
                onExpandedChange = { showProviderMenu = !showProviderMenu },
                modifier = Modifier.weight(1f)
            ) {
                OutlinedCard(
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true),
                    onClick = { showProviderMenu = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = activeProvider.name,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                ExposedDropdownMenu(
                    expanded = showProviderMenu,
                    onDismissRequest = { showProviderMenu = false }
                ) {
                    providers.forEach { prov ->
                        DropdownMenuItem(
                            text = { Text(prov.name) },
                            onClick = {
                                vm.selectProvider(prov)
                                showProviderMenu = false
                            }
                        )
                    }
                }
            }

            ExposedDropdownMenuBox(
                expanded = showPromptMenu,
                onExpandedChange = { showPromptMenu = !showPromptMenu },
                modifier = Modifier.weight(1f)
            ) {
                OutlinedCard(
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true),
                    onClick = { showPromptMenu = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = activePrompt.name,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                ExposedDropdownMenu(
                    expanded = showPromptMenu,
                    onDismissRequest = { showPromptMenu = false }
                ) {
                    prompts.forEach { p ->
                        DropdownMenuItem(
                            text = { Text(p.name) },
                            onClick = {
                                vm.selectPrompt(p)
                                showPromptMenu = false
                            }
                        )
                    }
                }
            }
        }

        // Language Selector with Swap Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = sourceLanguage,
                onValueChange = { sourceLanguage = it },
                modifier = Modifier.weight(1f),
                label = { Text("Sumber") },
                singleLine = true
            )

            IconButton(
                onClick = {
                    val temp = sourceLanguage
                    sourceLanguage = targetLanguage
                    targetLanguage = temp
                }
            ) {
                Icon(Icons.Default.SwapHoriz, contentDescription = "Tukar Bahasa")
            }

            OutlinedTextField(
                value = targetLanguage,
                onValueChange = { targetLanguage = it },
                modifier = Modifier.weight(1f),
                label = { Text("Target") },
                singleLine = true
            )
        }

        // Source Text Input Field
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val charCount = state.input.length
                val wordCount = if (state.input.isBlank()) 0 else state.input.trim().split("\\s+".toRegex()).size

                Text(
                    text = "Teks Sumber ($charCount kar / $wordCount kata)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row {
                    if (state.input.isNotBlank()) {
                        IconButton(onClick = { vm.setInput("") }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Clear, contentDescription = "Hapus", modifier = Modifier.size(18.dp))
                        }
                    }
                    IconButton(
                        onClick = {
                            clipboard.primaryClip?.getItemAt(0)?.text?.let { vm.setInput(it.toString()) }
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.ContentPaste, contentDescription = "Tempel", modifier = Modifier.size(18.dp))
                    }
                }
            }

            OutlinedTextField(
                value = state.input,
                onValueChange = vm::setInput,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                placeholder = { Text("Tempel atau ketik teks yang ingin diterjemahkan di sini...") }
            )
        }

        // Progress or Error Message
        if (state.error != null) {
            Text(
                text = "Error: ${state.error}",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        if (state.isTranslating) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (state.isPaused) "Penerjemahan dijeda..." else "Menerjemahkan per chunk...",
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(
                        text = "${(state.progress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                LinearProgressIndicator(
                    progress = { state.progress },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Action Controls (Translate, Pause, Cancel)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { vm.translate(target = targetLanguage) },
                enabled = state.input.isNotBlank() && !state.isTranslating,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Terjemahkan")
            }

            if (state.isTranslating) {
                OutlinedButton(onClick = { if (state.isPaused) vm.resume() else vm.pause() }) {
                    Icon(if (state.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (state.isPaused) "Lanjut" else "Jeda")
                }

                OutlinedButton(onClick = vm::cancel) {
                    Icon(Icons.Default.Stop, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Batal")
                }
            }
        }

        // Translated Result Output Field
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Hasil Terjemahan",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (state.output.isNotBlank()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(
                            onClick = {
                                clipboard.setPrimaryClip(ClipData.newPlainText("MochiTL", state.output))
                                Toast.makeText(context, "Teks disalin ke clipboard", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Salin", modifier = Modifier.size(18.dp))
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
                            Icon(Icons.Default.Share, contentDescription = "Bagikan", modifier = Modifier.size(18.dp))
                        }

                        IconButton(
                            onClick = {
                                (context as? MainActivity)?.exportText("terjemahan_${System.currentTimeMillis()}.txt", state.output)
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.FileDownload, contentDescription = "Simpan", modifier = Modifier.size(18.dp))
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
                readOnly = false,
                placeholder = { Text("Hasil terjemahan AI akan muncul di sini...") }
            )
        }
    }
}

@Composable
private fun FileTranslationScreen(vm: MochiViewModel) {
    var fileName by remember { mutableStateOf<String?>(null) }
    var fileText by remember { mutableStateOf("") }
    val state by vm.state.collectAsState()
    val context = LocalContext.current

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
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Description, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Penerjemah Dokumen TXT", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Text(
                    text = "Pilih file teks (.txt) untuk diterjemahkan secara otomatis per-chunk dengan context prompt & glossary aktif.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Button(
            onClick = { filePicker.launch(arrayOf("text/plain")) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.FolderOpen, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (fileName == null) "Buka File Teks (.txt)" else "File: $fileName")
        }

        if (fileText.isNotBlank()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Info File Loaded:", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    Text("Ukuran Karakter: ${fileText.length} karakter")
                    Text("Estimasi Chunk API: ~${(fileText.length / 4000) + 1} bagian")
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Text("Pratinjau Teks Asli:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(fileText.take(300) + if (fileText.length > 300) "..." else "", style = MaterialTheme.typography.bodySmall)
                }
            }

            Button(
                onClick = { vm.translate(source = fileText) },
                enabled = !state.isTranslating,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Proses Terjemahkan File")
            }
        }

        if (state.isTranslating) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Progres Terjemahan File: ${(state.progress * 100).toInt()}%")
                LinearProgressIndicator(progress = { state.progress }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { if (state.isPaused) vm.resume() else vm.pause() }) {
                        Text(if (state.isPaused) "Lanjutkan" else "Jeda")
                    }
                    OutlinedButton(onClick = vm::cancel) { Text("Batal") }
                }
            }
        }

        if (state.output.isNotBlank()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Pratinjau Hasil Terjemahan Dokumen:", fontWeight = FontWeight.Bold)
                    Text(state.output.take(800) + if (state.output.length > 800) "\n..." else "", style = MaterialTheme.typography.bodyMedium)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Button(
                        onClick = {
                            val name = "terjemahan_${fileName?.removeSuffix(".txt") ?: "file"}.txt"
                            (context as? MainActivity)?.exportText(name, state.output)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Simpan File Terjemahan Lengkap")
                    }
                }
            }
        }
    }
}

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
            Text("Daftar Proyek", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Button(onClick = { editingProject = null; showDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Tambah Proyek")
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
                    colors = if (isActive) CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer) else CardDefaults.cardColors()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(proj.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            if (isActive) {
                                SuggestionChip(onClick = {}, label = { Text("AKTIF") })
                            }
                        }

                        if (proj.description.isNotBlank()) {
                            Text(proj.description, style = MaterialTheme.typography.bodyMedium)
                        }

                        Text("Prompt: $promptName • Provider: $providerName • Target: ${proj.targetLanguage}", style = MaterialTheme.typography.bodySmall)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (!isActive) {
                                TextButton(onClick = { vm.selectProject(proj) }) { Text("Aktifkan") }
                            } else {
                                TextButton(onClick = { vm.selectProject(null) }) { Text("Nonaktifkan") }
                            }
                            IconButton(onClick = { editingProject = proj; showDialog = true }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit")
                            }
                            IconButton(onClick = { vm.deleteProject(proj.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = MaterialTheme.colorScheme.error)
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
private fun ProjectEditDialog(
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
    var targetLang by remember { mutableStateOf(project?.targetLanguage ?: "Indonesia") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (project == null) "Tambah Proyek Baru" else "Edit Proyek") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Proyek") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Deskripsi Catatan") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = targetLang,
                    onValueChange = { targetLang = it },
                    label = { Text("Bahasa Target Utama") },
                    modifier = Modifier.fillMaxWidth()
                )
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
private fun PromptScreen(vm: MochiViewModel) {
    val prompts by vm.prompts.collectAsState()
    var showEditDialog by remember { mutableStateOf(false) }
    var editingPrompt by remember { mutableStateOf<PromptTemplate?>(null) }

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
            Text("Pengelola Prompt", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(onClick = { vm.resetPromptsToDefault() }) {
                    Text("Reset")
                }
                Button(onClick = { editingPrompt = null; showEditDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Tambah")
                }
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(prompts) { p ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(p.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            SuggestionChip(
                                onClick = {},
                                label = { Text(if (p.isBuiltIn) "Built-in" else "Kustom") }
                            )
                        }

                        Text(p.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        Text(p.content, style = MaterialTheme.typography.bodyMedium, maxLines = 4, overflow = TextOverflow.Ellipsis)

                        if (!p.isBuiltIn) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                IconButton(onClick = { editingPrompt = p; showEditDialog = true }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit")
                                }
                                IconButton(onClick = { vm.deletePrompt(p.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showEditDialog) {
        PromptEditDialog(
            prompt = editingPrompt,
            onDismiss = { showEditDialog = false },
            onSave = { newP ->
                vm.savePrompt(newP)
                showEditDialog = false
            }
        )
    }
}

@Composable
private fun PromptEditDialog(
    prompt: PromptTemplate?,
    onDismiss: () -> Unit,
    onSave: (PromptTemplate) -> Unit
) {
    var name by remember { mutableStateOf(prompt?.name.orEmpty()) }
    var description by remember { mutableStateOf(prompt?.description.orEmpty()) }
    var content by remember { mutableStateOf(prompt?.content.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (prompt == null) "Tambah Prompt Baru" else "Edit Prompt") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nama Prompt") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Deskripsi Singkat") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Instruksi System Prompt (Gunakan {target})") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 5
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && content.isNotBlank()) {
                        val p = PromptTemplate(
                            id = prompt?.id ?: java.util.UUID.randomUUID().toString(),
                            name = name,
                            content = content,
                            category = "custom",
                            description = description,
                            isBuiltIn = false
                        )
                        onSave(p)
                    }
                }
            ) { Text("Simpan") }
        },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("Batal") } }
    )
}

@Composable
private fun GlossaryScreen(vm: MochiViewModel) {
    val glossaryList by vm.glossary.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var showDialog by remember { mutableStateOf(false) }
    var editingEntry by remember { mutableStateOf<GlossaryEntry?>(null) }

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
            Text("Glosarium Istilah", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Button(onClick = { editingEntry = null; showDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Tambah Istilah")
            }
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
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
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(entry.source, fontWeight = FontWeight.Bold)
                                Text("  ➔  ", color = MaterialTheme.colorScheme.primary)
                                Text(entry.target, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                            if (entry.note.isNotBlank()) {
                                Text("Catatan: ${entry.note}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        Row {
                            IconButton(onClick = { editingEntry = entry; showDialog = true }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit")
                            }
                            IconButton(onClick = { vm.deleteGlossaryItem(entry.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = MaterialTheme.colorScheme.error)
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

@Composable
private fun GlossaryEditDialog(
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
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(value = source, onValueChange = { source = it }, label = { Text("Teks/Istilah Asli") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = target, onValueChange = { target = it }, label = { Text("Terjemahan Baku") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Catatan / Context (Opsional)") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (source.isNotBlank() && target.isNotBlank()) {
                        val item = GlossaryEntry(
                            id = entry?.id ?: java.util.UUID.randomUUID().toString(),
                            source = source,
                            target = target,
                            note = note
                        )
                        onSave(item)
                    }
                }
            ) { Text("Simpan") }
        },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("Batal") } }
    )
}

@Composable
private fun HistoryScreen(
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
            Text("Riwayat Terjemahan", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            if (history.isNotEmpty()) {
                OutlinedButton(onClick = { vm.clearHistory() }) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = null)
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
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
                            color = MaterialTheme.colorScheme.primary
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { onSelectHistoryItem(record.translatedText) }) {
                                Text("Buka di Editor")
                            }
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

@Composable
private fun SettingsScreen(vm: MochiViewModel) {
    val providers by vm.providers.collectAsState()
    val activeProvider by vm.activeProvider.collectAsState()
    val scope = rememberCoroutineScope()

    var apiKeyText by remember { mutableStateOf(vm.apiKey.orEmpty()) }
    var baseUrlText by remember { mutableStateOf(vm.customBaseUrl.orEmpty()) }
    var isKeyVisible by remember { mutableStateOf(false) }
    var autoSaveHistory by remember { mutableStateOf(vm.autoSave()) }

    var testStatus by remember { mutableStateOf<String?>(null) }
    var isTesting by remember { mutableStateOf(false) }

    LaunchedEffect(activeProvider.id) {
        apiKeyText = vm.apiKey.orEmpty()
        baseUrlText = vm.customBaseUrl.orEmpty()
        testStatus = null
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
            OutlinedCard(
                onClick = {
                    vm.selectProvider(prov)
                    apiKeyText = vm.apiKey.orEmpty()
                    baseUrlText = vm.customBaseUrl.orEmpty()
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
                        Text("Model: ${prov.model} • ${prov.baseUrl}", style = MaterialTheme.typography.bodySmall)
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
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isTesting) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Menguji Koneksi...")
            } else {
                Icon(Icons.Default.CheckCircle, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Uji Koneksi API")
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
    }
}

@Composable
private fun DocumentationScreen() {
    val docs = listOf(
        "Petunjuk Penggunaan" to "1. Atur Provider API & API Key di Pengaturan AI.\n2. Pilih Prompt Mode (Novel, Komik, General, atau OCR Cleanup).\n3. Tambahkan Glosarium istilah spesifik di Pengelola Glosarium.\n4. Masukkan teks di halaman Terjemahkan Teks atau buka file di Terjemahkan File.",
        "Prompt Engine & Context Injection" to "MochiTL secara otomatis menggabungkan instruksi prompt terpilih dan glosarium aktif ke dalam konteks system prompt sebelum mengirim permintaan ke AI.",
        "OpenAI & Local Model (Ollama / LM Studio)" to "Untuk Ollama/LM Studio lokal, pastikan server lokal Anda aktif di alamat http://127.0.0.1:11434 atau http://127.0.0.1:1234. Anda dapat menyesuaikan Base URL di Pengaturan AI.",
        "Split Build Multi-ABI APK" to "MochiTL dibangun dengan split ABI (arm64-v8a, armeabi-v7a, x86, x86_64, serta Universal APK) untuk performa optimal di setiap arsitektur Android.",
        "Keamanan Key" to "Seluruh API Key disimpan secara aman menggunakan EncryptedSharedPreferences yang dilindungi Android Keystore hardware-backed."
    )

    val pagerState = rememberPagerState(pageCount = { docs.size })
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Dokumentasi & Panduan MochiTL",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        // Carousel Slider
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) { page ->
            val (title, content) = docs[page]
            Card(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Topik ${page + 1} dari ${docs.size}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    HorizontalDivider()

                    Text(
                        text = content,
                        style = MaterialTheme.typography.bodyLarge,
                        lineHeight = 24.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Pager Indicator Dots & Navigation Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    if (pagerState.currentPage > 0) {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                    }
                },
                enabled = pagerState.currentPage > 0
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Sebelumnya")
            }

            // Dot indicators
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(docs.size) { index ->
                    val isSelected = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .size(if (isSelected) 10.dp else 6.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                            )
                    )
                }
            }

            IconButton(
                onClick = {
                    if (pagerState.currentPage < docs.size - 1) {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    }
                },
                enabled = pagerState.currentPage < docs.size - 1
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Selanjutnya")
            }
        }
    }
}
