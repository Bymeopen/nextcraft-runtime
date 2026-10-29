# NextCraft Forge

Aplikasi Android (Kotlin, Jetpack Compose) yang menjalankan lingkungan pengembangan dan agen AI di perangkat
(rootfs chroot/PRoot, terminal, build Gradle).

- minSdk 31, targetSdk 36, compileSdk 37, ABI `arm64-v8a`
- Build: `gradle :app:assembleDebug`; test: `gradle :app:testDebugUnitTest`
- Release: lihat [docs/RELEASING.md](docs/RELEASING.md) (butuh `FORGE_KEYSTORE_*`); `-PrequireNdk` mewajibkan NDK
- Arsitektur: [ARCHITECTURE.md](ARCHITECTURE.md); izin: [docs/PERMISSIONS.md](docs/PERMISSIONS.md)
- Lisensi: [LICENSE](LICENSE), pihak ketiga: [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md)

## Lisensi dan pihak ketiga

- Kode NextCraft Forge: `GPL-3.0-only` ([LICENSE](LICENSE)). Sebagian file adalah turunan Mobile Harness (MIT); daftar lengkap komponen pihak ketiga, termasuk PRoot/talloc, dependensi Maven, dan bundle runtime, ada di [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md). Metadata per file: [REUSE.toml](REUSE.toml).
- Distribusi resmi hanya lewat GitHub Releases. Kode sumber untuk setiap rilis ada di tag yang sama.
- Claude Code, Antigravity CLI, dan DeepSeek Harness adalah perangkat lunak pihak ketiga dengan syarat penggunaan masing-masing vendor. Proyek ini tidak berafiliasi dengan, disponsori, atau didukung oleh Anthropic, Google, DeepSeek, Canonical, atau Termux. Semua nama dan merek dagang milik pemiliknya.
- Anda bertanggung jawab mematuhi syarat layanan vendor tersebut (akun, API key, batas penggunaan) saat memakainya lewat aplikasi ini.
