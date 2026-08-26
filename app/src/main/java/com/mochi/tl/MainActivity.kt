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
        secondaryContainer = Color(0xFFE8DEF8),
        onSecondaryContainer = Color(0xFF1D192B),
        background = Color(0xFFFBF8FD),
        surface = Color(0xFFFBF8FD),
        surfaceVariant = Color(0xFFE7E0EC),
        onSurfaceVariant = Color(0xFF49454F)
    )

    val darkColors = darkColorScheme(
        primary = Color(0xFFD0BCFF),
        onPrimary = Color(0xFF381E72),
        primaryContainer = Color(0xFF4F378B),
        onPrimaryContainer = Color(0xFFEADDFF),
        secondary = Color(0xFFCCC2DC),
        onSecondary = Color(0xFF332D41),
        secondaryContainer = Color(0xFF4A4458),
        onSecondaryContainer = Color(0xFFE8DEF8),
        background = Color(0xFF141218),
        surface = Color(0xFF141218),
        surfaceVariant = Color(0xFF49454F),
        onSurfaceVariant = Color(0xFFCAC4D0)
    )

    val colorScheme = if (darkTheme) darkColors else lightColors
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            window?.let { win ->
                win.statusBarColor = colorScheme.surface.toArgb()
                win.navigationBarColor = colorScheme.surface.toArgb()
                val controller = WindowCompat.getInsetsController(win, view)
                controller.isAppearanceLightStatusBars = !darkTheme
                controller.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
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

@Composable
private fun HomeScreen(vm: MochiViewModel, navigate: (Screen) -> Unit) {
    val glossaryList by vm.glossary.collectAsState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(R.drawable.mochitl_mascot),
                    contentDescription = "MochiTL Mascot",
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(16.dp))
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "MochiTL Workspace",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Aplikasi Penerjemah AI untuk Novel, Manga, Manhwa & File",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )
            }
        }

        // Action Quick Access Buttons
        Button(
            onClick = { navigate(Screen.TEXT) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(vertical = 12.dp, horizontal = 16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.Language, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Mulai Terjemahkan Teks",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        OutlinedButton(
            onClick = { navigate(Screen.FILE) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(vertical = 12.dp, horizontal = 16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.Description, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Terjemahkan Dokumen / File TXT",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = { navigate(Screen.PROJECTS) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(vertical = 10.dp, horizontal = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.Book, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Proyek", fontWeight = FontWeight.Medium, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            OutlinedButton(
                onClick = { navigate(Screen.SETTINGS) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(vertical = 10.dp, horizontal = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Settings AI", fontWeight = FontWeight.Medium, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = { navigate(Screen.PROMPTS) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(vertical = 10.dp, horizontal = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Prompts", fontWeight = FontWeight.Medium, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            OutlinedButton(
                onClick = { navigate(Screen.GLOSSARY) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(vertical = 10.dp, horizontal = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.AutoMirrored.Filled.Comment, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Glosarium (${glossaryList.size})", fontWeight = FontWeight.Medium, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FileTranslationScreen(vm: MochiViewModel) {
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
                    Text("Penerjemah Dokumen TXT", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Text(
                    text = "Pilih file teks (.txt) untuk diterjemahkan secara otomatis per-chunk dengan context prompt & glossary aktif.",
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
            onClick = { filePicker.launch(arrayOf("text/plain")) },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.FolderOpen, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (fileName == null) "Buka File Teks (.txt)" else "File: $fileName", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
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
private fun PromptSampleViewerDialog(
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
private fun PromptEditDialog(
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
                OutlinedButton(
                    onClick = { vm.clearHistory() },
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
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
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilledTonalButton(
                                onClick = { onSelectHistoryItem(record.translatedText) },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Buka di Editor")
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(vm: MochiViewModel) {
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
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Pilih Provider AI", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        providers.forEach { prov ->
            val isSelected = activeProvider.id == prov.id
            val currentModel = if (isSelected) modelText else (vm.storageModelFor(prov.id) ?: prov.model)
            OutlinedCard(
                onClick = {
                    vm.selectProvider(prov)
                    apiKeyText = vm.apiKey.orEmpty()
                    baseUrlText = vm.customBaseUrl.orEmpty()
                    modelText = vm.customModel ?: prov.model
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
                        Text("Model: $currentModel • ${prov.baseUrl}", style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
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

        // Model Selection & Fetch Button
        Text("Pilihan Model AI", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
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
                    }
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
                            fetchModelStatus = "Gagal memuat model: ${res.exceptionOrNull()?.message}"
                        }
                    }
                },
                enabled = !isFetchingModels,
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp)
            ) {
                if (isFetchingModels) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Fetch", fontWeight = FontWeight.Bold)
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
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (isTesting) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Menguji Koneksi...", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            } else {
                Icon(Icons.Default.CheckCircle, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Uji Koneksi API", fontSize = 15.sp, fontWeight = FontWeight.Bold)
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

        Text("Integrasi Glosarium & Context", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
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

        // ===== Parameter Generasi AI =====
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("Parameter Generasi AI", fontWeight = FontWeight.Bold)
            Text(
                "Berlaku untuk semua provider (Gemini, OpenAI, OpenRouter, lokal).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
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
}
