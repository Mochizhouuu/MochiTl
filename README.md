# MochiTL (Kotlin Native)

Aplikasi penerjemah AI untuk manga, manhwa, light novel — dibangun dengan
Kotlin + Jetpack Compose, terhubung ke Gemini, OpenAI, OpenRouter, Ollama,
atau LM Studio.

## Status

Kerangka awal: terjemahkan teks per-chunk, ganti provider AI, simpan API
key terenkripsi, riwayat terjemahan, export ke folder Download. Fitur
yang belum lengkap: parser file PDF/EPUB/DOCX, database Room, Prompt
Manager dan Glossary (masih tampilan dasar).

## Build APK

Build otomatis lewat GitHub Actions setiap push ke main. Cek hasilnya
di tab Actions repo ini — unduh APK dari bagian Artifacts pada run yang
sudah selesai, atau dari tab Releases kalau build dipicu lewat tag versi.

### Build manual via Termux

pkg install -y openjdk-17 gradle
cd ~/mochitl-kotlin
gradle wrapper
./gradlew :app:assembleRelease

Build release butuh keystore signing. Salin key.properties.example
menjadi key.properties di root proyek, isi dengan password keystore
kamu. File key.properties dan *.jks sudah di-gitignore, jangan pernah
di-commit.

## Stack

Jetpack Compose (Material 3), Ktor, kotlinx.serialization,
EncryptedSharedPreferences (Android Keystore) untuk API key,
minSdk 26 / targetSdk 35.
