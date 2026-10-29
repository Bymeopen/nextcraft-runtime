# Kebijakan Privasi NextCraft Forge

Terakhir diperbarui: 2026-10-29. Dokumen ini menjelaskan apa yang dilakukan aplikasi menurut kodenya di repositori ini.
Pemilik repositori perlu meninjaunya sebelum rilis publik.

## Ringkasan

- NextCraft Forge **tidak memiliki akun, server, atau analitik milik proyek**. Tidak ditemukan SDK analitik atau
  pelaporan kerusakan pihak ketiga (Firebase, Crashlytics, Sentry, dan sejenisnya) di kode aplikasi.
- Data Anda disimpan **di perangkat**, di penyimpanan privat aplikasi. `allowBackup` dimatikan, sehingga data tidak ikut
  pencadangan otomatis Android.
- Aplikasi mengirim data ke **layanan yang Anda pilih sendiri** (penyedia AI, GitHub, vendor agen). Bagian di bawah
  menyebut ke mana.

## Data yang disimpan di perangkat

| Data | Di mana | Catatan |
| :--- | :--- | :--- |
| API key penyedia AI | `SharedPreferences` privat (`nextcraft_secrets`) | Dienkripsi AES-GCM dengan kunci di Android Keystore (`ApiKeyVault`) |
| Login Claude Code dan Antigravity CLI | Di dalam rootfs privat aplikasi (`/root/.claude`, `/root/.gemini` di guest) | Dikelola oleh CLI masing-masing, bukan oleh proyek ini |
| Proyek, riwayat chat, rootfs Linux | Penyimpanan privat aplikasi | Terhapus saat aplikasi di-uninstall atau datanya dihapus |
| Laporan kerusakan terakhir | `last-crash.txt` di penyimpanan privat | Hanya satu laporan, hanya bisa dibaca dan disalin dari Alat, Diagnostik Sistem. Tidak dikirim otomatis |

Pada perangkat root, berkas di dalam rootfs dimiliki oleh root, tetapi tetap berada di direktori privat aplikasi.

## Ke mana data dikirim

Aplikasi hanya menghubungi layanan berikut, dan hanya saat fiturnya dipakai:

| Tujuan | Untuk apa | Data yang dikirim |
| :--- | :--- | :--- |
| Penyedia AI yang Anda pilih (misalnya `api.anthropic.com`, `generativelanguage.googleapis.com`, `openrouter.ai`, `api.deepseek.com`, `api.moonshot.ai`, `integrate.api.nvidia.com`) | Menjalankan percakapan dan agen | Isi pesan, potongan kode atau berkas yang Anda atau agen sertakan, dan API key Anda untuk autentikasi |
| Anthropic, Google (Claude Code, Antigravity CLI) | Login dan panggilan agen oleh CLI resmi | Ditangani langsung oleh CLI di perangkat Anda, tunduk pada syarat vendor |
| `downloads.claude.ai`, `registry.npmjs.org`, `storage.googleapis.com` dan manifes pembaruan Antigravity | Mengunduh dan memperbarui agen langsung dari vendor | Permintaan unduh biasa (alamat IP dan versi yang diminta) |
| `github.com`, `api.github.com` | Rilis, runtime, pembaruan aplikasi, dan fitur GitHub bila Anda masuk | Token GitHub Anda (disimpan terenkripsi) dan data repositori yang Anda minta |
| `nodejs.org`, mirror Ubuntu (`apt`), `repo.maven.apache.org`, `getcomposer.org` | Menyiapkan runtime dan alat build di dalam guest | Permintaan unduh paket |
| `mcp.context7.com` dan konektor MCP lain yang Anda aktifkan | Alat tambahan untuk agen | Apa yang diminta agen lewat konektor itu |

Proyek ini tidak menerima salinan data tersebut. Apa yang dikirim ke penyedia AI tunduk pada kebijakan privasi penyedia itu.
Agen dapat membaca berkas di ruang kerja yang Anda buka dan mengirim isinya ke penyedia sebagai bagian dari percakapan.
Jangan membuka proyek berisi rahasia yang tidak ingin Anda kirim.

## Izin

Daftar lengkap izin dan alasannya ada di [docs/PERMISSIONS.md](docs/PERMISSIONS.md). Yang menyangkut privasi:
`RECORD_AUDIO` hanya untuk input suara opsional lewat pengenal suara bawaan Android (`SpeechRecognizer`);
bagaimana audio diproses bergantung pada layanan suara di perangkat Anda. `REQUEST_INSTALL_PACKAGES` hanya untuk
memasang APK hasil build dan pembaruan aplikasi.

## Menghapus data

Hapus data aplikasi lewat Pengaturan Android, atau uninstall aplikasi. Untuk mencabut akses agen, keluar (logout) dari
layar Atur AI, atau cabut API key di penyedia yang bersangkutan.

## Anak-anak

Aplikasi ini adalah alat pengembangan untuk pengguna dewasa dan tidak ditujukan bagi anak-anak.

## Perubahan dan kontak

Perubahan kebijakan dicatat lewat riwayat git berkas ini. Pertanyaan privasi disampaikan lewat Issues di repositori GitHub
proyek, tanpa menyertakan data pribadi atau rahasia.
