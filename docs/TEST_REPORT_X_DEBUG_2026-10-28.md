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

---

## Tes lanjutan (ketahanan, lint, instrumentasi)

Tiga commit lokal sudah dibuat (perbaikan vault, sample `compileSdk`, laporan); tidak ada push. APK `x` yang berisi perbaikan vault dipasang ulang dengan `pm install -r` (data `x` tetap; aplikasi induk tidak tersentuh).

| Tes | Hasil |
|---|---|
| Proses dimatikan paksa (`kill -9` pada `x` yang sedang di depan) | LULUS — `ActivityManager` menghidupkan ulang activity teratas dengan pid baru (`Start proc ... for top-activity`); 0 crash, 0 ANR |
| 3 putaran x 25 siklus lima tab (375 perpindahan) | Tidak ada bukti kebocoran, tapi tidak konklusif. PSS (setelah GC paksa) 100,5 → 125,4 MB di putaran pertama (pemanasan cache), lalu +1,7 MB dan +1,1 MB di putaran berikutnya. Java heap datar (±15,7–16,1 MB), `Views=21`, `ViewRootImpl=2` stabil. 0 crash, 0 ANR. |
| Terminal: banjir output (`yes line \| head -n 30000; echo STRESS_OK`) | LULUS — `STRESS_OK` tampil, prompt kembali, 0 crash, 0 ANR |
| Proses bertahan di latar belakang (Home, 55 detik, command `sleep 40` berjalan) | LULUS di level proses (pid sama). TIDAK TERVERIFIKASI di level sesi: `am start -n` tanpa flag membuat instance `MainActivity` baru (21 instance tercatat), sehingga terminal menampilkan shell baru dan `BG_DONE` tidak bisa dicek. Ini artefak skrip uji, bukan bug yang terbukti. Tidak terdeteksi foreground service (`isForeground=true` = 0), jadi proses mungkin hanya selamat karena belum dimatikan sistem. |
| Lint (`gradle :app:lintDebug --offline`) | `BUILD SUCCESSFUL`; 0 error, 43 warning, 12 hint |
| Tes instrumentasi `NativeSpawnProcessTest` | TIDAK DAPAT DIJALANKAN — APK `x` dan APK tes terbuild dan terpasang (`pm list instrumentation` mencantumkannya), tapi `am instrument` tidak mengeluarkan output (juga untuk target palsu) dan proses tes tidak pernah muncul; logcat kosong. Penyebab belum diketahui. |

Lint, rincian (selain 29 `PluralsCandidate`, 12 `AutoboxingStateCreation`, 1 `TypographyDashes`):
- `ConstantLocale` — `model/WorkspaceDomain.kt:34`: `Locale.getDefault()` disimpan di field statis final; tidak ikut berubah bila locale berubah saat aplikasi berjalan.
- `LocalContextResourcesRead` — `ui/UiText.kt:56`: membaca `LocalContext.current.resources` tidak sadar konfigurasi; bisa menampilkan bahasa lama setelah pergantian bahasa di dalam aplikasi (dugaan, belum diuji lewat UI).
- `ConfigurationScreenWidthHeight` x4 — `ui/ForgeMotion.kt:232,237`, `ui/ProjectsScreen.kt:582`.
- `SdCardPath` — `runtime/AntigravityHermesScripts.kt:956`; `UsableSpace` — `runtime/RuntimeInstallerStacks.kt:340`.
- `EmptySuperCall` — `ui/MainViewModel.kt:808`; `ModifierParameter` x2 — `ui/AndroidAndPreviewScreen.kt:591`, `ui/ProjectsScreen.kt:106`; `UseOfNonLambdaOffsetOverload` — `ui/ForgeBottomBar.kt:213`; `UnusedResources` — `R.string.chat_menu_changes`.

Pembersihan: paket `com.nextcraft.forge.x.test` dihapus, `x` di-force-stop, berkas sementara saya di `/data/local/tmp` dihapus.

---

## Uji lanjutan 2: bahasa di dalam aplikasi dan kripto backup

### Ganti bahasa di `x` (via UI, lalu dikembalikan ke "Bawaan sistem")

| Bahasa | Hasil |
|---|---|
| English | LULUS — tab Proyek, Alat, dan Setelan langsung berganti, termasuk teks keadaan kosong; tidak ada sisa bahasa Indonesia di layar yang dicek. 0 crash. |
| العربية (RTL) | LULUS — tata letak dan bilah bawah dicerminkan, 0 crash, 0 ANR. Dua catatan visual (LOW): chevron di daftar Alat tetap menghadap kanan di sisi kiri layar (tidak ikut dicerminkan), dan label tab "إعداد الذكاء الاص…" terpotong. |

Temuan lint `LocalContextResourcesRead` (`ui/UiText.kt:56`) tidak terbukti untuk teks statis pada English dan Arab. Skenario teks `UiText` yang sudah tampil lalu bahasa berubah tidak diuji. Bahasa Spanyol, Mandarin, Hindi, dan Rusia tidak diuji.

### Tes unit kripto backup (`BackupCryptoTest`)

Ditambah 7 tes (sebelumnya 5): bit ciphertext dibalik, bit tag dibalik, dua chunk ditukar, salt atau nonce prefix diubah, versi tidak didukung dan magic salah, salt/nonce unik per backup, serta password Unicode dan berspasi. Hasil: 12/12 lulus. Seluruh suite: 372 tes, 0 gagal, 0 error, 1 dilewati.

Probe sementara (dihapus, tidak di-commit) memastikan temuan E:
- Byte tambahan setelah chunk terakhir diterima begitu saja (data tetap terbaca utuh).
- File terpotong di tengah chunk memunculkan `EOFException`, bukan `WrongPasswordException`.
Keduanya belum diperbaiki dan tidak dikunci oleh tes permanen.

---

## Perbaikan temuan E (`BackupCrypto.decrypt`)

Temuan E (INFO) pada audit: byte tambahan setelah chunk terakhir diterima begitu saja, dan file yang terpotong di tengah chunk memunculkan `EOFException` (UI menampilkan pesan umum), bukan `WrongPasswordException`. Status sekarang: DIPERBAIKI.

Perubahan di `app/src/main/java/com/nextcraft/forge/data/BackupCrypto.kt` (`decrypt`, fungsi `next()`):
- Pembacaan isi chunk dibungkus `try/catch EOFException` → `WrongPasswordException` (pemotongan di tengah chunk kini dinilai sama dengan pemotongan di antara chunk).
- Setelah chunk terakhir berhasil dibuka (`done`), jika masih ada byte di aliran (`data.read() != -1`) → `WrongPasswordException`. Ini juga menolak backup kedua yang disambung di belakang backup pertama.

Tes baru di `BackupCryptoTest` (kini 14 tes):
- `bytesAfterTheFinalChunkAreRejected` — 7 byte tambahan, dan backup kedua yang disambung.
- `truncationInsideAChunkOrItsLengthIsWrongPassword` — terpotong di dalam ciphertext, di dalam 4 byte panjang, dan hanya header.

Verifikasi: `gradle :app:testDebugUnitTest --offline` → exit 0; `BackupCryptoTest` 14/14; seluruh suite 374 tes, 0 gagal, 0 error, 1 dilewati. Tes round-trip lama (multi-chunk, kosong, tepat satu chunk) tetap lulus, jadi backup yang sah tidak ditolak.

Batasan yang diketahui:
- Pemotongan di dalam 33 byte header (sebelum salt/nonce lengkap) masih memunculkan `EOFException`; tidak diubah.
- Panjang chunk yang diubah di luar rentang 16..(1 MiB+16) tetap `IllegalArgumentException("Damaged backup")`; ini disengaja karena pesannya informatif.
- Perubahan ini hanya diuji di tingkat `BackupCrypto` (JVM). Alur restore lewat UI (`BackupManager` + pemilih berkas) belum diuji di perangkat.

---

## Uji restore backup terenkripsi lewat UI (`x`)

Skenario: tema `x` diubah ke Putih, backup terenkripsi dibuat (Proyek, Chat, Setelan aktif; API key mati) dengan kata sandi berspasi `kata sandi uji 42`, tema dikembalikan ke Gelap, lalu backup direstore. Pemilih berkas adalah DocumentsUI sistem (penyimpanan di `Download`).

| Langkah | Hasil |
|---|---|
| Buat backup terenkripsi | LULUS — pesan "Backup tersimpan: 33 file, 79 KB"; berkas `.ncbak` 40.534 byte, header `NCBK` versi 1; tidak ada penanda `PK` atau teks pengaturan di isinya |
| Restore dengan kata sandi SALAH | LULUS — "Kata sandi salah, atau backup rusak."; 0 crash, 0 ANR |
| Restore tanpa kata sandi | LULUS — tombol Pulihkan nonaktif (`enabled = !restoreEncrypted \|\| restorePassword.isNotEmpty()`), dialog tetap terbuka |
| Restore dengan kata sandi BENAR | LULUS — "34 file dipulihkan. Mulai ulang app untuk menyelesaikan."; tombol "Mulai ulang sekarang" memulai proses baru (pid 26271 → 29055) |
| Pengaturan dari backup diterapkan | LULUS — setelah restart tema kembali Putih (nilai di dalam backup) |

Isi backup (didekripsi dengan `BackupCrypto` lewat tes JVM sementara yang tidak di-commit): `nextcraft-backup.json`, `prefs/nextcraft_preferences.xml` (hanya `runtime_setup_complete=true` dan `background_prompts_shown=true` sebagai boolean), dan 32 entri `workspaces/`. Total 34 entri.

Temuan UX (LOW/INFO):
- LOW — Tombol "Pulihkan" yang nonaktif tampak sama dengan yang aktif: `Text(..., color = tokens.dangerText)` di `ui/BackupScreen.kt` (dialog restore) memberi warna eksplisit sehingga warna nonaktif tidak berlaku. Pengguna menekan tombol tanpa umpan balik. (Dugaan dari kode dan screenshot; tidak diuji dengan pembaca layar.)
- LOW — Label kolom kata sandi di dialog restore berbunyi "Kata sandi (opsional)" padahal untuk backup terenkripsi wajib diisi.
- LOW — Pesan hasil ("Backup tersimpan…", "Kata sandi salah…", "34 file dipulihkan…") muncul di dasar layar yang panjang; setelah menekan "Buat backup" di bagian atas, pesan tidak terlihat tanpa menggulir.
- INFO — Jumlah file tidak konsisten: backup melaporkan 33 file, restore melaporkan 34 (arsip berisi 34 entri; manifest `nextcraft-backup.json` tampaknya dihitung hanya saat restore).

Kesalahan skrip uji (bukan bug aplikasi): ketukan di luar dialog menutupnya; satu ketukan salah koordinat membuka Setelan Gboard saat keyboard menggeser dialog; ketukan "buka kartu Recents" mengenai baris sakelar "Simpan perubahan otomatis" dan mematikannya (`auto_keep_changes` tidak ada di backup dan hanya ditulis oleh sakelar UI; thumbnail Recents sebelum ketukan masih menunjukkan aktif). Sakelar dan tema sudah dikembalikan.

Belum diuji: restore backup yang memuat API key; restore dengan `android_host_tools` atau `auto_approve_all_tools` bernilai true di dalam arsip (kode meresetnya, hanya dibaca, tidak dijalankan); restore backup tidak terenkripsi; backup otomatis terjadwal.

Pembersihan: berkas backup uji di `Download` dihapus, berkas sementara di `/data/local/tmp` dan `/tmp/bk` dihapus, tes JVM sementara dihapus.
