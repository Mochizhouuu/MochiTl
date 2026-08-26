package com.mochi.tl

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

/**
 * Layar dokumentasi & panduan penggunaan aplikasi (6 halaman pager).
 * Diekstrak dari MainActivity.kt sebagai bagian dari pemisahan UI
 * per-screen.
 */
internal fun DocumentationScreen(initialPage: Int = 0) {
    val sections = listOf(
        Triple(
            "1. Penerjemahan Teks & Dokumen TXT",
            Icons.Default.Language,
            "• Editor Teks Langsung:\n" +
                    "  Salin dan tempelkan kalimat, paragraf, atau bab cerita langsung di menu 'Terjemahkan Teks'. Aplikasi menghitung karakter dan estimasi kata secara real-time.\n\n" +
                    "• Dokumen Berukuran Besar (.txt, .epub, .docx, .pdf):\n" +
                    "  Gunakan menu 'Terjemahkan File' untuk memuat dokumen. MochiTL akan secara otomatis memecah file menjadi potongan teks (chunking di batas paragraf, ~4000 karakter), menerjemahkannya secara berurutan dengan percobaan ulang otomatis saat gagal, dan menyimpan hasil sebagian bila ada bagian yang tidak terjawab.\n\n" +
                    "• Kontrol Proses Real-time:\n" +
                    "  Saat penerjemahan berjalan, Anda dapat menekan tombol Jeda (Pause) untuk menghentikan sementara, Lanjutkan (Resume) untuk meneruskan, atau Batal (Cancel) kapan saja."
        ),
        Triple(
            "2. Pengkonfigurasian Provider AI",
            Icons.Default.Settings,
            "• Layanan Provider Cloud:\n" +
                    "  Mendukung Google Gemini API, OpenAI (GPT-4o/GPT-3.5), dan OpenRouter. Masukkan API Key Anda pada menu Pengaturan AI. Kunci disandikan secara aman menggunakan EncryptedSharedPreferences (Android Keystore).\n\n" +
                    "• Server AI Lokal (Ollama & LM Studio):\n" +
                    "  Dapat dijalankan secara offline atau lokal tanpa API Key! Atur Base URL ke http://10.0.2.2:11434 (Emulator) atau IP PC lokal Anda (misal: http://192.168.1.50:11434) untuk Ollama / LM Studio.\n\n" +
                    "• Fitur Uji Koneksi:\n" +
                    "  Sebelum melakukan terjemahan panjang, tekan tombol 'Uji Koneksi API' untuk meyakinkan bahwa endpoint server dan API Key siap digunakan."
        ),
        Triple(
            "3. Manajemen Proyek Terjemahan",
            Icons.Default.Book,
            "• Isolasi Preferensi Seri:\n" +
                    "  Buat proyek khusus untuk setiap judul Light Novel, Manga, atau Seri Dokumen. Setiap proyek menyimpan Prompt Template pilihan, Provider AI, Bahasa Target, dan Glosarium terikat.\n\n" +
                    "• Pengaktifan Proyek (Active Project):\n" +
                    "  Saat sebuah proyek diaktifkan dari menu Proyek, indikator Proyek Aktif akan muncul di bilah navigasi atas. Seluruh sesi penerjemahan otomatis menggunakan aturan proyek tersebut.\n\n" +
                    "• Menjaga Konsistensi Istilah:\n" +
                    "  Mencegah AI lupa nama tokoh atau perubahan istilah secara tiba-tiba di pertengahan bab."
        ),
        Triple(
            "4. Panduan Menulis Prompt Custom",
            Icons.AutoMirrored.Filled.Send,
            "• CARA KERJA OTOMATIS:\n" +
                    "  Anda TIDAK PERLU menulis {target} atau <source_text> sama sekali! Bahasa sumber, bahasa target, serta pembungkus data aman <source_text> diatur otomatis oleh sistem di belakang layar.\n\n" +
                    "• CARA MENULIS ATURAN GAYA:\n" +
                    "  Cukup tulis aturan gaya/preferensi terjemahan Anda menggunakan bahasa natural sehari-hari.\n" +
                    "  Contoh:\n" +
                    "  - \"Gunakan bahasa santai/gaul untuk percakapan antartokoh.\"\n" +
                    "  - \"Pertahankan honorifik Jepang seperti -san, -kun, -sama.\"\n" +
                    "  - \"Jangan terjemahkan nama tempat dan nama jurus.\"\n" +
                    "  - \"Gunakan tata bahasa formal dan lugas.\"\n\n" +
                    "• CONTOH ATURAN GAYA BUILT-IN 'Novel & Fiction':\n" +
                    "  \"Gaya penulisan novel fiksi. Pertahankan nada emosional, narasi ekspresif, nuansa percakapan tokoh, dan konsistensi panggilan/honorifik.\"\n\n" +
                    "• SARAN & PRAKTIK BAIK (BEST PRACTICES):\n" +
                    "  - Tulis aturan dengan jelas dan singkat.\n" +
                    "  - Gunakan Glosarium untuk mengunci istilah baku khusus (nama tokoh, lokasi).\n" +
                    "  - Sistem akan otomatis menggabungkan aturan Anda dengan instruksi penerjemah profesional."
        ),
        Triple(
            "5. Glosarium & Impor / Ekspor JSON",
            Icons.AutoMirrored.Filled.Comment,
            "• Automatic Context Injection:\n" +
                    "  Daftar pasangan istilah di Glosarium (Misal: 主人公 ➔ Pahlawan Utama) disuntikkan secara otomatis ke dalam instruksi AI setiap kali penerjemahan diproses.\n\n" +
                    "• Format & Struktur File JSON Glosarium:\n" +
                    "  Proses impor Glosarium HANYA mendukung file JSON dengan struktur array objek seperti berikut:\n" +
                    "  [\n" +
                    "    {\n" +
                    "      \"source\": \"Kata / Nama Asli\",\n" +
                    "      \"target\": \"Hasil Terjemahan Baku\",\n" +
                    "      \"note\": \"Catatan opsional\"\n" +
                    "    }\n" +
                    "  ]\n\n" +
                    "• Fitur Ekspor & Impor JSON:\n" +
                    "  Gunakan 'Ekspor JSON' untuk membuat cadangan glosarium. Gunakan 'Impor JSON' untuk memuat file .json glosarium."
        ),
        Triple(
            "6. Riwayat & Ekspor Hasil",
            Icons.Default.History,
            "• Penyimpanan Riwayat Otomatis:\n" +
                    "  Jika opsi 'Simpan Riwayat Otomatis' di Pengaturan diaktifkan, setiap hasil penerjemahan tersimpan otomatis dan dapat dibuka kembali di Editor kapan saja.\n\n" +
                    "• Ekspor Langsung Ke Penyimpanan:\n" +
                    "  Hasil penerjemahan teks dan file dapat disimpan langsung menjadi file file .txt di folder Download/MochiTL perangkat Android Anda."
        )
    )

    val pagerState = rememberPagerState(
        initialPage = initialPage.coerceIn(0, sections.size - 1),
        pageCount = { sections.size }
    )
    val scope = rememberCoroutineScope()

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
                text = "Dokumentasi & Panduan",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${pagerState.currentPage + 1} / ${sections.size}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }

        // Slider Horizontal Pager
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { page ->
            val (title, icon, content) = sections[page]
            Card(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 4.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))

                    Text(
                        text = content,
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 22.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // Page Indicator Dots & Navigation Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = {
                    if (pagerState.currentPage > 0) {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                    }
                },
                enabled = pagerState.currentPage > 0,
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Sebelumnya", fontSize = 13.sp)
            }

            // Indicator Dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(sections.size) { index ->
                    val isSelected = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .size(if (isSelected) 10.dp else 6.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                    )
                }
            }

            Button(
                onClick = {
                    if (pagerState.currentPage < sections.size - 1) {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    }
                },
                enabled = pagerState.currentPage < sections.size - 1,
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text("Berikutnya", fontSize = 13.sp)
                Spacer(modifier = Modifier.width(4.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
            }
        }
    }
}
