# Daftar periksa uji rilis (instalasi bersih)

Tujuan: memastikan APK **hasil build CI** (NDK dan kunci resmi) berfungsi di perangkat bersih sebelum rilis publik.
Aplikasi ini hanya untuk perangkat root (mesin PRoot sudah dibuang; lihat `docs/NON_ROOT.md`).
Salin berkas ini per putaran uji, isi kotak `[ ]` menjadi `[x]`, dan tulis hasil di bagian **Catatan**.

- Penguji: ________  Tanggal: ________
- Tahap: `[ ]` A (build debug CI, `release-debug.yml`)  `[ ]` B (build release bertanda tangan, `release-app.yml`, pre-release)
- Commit atau tag yang diuji: ________  `versionName` (Setelan, About): ________
- SHA-256 APK yang dipasang: ________

## 0. Sebelum mulai

- [ ] APK berasal dari artefak CI (bukan build lokal). Untuk tahap B: rilis ditandai **pre-release**, bukan "latest".
- [ ] `libforgespawn.so` ada di APK: `unzip -l <apk> | grep forgespawn` menampilkan satu baris.
- [ ] Perangkat bersih: aplikasi lama (dan edisi samping `.x`) di-uninstall, atau data aplikasinya dihapus.
- [ ] Ruang kosong lebih dari 3 GB dan koneksi stabil. Unduhan agen besar: Claude Code sekitar 292 MB, Antigravity CLI sekitar 61 MB.

## 1. Perangkat root (`com.nextcraft.forge`, mesin chroot)

Perangkat: ________  Android: ____  Root (Magisk/KernelSU): ________

- [ ] APK `com.nextcraft.forge` terpasang dan terbuka. Layar Alat menunjukkan mesin **chroot**.
- [ ] Setup runtime selesai tanpa error: bundle Core dan Android diunduh dari release `runtime-2026.10.2` (checksum cocok).
- [ ] **Claude Code**: ter-install dari vendor. Di terminal aplikasi: `claude --version` (acuan uji sebelumnya: `2.1.312`).
- [ ] Login Claude: browser terbuka sendiri, kode otorisasi diterima, `claude auth status` menunjukkan `"loggedIn": true`.
- [ ] Satu pesan ke Claude dari UI aplikasi dijawab.
- [ ] **Antigravity CLI**: ter-install dari vendor. `agy --version` (acuan: `1.4.10`).
- [ ] Login Google: browser terbuka sendiri (atau tombol **Buka browser** bekerja), kode terisi atau ditempel, status masuk.
- [ ] `agy models` menampilkan daftar model.
- [ ] Satu pesan ke Antigravity dari UI aplikasi dijawab (**belum pernah diuji**).
- [ ] Tombol **Batal** saat login berjalan menghentikan prosesnya, dan login bisa dimulai lagi.
- [ ] Build proyek contoh `samples/mini-sekolah` (Gradle) selesai.
- [ ] Setelah aplikasi ditutup paksa dan dibuka lagi, kedua login masih ada.

## 2. Khusus tahap B (build release, R8)

- [ ] APK bertanda tangan kunci rilis: `apksigner verify --print-certs <apk>` menampilkan sertifikat rilis, bukan `CN=Android Debug`.
- [ ] Tidak ada crash saat startup, membuka tiap layar utama, mengirim pesan, dan membuka terminal.
- [ ] Unduh manifes dan biner agen berjalan (R8 tidak menghapus kelas JSON atau JNI): login Claude dan Antigravity tetap berhasil.
- [ ] Cek pembaruan aplikasi (Setelan) tidak menawarkan pre-release ini kepada build lain yang terpasang.
- [ ] Rilis di GitHub berstatus **Pre-release** dan **bukan** "Latest".

## 4. Hasil

| Perangkat | Mode | Tahap | Lulus | Gagal | Catatan |
| :--- | :--- | :--- | :---: | :---: | :--- |
| ________ | chroot | A | | | |
| ________ | chroot | B | | | |

**Catatan** (pesan error, tangkapan layar, isi `last-crash.txt` bila ada, tanpa token atau API key):

________

Setelah selesai: perbarui bagian "Terverifikasi di perangkat" dan "Belum terverifikasi" di
[RUNTIME_KNOWN_ISSUES.md](RUNTIME_KNOWN_ISSUES.md), lalu lanjutkan ke [RELEASING.md](RELEASING.md).
