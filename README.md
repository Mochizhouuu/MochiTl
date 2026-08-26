# MochiTL (Kotlin Native)

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Platform](https://img.shields.io/badge/platform-Android-green)](https://www.android.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9-purple)](https://kotlinlang.org/)

Aplikasi penerjemah AI untuk manga, manhwa, light novel, dan dokumen — dibangun
dengan Kotlin + Jetpack Compose, terhubung ke Gemini, OpenAI, OpenRouter,
Ollama, atau LM Studio.

## Fitur

- **Multi-provider AI** — Gemini, OpenAI, OpenRouter, dan server lokal
  (Ollama / LM Studio) dengan uji koneksi & pengambilan daftar model otomatis.
- **Penerjemahan dokumen** — TXT, EPUB (urutan baca spine), DOCX, dan PDF.
- **Chunking cerdas** — pemotongan di batas paragraf (~4000 karakter) agar
  konteks terjemahan tidak terpotong di tengah kalimat.
- **Retry otomatis + backoff** — percobaan ulang per chunk saat kena rate limit
  atau jaringan terputus; hasil sebagian tetap disimpan bila ada bagian gagal.
- **Prompt Manager** — template gaya bawaan (novel, komik, akademik, pembersih
  OCR) + prompt kustom, pencarian, duplikasi, serta ekspor/impor JSON.
- **Glosarium** — istilah wajib (nama tokoh, jurus, tempat) disuntikkan
  otomatis ke setiap permintaan; ekspor/impor JSON.
- **Aturan kata benda khusus** — nama orang diromanisasi (Hepburn, Revised,
  Pinyin); nama jurus/title/senjata diterjemahkan ke Inggris untuk konteks fiksi.
- **Keamanan** — API key tersimpan terenkripsi (Android Keystore), teks user
  dibungkus tag anti-injection `<source_text>`.
- **Riwayat & proyek** — riwayat terjemahan otomatis (dibatasi 100 entri),
  proyek per-judul yang mengikat prompt/provider/glosarium/bahasa target.

## Penyimpanan Data

Metadata koleksi (proyek, prompt, glosarium, riwayat) tersimpan di database
Room. Pengaturan ringan seperti API key, model pilihan, dan parameter generasi
disimpan di SharedPreferences (API key melalui EncryptedSharedPreferences).
Data lama dari versi SharedPreferences dimigrasikan otomatis saat aplikasi
pertama kali dibuka.

## Stack

Jetpack Compose (Material 3), Room, Ktor, kotlinx.serialization,
EncryptedSharedPreferences (Android Keystore), PDFBox Android,
minSdk 26 / targetSdk 35.

## Lisensi

Dirilis under [MIT License](LICENSE).
