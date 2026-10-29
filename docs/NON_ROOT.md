# Perangkat tanpa root

NextCraft Forge hanya berjalan di perangkat **root** (Magisk, KernelSU, atau APatch): runtime Ubuntu 24.04 masuk lewat
`su -mm` + mount kernel + `chroot-exec.sh` (ADR-002).

Jalur cadangan tanpa root (PRoot, ADR-007) sudah **dibuang** dari proyek ini: mesin PRoot, `proot-exec.sh`,
`libproot.so`, `libproot_loader.so`, pilihan "Runtime engine" di Pengaturan, dan skrip pembangunnya. Aplikasi pada
perangkat tanpa root menampilkan layar "Root diperlukan" di onboarding.

Pengguna tanpa root memakai aplikasi terpisah `com.nextcraft.forge.proot`, yang dipelihara di luar proyek ini
(branch `proot-main` di repositori asal). Versi terakhir proyek ini yang masih memuat mesin PRoot ditandai tag
`proot-edition-last` (commit `3351dad`).

Bila rootfs lama masih punya berkas bantu PRoot (`/opt/nextcraft/bin/proot-fast`, shim `/usr/local/bin/proot`) atau
`runtime/bin/proot-exec.sh`, aplikasi menghapusnya saat skrip runtime dipasang ulang.
