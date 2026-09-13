# Metadata & Pengaturan Repository GitHub: MochiTL

Dokumen ini berisi panduan konfigurasi metadata GitHub (bagian **About**, **Topics/Tags**, dan **Releases**) untuk pemilik/maintainer repository **MochiTL**.

---

## 📌 1. Deskripsi Repository (Bagian "About")

Silakan menyalin teks berikut ke kolom **Description** di bagian kanan atas halaman utama repository (atau melalui tombol ⚙️ *Settings* -> *About*):

```text
Aplikasi Penerjemah AI Android (Kotlin + Jetpack Compose) untuk Manga, Light Novel, Dokumen & Teks dengan Dukungan Multi-Provider (Gemini, OpenAI, Ollama, LM Studio).
```

---

## 🏷️ 2. Topics / Tags GitHub

Tambahkan daftar topik/tags berikut pada menu **Topics** di bagian **About** agar repository MochiTL mudah ditemukan di pencarian GitHub:

```text
android
kotlin
jetpack-compose
ai-translator
manga-translator
light-novel-translator
gemini-api
openai-api
ollama
lm-studio
material-3
room-database
ktor
```

---

## 🚀 3. Langkah Memicu Release APK Otomatis

Workflow GitHub Actions (`.github/workflows/build-apk.yml`) sudah siap secara penuh. Setiap kali tag versi baru di-push ke GitHub remote, CI akan mem-build APK bertanda tangan (*signed*) dan otomatis membuat entri rilis di tab **Releases**.

### Cara Melakukan Rilis Versi Perdana (v1.0.1):

1. **Pastikan `versionCode` dan `versionName` di `app/build.gradle.kts` sudah sesuai:**
   ```kotlin
   defaultConfig {
       versionCode = 9
       versionName = "1.0.1"
   }
   ```

2. **Buat Git Tag di Terminal Lokal / Termux:**
   ```bash
   git tag -a v1.0.1 -m "Release MochiTL v1.0.1 - Versi Perdana Rilis Publik"
   ```

3. **Unggah Tag ke GitHub Remote:**
   ```bash
   git upload-tag-cmd v1.0.1
   ```
   *(Gunakan perintah git standar untuk mengunggah tag v1.0.1 ke origin)*

4. **Pantau Progres Build:**
   Buka tab **Actions** di GitHub Repository. Setelah workflow *Build Signed APK* selesai (berwarna hijau), file APK siap diunduh oleh publik di tab **Releases**.

---

## 📋 4. Template GitHub Issues

Template issue telah dikonfigurasi di folder `.github/ISSUE_TEMPLATE/`:
- `bug_report.md`: Laporan bug berstruktur untuk mempermudah perbaikan.
- `feature_request.md`: Usulan fitur baru dari pengguna dan kontributor.
