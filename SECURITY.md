# Kebijakan Keamanan

## Versi yang didukung

Hanya rilis terbaru di [GitHub Releases](https://github.com/bymeopen/nextcraft-forge/releases) yang menerima perbaikan
keamanan.

## Melaporkan kerentanan

Laporkan secara **privat** lewat GitHub: tab **Security**, lalu **Report a vulnerability** (private vulnerability
reporting). Pemilik repositori perlu mengaktifkan fitur itu di Settings, Code security. Jangan membuka Issue publik
untuk kerentanan, dan jangan menyertakan API key, token, atau data pribadi dalam laporan.

Sertakan: versi aplikasi (Setelan, About), versi Android dan manajer root (Magisk, KernelSU, atau APatch), langkah mereproduksi, dan dampak yang Anda duga.
Pemelihara menanggapi dengan upaya terbaik, tanpa janji waktu tertentu.

## Cakupan

Dalam cakupan: kode aplikasi, skrip runtime (`scripts/runtime-bundles`, `chroot-exec.sh`), gateway
loopback, penyimpanan rahasia, dan alur pembaruan.

Di luar cakupan: kerentanan di Claude Code, Antigravity CLI, DeepSeek Harness, Ubuntu, Node.js, atau pustaka pihak ketiga
lain. Laporkan itu ke pemiliknya. Kami tetap menerima laporan bila proyek ini menggunakannya dengan cara yang tidak aman.

## Model ancaman yang perlu Anda ketahui

- **Agen menjalankan perintah.** Agen AI di aplikasi ini dapat menjalankan perintah shell di ruang kerja Linux privat.
  Aplikasi ini hanya untuk perangkat **root**: proses guest berjalan sebagai root di dalam chroot pada perangkat Anda,
  jadi agen yang dikompromikan atau proyek yang tidak tepercaya dapat berdampak besar. Jangan menjalankan agen atau
  proyek yang tidak Anda percayai, dan beri izin root hanya kepada aplikasi yang Anda pasang dari sumber resmi.
- **Rahasia di perangkat.** API key disimpan terenkripsi (AES-GCM, kunci di Android Keystore). Login Claude dan Google
  disimpan oleh CLI masing-masing di rootfs privat aplikasi. Perangkat yang di-root dan dikompromikan dapat membaca
  keduanya.
- **Pemasangan APK.** Izin `REQUEST_INSTALL_PACKAGES` dipakai untuk memasang APK hasil build dan pembaruan aplikasi.
  Pasang pembaruan hanya dari GitHub Releases resmi proyek.
- **Unduhan agen.** Claude Code dan Antigravity CLI diunduh langsung dari vendor dan diperiksa checksum-nya terhadap
  manifes vendor. Checksum dari server yang sama melindungi dari unduhan rusak, bukan dari server yang dikompromikan.
- **Tanda tangan.** Build debug memakai kunci debug yang ada di repositori dan **tidak boleh** dianggap resmi. Hanya APK
  rilis yang ditandatangani kunci rilis (secret CI) yang resmi.

Lihat juga [PRIVACY.md](PRIVACY.md) dan [docs/PERMISSIONS.md](docs/PERMISSIONS.md).
