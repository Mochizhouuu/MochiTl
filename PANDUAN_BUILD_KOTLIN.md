# Panduan Build MochiTL (Kotlin Native) via Termux + GitHub Actions

## Ringkasan jawaban: bisa

Proyek Kotlin ini sudah berupa kerangka yang bisa dikompilasi (Jetpack
Compose + Ktor + EncryptedSharedPreferences), bukan proyek kosong. Saya
audit manual seluruh 6 file `.kt` (Flutter/Android SDK tidak tersedia di
sandbox saya untuk compile langsung, jadi ini hasil pembacaan kode +
verifikasi API terhadap dokumentasi resmi, bukan hasil compile sungguhan)
dan menemukan beberapa bug nyata:

## Bug yang sudah diperbaiki

1. **`Unresolved reference: json`** di `TranslationRepository.kt` — fungsi
   `json(json)` dari Ktor dipanggil tanpa import
   `io.ktor.serialization.kotlinx.json.json`. Ini **pasti gagal compile**
   tanpa perbaikan ini.
2. **Tidak ada Gradle Wrapper** (`gradlew`, `gradle-wrapper.jar`) sama
   sekali di proyek — tanpa ini `./gradlew` tidak bisa dipakai di Termux
   maupun CI naif. Solusi: `gradle-wrapper.properties` sudah dibuat, dan
   workflow GitHub Actions pakai `gradle/actions/setup-gradle` yang tidak
   bergantung pada wrapper fisik. Langkah generate wrapper lengkap untuk
   Termux ada di bawah.
3. **Signing config release kosong** — gradle punya komentar "signing is
   intentionally absent", jadi `assembleRelease` akan menghasilkan APK
   *unsigned* yang tidak bisa diinstal. Sudah ditambahkan pembacaan
   `key.properties` seperti proyek Flutter.
4. **Crash potensial di Android 8/9 (API 26-28)** saat export TXT ke
   Download — `MediaStore.Downloads` baru ada sejak API 29, sementara
   `minSdk = 26`. Sudah ditambah fallback penulisan file langsung untuk
   API di bawah 29, plus permission `WRITE_EXTERNAL_STORAGE` khusus API
   lama.
5. **Ollama/LM Studio (provider lokal) tidak akan pernah bisa konek** —
   Android 9+ memblokir cleartext HTTP secara default, sementara kedua
   provider itu memang HTTP polos ke `127.0.0.1`. Sudah ditambahkan
   `network_security_config.xml` yang mengizinkan cleartext khusus untuk
   localhost saja (provider cloud tetap wajib HTTPS).
6. `Divider()` diganti `HorizontalDivider()` — yang lama cuma deprecated
   (bukan dihapus), jadi sebenarnya tetap bisa compile, tapi saya
   rapikan supaya tidak kena breaking change di update Compose berikutnya.
7. `LocalContext.current` dipanggil di dalam lambda callback file picker
   (bukan scope Composable langsung) — dirapikan dengan capture context
   di scope yang benar, supaya tidak berisiko baca context yang stale.

## Yang belum lengkap (di luar scope "pastikan bisa build")

Dependency **Room** dan **PDFBox** sudah dideklarasikan di gradle tapi
**belum dipakai sama sekali** di kode — tidak ada `@Entity`/`@Dao`, dan
`FileParser.kt` cuma baca TXT polos. Ini tidak menyebabkan gagal build
(dependency yang tidak dipakai bukan error), tapi berarti PDF/EPUB/DOCX
dan Room database masih perlu diimplementasikan dari nol. Screen
Glossary dan Prompt Manager juga masih placeholder (tombol tidak
berfungsi).

---

## Langkah build via Termux

### 1. Install Java (untuk Gradle)

```bash
pkg update -y
pkg install -y openjdk-17
```

### 2. Install Gradle di Termux

```bash
pkg install -y gradle
gradle -version
```

### 3. Generate Gradle Wrapper (sekali saja, lalu commit hasilnya)

```bash
cd ~/mochitl-kotlin
gradle wrapper --gradle-version 8.9
```

Ini akan membuat `gradlew`, `gradlew.bat`, dan
`gradle/wrapper/gradle-wrapper.jar`. Setelah ini ada, kamu bisa pakai
`./gradlew` seperti proyek Android pada umumnya, dan commit semuanya ke
repo (file-file wrapper memang lazim di-commit, bukan digitignore).

### 4. Generate keystore baru (sama seperti versi Flutter)

```bash
cd ~
keytool -genkeypair -v \
  -keystore release-key.jks \
  -alias mochitl \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000
```

Simpan password keystore & key baik-baik — kalau hilang, update APK ke
instalasi yang sudah ada tidak akan bisa lagi.

### 5. Build APK release secara lokal (opsional, untuk tes cepat)

```bash
cd ~/mochitl-kotlin
cp key.properties.example key.properties
# edit key.properties, isi storePassword/keyPassword/keyAlias sesuai
# langkah 4, dan pastikan storeFile menunjuk ke path release-key.jks kamu
./gradlew :app:assembleRelease
```

Hasil APK ada di `app/build/outputs/apk/release/app-release.apk`.

### 6. Setup GitHub Secrets untuk build otomatis

Sama seperti proyek Flutter — buka repo di GitHub → **Settings** →
**Secrets and variables** → **Actions** → buat 4 secret:

| Nama Secret | Isi |
|---|---|
| `ANDROID_KEYSTORE_BASE64` | `base64 -w 0 release-key.jks` lalu copy hasilnya |
| `ANDROID_KEYSTORE_PASSWORD` | Password keystore |
| `ANDROID_KEY_PASSWORD` | Password key |
| `ANDROID_KEY_ALIAS` | `mochitl` |

### 7. Push dan build otomatis

```bash
cd ~/mochitl-kotlin
git add .
git commit -m "Setup build APK Kotlin native"
git push origin main
```

Workflow `.github/workflows/build-apk.yml` akan build otomatis setiap
push ke `main`, atau bisa dipicu manual dari tab **Actions** → **Build
Signed APK (Kotlin native)** → **Run workflow**. Push tag `vX.Y.Z` untuk
sekaligus membuat GitHub Release berisi APK.

---

## Catatan penting

- Workflow GitHub Actions **tidak menunggu kamu generate wrapper** —
  dia pakai `gradle/actions/setup-gradle` yang menyediakan Gradle
  langsung di runner. Jadi kamu bisa langsung push dan build jalan
  duluan di CI, baru generate wrapper lokal belakangan kalau perlu untuk
  development harian di Termux.
- Saya tidak bisa benar-benar menjalankan compiler Kotlin/Gradle di sesi
  ini (tidak ada Android SDK, network mati untuk download dependency).
  Semua perbaikan di atas berdasarkan pembacaan kode manual +
  verifikasi API terhadap dokumentasi resmi Android/Ktor/Compose, bukan
  hasil compile sungguhan. Build pertama di GitHub Actions adalah
  validasi nyata pertama — kalau masih ada error, kirimkan log-nya dan
  saya bantu perbaiki lagi.
