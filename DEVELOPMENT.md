# Panduan Pengembang (Development & CI/CD Guide) - MochiTL

Dokumen ini ditujukan untuk pengembang dan kontributor yang ingin melakukan kompilasi, pengujian, serta pengkelolaan rilis aplikasi **MochiTL**.

---

## 🛠️ Tech Stack & Arsitektur

MochiTL dibangun menggunakan teknologi Android modern berbasis Kotlin Native:

- **UI Framework:** Jetpack Compose (Material 3)
- **Database:** Room Database (Proyek, Prompt, Glosarium, Riwayat)
- **Preferences:** SharedPreferences & EncryptedSharedPreferences (Android Keystore)
- **Network Client:** Ktor Client (Android Engine) + kotlinx.serialization
- **Dokumen Parser:** PDFBox Android
- **Target SDK:** 35 (minSdk 26)
- **Java Version:** Java 21 (JDK 17+)

---

## 💻 Kompilasi & Build Lokal

### Persyaratan Environment:
- JDK 17 atau OpenJDK 21
- Android SDK 35 (Platform Tools & Build Tools)
- Gradle 8.9+ (atau menggunakan `./gradlew`)

### Perintah Build:

1. **Jalankan Unit Test:**
   ```bash
   ./gradlew testDebugUnitTest
   ```

2. **Build Debug APK:**
   ```bash
   ./gradlew assembleDebug
   ```
   *APK tersimpan di:* `app/build/outputs/apk/debug/app-debug.apk`

3. **Build Release APK (Unsigned / Signed):**
   ```bash
   ./gradlew assembleRelease
   ```
   *APK tersimpan di:* `app/build/outputs/apk/release/app-release-unsigned.apk`

---

## 🔑 Konfigurasi Keystore & Signing

Untuk membuat Release APK signed secara lokal:

1. Salin `key.properties.example` menjadi `key.properties` di root repository.
2. Isi kredensial keystore milik Anda:
   ```properties
   storePassword=password_keystore_anda
   keyPassword=password_key_anda
   keyAlias=alias_key_anda
   storeFile=path/ke/release-key.jks
   ```
3. `key.properties` dan `*.jks` otomatis diabaikan oleh `.gitignore` dan **tidak boleh dicommit**.

---

## 🤖 CI/CD Workflow (GitHub Actions)

Workflow Build & Release berada di `.github/workflows/build-apk.yml`. Workflow ini menjalankan otomatis:

1. **Pengujian:** Menjalankan `:app:testDebugUnitTest` pada runner `ubuntu-latest`.
2. **Kompilasi Release:** Mendekode keystore dari GitHub Secrets (`ANDROID_KEYSTORE_BASE64`) dan mem-build APK Release Signed.
3. **Artifact Upload:** Menyimpan hasil APK sebagai artifact build yang bisa diunduh dari tab Actions.
4. **GitHub Releases:** Apabila push berupa tag versi (misal `v1.0.1`), CI otomatis membuat entri Rilis publik dan melampirkan APK release secara langsung.

---

## 🏷️ Prosedur Rilis Versi Baru

1. **Naikkan Versi di `app/build.gradle.kts`:**
   ```kotlin
   defaultConfig {
       versionCode = 10       // Naikkan +1
       versionName = "1.0.2"  // Nama versi baru
   }
   ```
2. **Commit Perubahan:**
   ```bash
   git commit -am "chore: release v1.0.2"
   ```
3. **Buat Tag Git & Upload ke Repository Remote:**
   ```bash
   git tag v1.0.2
   ```
   *Gunakan perintah git upload tag ke remote repo untuk memicu rilis otomatis.*
