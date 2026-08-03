# BARRA CLOUD ☁️

**BARRA CLOUD** adalah aplikasi Gallery Online Pribadi berbasis SAMBA (SMB File Viewer) dan Tailscale Embedded Engine untuk Android Native (Kotlin + Jetpack Compose + Material Design 3).

Aplikasi ini membaca file dari server Samba (NAS) dan menampilkannya sebagai galeri foto & video modern layaknya Google Photos dengan performa tinggi, 60 FPS, dan hemat daya.

---

## 🌟 Fitur Utama

- **Google Photos-like Gallery**: Grid galeri foto & video modern dengan pilihan 2, 3, 4, 5 kolom.
- **Header Server Realtime**: Menampilkan IP server aktif (Tailscale / Local), kapasitas storage (terpakai/total), uptime, dan suhu server secara realtime.
- **Bottom Navigation (4 Menu)**:
  1. **Photos**: Galeri foto lengkap dengan thumbnail caching, lazy loading, dan Fullscreen Photo Viewer (Pinch-to-zoom, Swipe, Share, Download).
  2. **Videos**: Galeri video dengan thumbnail otomatis, durasi video (kanan bawah), dan Pemutar Video ExoPlayer lengkap dengan tombol download offline.
  3. **Albums**: Pengelompokan media otomatis berdasarkan folder server Samba.
  4. **Files**: File Manager sederhana lengkap dengan fitur Rename, Delete, Copy, Move, serta sorting berdasarkan Nama, Tanggal, dan Ukuran (mengabaikan folder thumbnail otomatis seperti `.thumb`).
- **Floating Upload Button (FAB)**: Mengunggah foto, video, atau dokumen dari perangkat Android ke server Samba.
- **SAMBA Login & Auto Connect**: SMBJ client dengan penyimpanan enkripsi EncryptedSharedPreferences, uji koneksi, dan auto reconnect.
- **Tailscale Embedded Engine**:
  - Switch ON/OFF Tailscale userspace networking engine (`tsnet`).
  - Tidak memerlukan aplikasi Tailscale resmi terpisah di HP.
  - Otomatis menggunakan IP Tailscale (`100.x.x.x`) saat ON, dan IP Lokal (`192.168.x.x`) saat OFF.
  - Status koneksi: Node IP, Device Name, Connection State, Login/Logout/Reconnect.
- **Customizable UI & Performance**:
  - Grid Slider (2–5 Kolom).
  - Thumbnail Size (Small, Medium, Large, XL).
  - Material You Theme (Light, Dark, Follow System).
  - Cache Manager (Clear Image Cache, Clear Video Cache, Clear Thumbnail Cache).

---

## 🛠️ Langkah Build Lokal

### Persyaratan System
- Android Studio Ladybug / ME / Jellyfish or later
- JDK 21
- Android SDK 36 (Min SDK 24 / Android 9.0+)

### Langkah Build via Command Line
```bash
# Clone repository
git clone https://github.com/your-username/BARRA-CLOUD.git
cd BARRA-CLOUD

# Berikan izin eksekusi gradle wrapper
chmod +x gradlew

# Build Debug APK
./gradlew assembleDebug

# Build Release APK
./gradlew assembleRelease
```
APK hasil build akan berada di:
- Debug: `app/build/outputs/apk/debug/app-debug.apk`
- Release: `app/build/outputs/apk/release/app-release.apk`

---

## 🤖 GitHub Actions CI/CD & Autobuild

Setiap kali Anda push commit atau membuat tag versi baru (`v1.0.0`), GitHub Actions workflow akan otomatis membangun file APK.

### Menyiapkan Secrets di Repository GitHub
Masuk ke repository GitHub Anda -> **Settings** -> **Secrets and variables** -> **Actions**:
1. `KEYSTORE_BASE64`: String Base64 dari file `.jks` keystore Anda.
2. `KEYSTORE_PASSWORD`: Password keystore.
3. `KEY_ALIAS`: Alias key penandatanganan.
4. `KEY_PASSWORD`: Password key alias.

### Cara Mengunduh Hasil Build APK
1. **Dari Action Artifacts**:
   - Buka tab **Actions** di repository GitHub Anda.
   - Pilih run workflow terbaru.
   - Pada bagian **Artifacts** di bagian bawah, unduh `BARRA-CLOUD-debug.apk` atau `BARRA-CLOUD-release.apk`.
2. **Dari GitHub Release (Tagging Auto-Release)**:
   - Buat tag baru:
     ```bash
     git tag v1.0.0
     git push origin v1.0.0
     ```
   - GitHub Actions akan otomatis membuat Release baru di halaman **Releases** repository dengan file `BARRA-CLOUD-debug.apk` dan `BARRA-CLOUD-release.apk` siap diunduh!

---

## 📄 Lisensi

Proyek ini dilisensikan di bawah **MIT License** - lihat file [LICENSE](LICENSE) untuk detail selengkapnya.
