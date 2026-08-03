# BARRA CLOUD - Samba (SMB) Photo & Video Viewer

**BARRA CLOUD** adalah aplikasi Android native modern yang dirancang khusus sebagai **SMB Photo & Video Streaming Viewer** dengan fokus utama pada performa tinggi, tampilan galeri modern, dan **buffering yang sangat minimal**.

---

## 🚀 Fitur Utama

- **Koneksi SMB2 / SMB3**: Dukungan autentikasi server SMB, custom port, remember login, dan auto-connect otomatis.
- **SMB Streaming Tanpa Download Penuh**: Server HTTP Proxy lokal internal (NanoHTTPD) melayani permintaan `Byte Range` ke ExoPlayer & Coil.
- **Viewer Foto Responsif**:
  - Format: JPG, JPEG, PNG, WEBP, HEIC, GIF (Animasi), RAW Preview.
  - Pager swipe kiri/kanan, Double-tap zoom, Pinch zoom, Rotasi 90°, dan mode **Slideshow** otomatis.
  - Background prefetch gambar berikutnya untuk perpindahan instan.
- **Media3 ExoPlayer Video Player**:
  - Format: MP4, MKV, AVI, MOV, M4V, 3GP, WEBM, TS, MPEG.
  - Gestures: Kecerahan (sisi kiri), Volume / Position Seek (sisi kanan), Double Tap Seek (+/- 10s).
  - Resume playback otomatis & pengingat posisi terakhir.
  - Kecepatan pemutaran: 0.5x, 1x, 1.25x, 1.5x, 2x, Auto-landscape, & Picture-in-Picture (PiP).
- **Galeri & Pencarian Realtime**: Grid responsif (2 - 5 kolom), tab Favorit (Room DB), dan Riwayat Terakhir Dilihat.
- **Keamanan Credentials**: Menggunakan `EncryptedSharedPreferences`.

---

## 🛠️ Arsitektur & Tech Stack

- **Bahasa**: Kotlin 100%
- **UI Framework**: Jetpack Compose + Material 3
- **Database**: Room Database (Favorites & Recents)
- **Media Player**: AndroidX Media3 ExoPlayer
- **Image Loader**: Coil dengan Disk & Memory Cache
- **Arsitektur**: MVVM + Repository Pattern + Kotlin Coroutines & StateFlow
- **CI/CD**: GitHub Actions (`.github/workflows/build.yml`)
