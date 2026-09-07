@file:OptIn(ExperimentalMaterial3Api::class)

package com.mochi.tl

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Layar beranda yang dipoles — grid aksi dengan kartu visual.
 */
@Composable
internal fun HomeScreen(vm: MochiViewModel, navigate: (Screen) -> Unit) {
    val glossaryList by vm.glossary.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            tonalElevation = 2.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Image(
                    painter = painterResource(R.drawable.mochitl_mascot),
                    contentDescription = "MochiTL Mascot",
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(16.dp))
                )
                Text(
                    text = "MochiTL Workspace",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Penerjemah AI Novel, Manga, Manhwa & File",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
                    textAlign = TextAlign.Center
                )
            }
        }

        // Grid Aksi Utama
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            item {
                ActionCard(
                    icon = Icons.Default.Language,
                    label = "Terjemahkan Teks",
                    subtitle = "Masukkan teks langsung",
                    color = MaterialTheme.colorScheme.primary,
                    onClick = { navigate(Screen.TEXT) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                ActionCard(
                    icon = Icons.Default.Description,
                    label = "Terjemahkan File",
                    subtitle = "TXT, PDF, DOCX, EPUB",
                    color = MaterialTheme.colorScheme.secondary,
                    onClick = { navigate(Screen.FILE) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                ActionCard(
                    icon = Icons.Default.Book,
                    label = "Proyek",
                    subtitle = "Kelola proyek terjemahan",
                    color = Color(0xFF1565C0),
                    onClick = { navigate(Screen.PROJECTS) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                ActionCard(
                    icon = Icons.AutoMirrored.Filled.Send,
                    label = "Prompt",
                    subtitle = "Pengelola template prompt",
                    color = Color(0xFF6A1B9A),
                    onClick = { navigate(Screen.PROMPTS) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                ActionCard(
                    icon = Icons.AutoMirrored.Filled.Comment,
                    label = "Glosarium",
                    subtitle = "${glossaryList.size} istilah tersimpan",
                    color = Color(0xFF00838F),
                    onClick = { navigate(Screen.GLOSSARY) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                ActionCard(
                    icon = Icons.Default.Settings,
                    label = "Pengaturan AI",
                    subtitle = "Provider & parameter",
                    color = Color(0xFFBF360C),
                    onClick = { navigate(Screen.SETTINGS) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Riwayat ringkas — card kecil di bawah
        val history by vm.history.collectAsState()
        if (history.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { navigate(Screen.HISTORY) }
                    .then(Modifier.padding(bottom = 8.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Icon(
                            Icons.Default.History,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Riwayat Terjemahan",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${history.size} terjemahan tersimpan",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    subtitle: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(120.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = color.copy(alpha = 0.12f)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.padding(10.dp)
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
