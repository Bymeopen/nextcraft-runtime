# Laporan Tes — Build Debug `x` (2026-10-28)

Target: `com.nextcraft.forge.x` (build debug side-by-side, `-Pnextcraft.sideBySideSuffix=x`), versi `1.0.0-forge` (versionCode 100).
Perangkat: kernel `5.10.269-gki`, SDK 37, Snapdragon, RAM 10,9 GB. Root diberikan ke `x` oleh pengguna.
Cakupan: tes yang bisa dijalankan dari shell sistem Android dan layar `x`. Provider Claude sengaja tidak diuji (sesuai permintaan pengguna).

## Ringkasan

| Area | Hasil |
|---|---|
| Build host + tes unit | LULUS — 365 tes, 0 gagal, 0 error, 1 dilewati |
| Pemasangan `x` berdampingan dengan aplikasi utama | LULUS — aplikasi utama tidak tersentuh |
| Smoke test (start, force-stop, rotasi, gelap/terang, Home, Back, trim memori, izin) | LULUS — 0 crash |
| Setup runtime `x` (root, chroot) | LULUS — `COMPLETE`, "Linux chroot runtime is ready" |
| Terminal `x` | LULUS — prompt `root@localhost:~#` hidup |
| Toolchain di rootfs `x` | LULUS — lihat tabel di bawah |
| Build Android di chroot `x` (sample `mini-sekolah`) | GAGAL apa adanya; LULUS setelah perbaikan `compileSdk` di salinan uji |
| Tab Alat, tombol pintasan terminal, alur agen, backup/restore | BELUM DIUJI (lihat bagian terakhir) |

## 1. Build dan tes unit (host)

- `gradle :app:testDebugUnitTest :app:assembleDebug --offline --rerun-tasks`: exit 0.
- 365 tes: 0 gagal, 0 error, 1 dilewati (`WorkspaceCheckpointsTest.unreadableFileDoesNotAbortSnapshotOrCheckpoint`, lewat `assume*`; alasan pastinya belum diperiksa).
- APK: `app/build/outputs/apk/debug/app-debug.apk` (±53 MB). Build `x` memakai `applicationId` `com.nextcraft.forge.x`.

## 2. Pemasangan

- APK debug biasa memakai `applicationId` yang sama dengan aplikasi induk (`com.nextcraft.forge`); memasangnya akan menimpa aplikasi induk dan memutus sesi. Karena itu dipakai varian side-by-side.
- `android-shell` tidak melihat `/tmp` guest; APK dikirim lewat stdin: `pm install -r -S <ukuran>` → `Success`.

## 3. Tes shell sistem (paket `x`)

| # | Tes | Hasil |
|---|---|---|
| 1 | Cold start (`am start -W`) | `Status: ok`, ±620 ms, 0 crash |
| 2 | Force-stop lalu buka lagi | OK |
| 3a | Rotasi 4 arah | proses hidup, 0 crash |
| 3b/3c | Mode gelap / terang | proses hidup, 0 crash |
| 4a–4c | Home, resume, Back (fokus diverifikasi di `x` dulu) | proses hidup, 0 crash |
| 5a | `send-trim-memory RUNNING_CRITICAL` | proses hidup, 0 crash |
| 5b | `am kill` di latar belakang | TIDAK VALID — proses tidak mati |
| 5c | `kill -9` | TIDAK VALID — `pidof` kosong, tidak ada target |
| 6 | Cabut dan beri ulang `POST_NOTIFICATIONS` dan `RECORD_AUDIO` | aplikasi dibuka 4x, semua OK, 0 crash |

Memori idle ±100–104 MB (PSS). Rotasi, mode gelap, dan izin dikembalikan ke nilai awal.

## 4. Runtime dan toolchain di rootfs `x`

Sebelum root: engine `proot`, setup `ERROR` di 98,5% — verifikasi Python dan Android gagal `exit 159 (signal 31)`.
Setelah root: engine chroot, setup `COMPLETE`.

| Komponen | Hasil |
|---|---|
| OS | Ubuntu 24.04.5 LTS (aarch64, kernel 5.10.269-gki) |
| Python | 3.12.3 (`sqlite3`, `ssl`, `json` OK) |
| Node / npm | v24.22.1 / 11.19.1 |
| Java | OpenJDK 21.0.12.1 (`javac` OK) |
| Gradle | 9.8.1 (Kotlin 2.4.10, JVM 21.0.12.1) |
| Android SDK | `/root/android-sdk`: build-tools 36.0.0, platform `android-37.2`; `aapt2` berjalan |
| PHP / git / gcc | 8.3.6 / 2.43.0 / 13.3.0 |
| Claude Code | terpasang di `/usr/local/bin/claude` (tidak diuji) |
| Jaringan / tulis file | DNS `github.com` OK; tulis + baca file OK |

Catatan: pada percobaan pertama `gradle` "command not found" karena PATH di skrip uji salah (`/opt/gradle/bin`); lokasi benar `/opt/gradle/gradle-9.8.1/bin`. Itu kesalahan skrip uji, bukan bug aplikasi.

## 5. UI `x` (screenshot)

- Layar Proyek, Terminal, Atur AI, Alat, dan Setelan tampil; proses hidup, 0 crash.
- Terminal: prompt hidup, perintah dieksekusi shell. Error `uname: invalid option -- 'V'` berasal dari escape `input text` di skrip uji, bukan dari aplikasi.
- Atur AI: Claude Code belum terpasang saat tes awal (sebelum setup selesai); tidak diuji.
- Setelan: "Akses sistem Android" mati secara bawaan (sesuai desain).
- Tombol Back di tab Terminal kembali ke tab Proyek.

## Temuan

| # | Severity | Temuan | Lokasi |
|---|---|---|---|
| 1 | MEDIUM | Sample `mini-sekolah` tidak bisa dibuild di chroot: `compileSdk = 37` meminta `android-37.0`, rootfs hanya punya `android-37.2`. Error: `Failed to find target with hash string 'android-37.0' in: /root/android-sdk`. Dengan `compileSdk { version = release(37) { minorApiLevel = 2 } }` di salinan uji: `BUILD_EXIT=0`, `app-debug.apk` 959.909 byte. | `samples/mini-sekolah/app/build.gradle.kts:7` |
| 2 | LOW (dugaan) | `app/build.gradle.kts:10` juga `compileSdk = 37`. Di host aman (ada `platforms/android-37` dan `android-37.2`); belum diuji di rootfs. | `app/build.gradle.kts:10` |
| 3 | INFO | Tanpa root, `x` jatuh ke PRoot dan verifikasi Python/Android gagal SIGSYS (exit 159) walau `GLIBC_TUNABLES=glibc.pthread.rseq=0` terpasang. Nomor syscall yang diblokir belum diambil. Perlu penyelidikan untuk pengguna non-root. | `RuntimeEngine.kt`, `proot-exec.sh` |
| 4 | LOW | `FileProvider` membuka seluruh cache (`<cache-path path=".">`). | `res/xml/file_paths.xml` |
| 5 | LOW | `QUERY_ALL_PACKAGES` perlu justifikasi bila dirilis ke Play Store. | `AndroidManifest.xml` |
| 6 | LOW | `Log.d` mencetak nama kunci environment (bukan nilainya). | `ClaudeRuntimeBridge.kt:182` |

Audit statis (pencarian pola): `allowBackup=false`; cleartext HTTP hanya loopback; hanya `MainActivity` yang `exported`; tidak ada `GlobalScope`, `runBlocking`, `addJavascriptInterface`, atau `Runtime.exec`; 3 pemakaian `!!`; guard SSRF di MCP server dan pratinjau web.

## Belum diuji

- Tombol pintasan terminal dan output perintah lewat UI (ketukan jatuh ke tab Proyek karena Back mengganti tab).
- Tab Alat (Diagnostik Sistem, Pratinjau web, Pembangun Android, Manajer SSH, File Linux): hanya terbuka tanpa crash.
- Alur agen AI dan login OAuth (sesuai permintaan: provider Claude tidak diuji).
- Proses dimatikan sistem secara paksa dan pemulihan state (tes 5b/5c tidak valid).
- Backup/restore terenkripsi; audit mendalam `BackupCrypto`, `ApiKeyVault`, `RootPathGuard`, `ForgeMcpServer`.
- Mode pesawat dan `monkey` sengaja tidak dipakai (bisa memutus sesi / aksi acak).
- Build `app/` itu sendiri di dalam chroot `x`.

## Keadaan akhir

- Repo: tidak ada perubahan selain berkas laporan ini; tidak ada commit atau push.
- `x` masih terpasang. Hapus dengan `android-shell "pm uninstall com.nextcraft.forge.x"`.
- Salinan uji `/root/t-mini` di rootfs `x` sudah dihapus.

---

## Pembaruan: perbaikan `compileSdk`, tab Alat, audit kripto

### Perbaikan `compileSdk` (sample)
- `samples/mini-sekolah/app/build.gradle.kts`: `compileSdk = 37` → `compileSdk { version = release(37) { minorApiLevel = 2 } }` (gaya yang sama dengan `AndroidProjectTemplateGenerator.kt:120`).
- Diverifikasi: sample dari repo disalin ke rootfs `x`, `gradle :app:assembleDebug --offline --no-daemon` → `BUILD_EXIT=0`, `app-debug.apk` 959.909 byte. Salinan uji dihapus.
- `app/build.gradle.kts:10` (`compileSdk = 37`) sengaja TIDAK diubah: host punya `platforms/android-37` dan `android-37.2`, dan app sudah terbukti terbuild di host. Hanya rootfs `x` yang tidak punya `android-37`. Generator template proyek baru (`AndroidProjectTemplateGenerator`, `GameProjectTemplateGenerator`) sudah memakai blok `compileSdk { ... }`.

### Tab Alat (9 layar, via UI `x`)
Toolchain, Diagnostik Sistem, REA, File, File Linux, Pratinjau web, Matriks build, Pembangun Android, Manajer SSH: semuanya terbuka, proses hidup, 0 crash (`logcat -b crash`).
Isi yang dilihat lewat screenshot: Diagnostik (perangkat, RAM, penyimpanan, "Akses root: Diberikan", "Runtime engine: chroot"); Pembangun Android (tab Tahap/Masalah/Log/Logcat, status "Siap dibangun · Siaga"); Pratinjau web (empty state "Pratinjau belum berjalan", URL `http://127.0.0.1:3000`); Pengelola file (empty state "Belum ada ruang kerja").
Tidak dilihat isinya (hanya cek crash): Toolchain, REA, File Linux, Matriks build, Manajer SSH. Aksi di dalam layar (menjalankan build, mulai server pratinjau, sesi SSH) tidak dijalankan karena belum ada proyek di `x`.

### Audit kode (baca manual; tidak dijalankan)

| # | Severity | Temuan | Lokasi |
|---|---|---|---|
| A | MEDIUM | Migrasi kunci auth-bound memanggil `cleanSecret(secret)` tanpa `storageId`, sehingga `auto_backup_password` yang berisi spasi kehilangan spasi internalnya (di jalur normal spasi dipertahankan, `ApiKeyVault.kt:228`). Dampak sempit: hanya pengguna dengan kunci lama (auth-bound) dan passphrase backup bertanda spasi; backup lama tidak bisa dibuka dengan passphrase tersimpan. Perbaikan: `cleanSecret(secret, storageId)`. | `ApiKeyVault.kt:70` |
| B | LOW | Migrasi legacy di `ensurePool` memanggil `removeEncrypted(providerId)` sebelum `savePool(...)`. Jika proses mati di antaranya, rahasia tersimpan di `...pool.legacy` tanpa entri pool dan tidak terbaca lagi. Perbaikan: `savePool` + `setActiveId` dulu, baru `removeEncrypted`. | `ApiKeyVault.kt:241-244` |
| C | LOW | `data class ApiKeyCredential` memuat `secret`; `toString()` otomatis akan membocorkannya bila suatu saat dilog. Tidak ada pemakaian yang mencetaknya saat ini (grep). | `ApiKeyVault.kt:304` |
| D | LOW | PBKDF2-HMAC-SHA256 hanya 210.000 putaran (rekomendasi OWASP untuk SHA-256 setahu saya 600.000; 210.000 untuk SHA-512). Jumlah putaran tidak disimpan di header, jadi menaikkannya butuh `VERSION` baru. | `BackupCrypto.kt:31` |
| E | INFO | Byte tambahan setelah chunk terakhir diabaikan (tidak terdeteksi); file terpotong di tengah chunk memunculkan `EOFException`, bukan `WrongPasswordException`, sehingga UI menampilkan pesan umum `restore_failed` (`BackupScreen.kt:203`). Tidak crash. | `BackupCrypto.kt:129-145` |
| F | OK | `BackupCrypto`: AES-256-GCM per chunk, nonce unik (prefiks acak + indeks), AAD berisi indeks + flag terakhir (anti reorder/drop/truncate), password dibersihkan. `RootPathGuard`: `resolveChild` menolak traversal dan nama absolut/kosong; `isInsideAppData` memakai `matches` penuh pada prefiks dan marker `/<paket>/`, jadi `...forge.x` tidak lolos sebagai `...forge`. Semua pemanggil (`RootfsRepair`, `FileBrowser`) mengirim path kanonik. Risiko TOCTOU antara cek dan pemakaian (symlink) bersifat teoretis. | `BackupCrypto.kt`, `RootPathGuard.kt` |

Kelas tes `BackupCryptoTest` dan `RootPathGuardTest` ada di `app/src/test`; hasil per kelas tidak saya periksa terpisah.
Dua pola yang saya kira bug ternyata aman: rantai `.remove(...)` di dalam `preferences.edit { }` (`ApiKeyVault.kt:191-195`) valid, dan `return` di dalam `runCatching` menjadi return dari fungsi migrasi (disengaja).

### Keadaan repo
- Berubah: `samples/mini-sekolah/app/build.gradle.kts` (perbaikan di atas) dan berkas laporan ini. Tidak ada commit dan tidak ada push.

### Status perbaikan A dan B (`ApiKeyVault.kt`)
- A: migrasi kunci lama kini memanggil `cleanSecret(secret, storageId)`, sehingga spasi internal `auto_backup_password` dipertahankan.
- B: `ensurePool` kini menjalankan `savePool` dan `setActiveId` lebih dulu, lalu `removeEncrypted(providerId)` paling akhir.
- Verifikasi: `gradle :app:testDebugUnitTest --offline` → exit 0, 365 tes, 0 gagal, 0 error, 1 dilewati. Tes ini hanya membuktikan kode masih terkompilasi dan tes yang sudah ada tidak rusak; tidak ada tes baru, karena `ApiKeyVault` bergantung pada `AndroidKeyStore` dan saya tidak memverifikasi harness JVM yang bisa menjalankannya.
- Risiko sisa pada B: bila proses mati antara `savePool` dan `removeEncrypted`, entri legacy tertinggal. Jika pengguna kemudian menghapus semua kunci provider itu (pool menjadi kosong), `ensurePool` bisa memigrasikan entri tertinggal itu lagi dan "menghidupkan kembali" kunci lama. Jendela waktunya sangat sempit; belum ditangani.
