#!/usr/bin/env bash
# Installs an aarch64 Android NDK + SDK CMake 3.22.1 bridge for local builds (Google ships no linux-aarch64 NDK).
# Source: lzhiyong/termux-ndk release "android-ndk" (third-party build, see THIRD_PARTY_NOTICES.md).
# app/build.gradle.kts picks it up from /root/android-sdk/ndk and pins ndkVersion to the folder name.
# Usage: scripts/install-ndk-local.sh [--force]
#   NDK_SHA256=<hex>  optional: abort when the archive digest differs
#   SDK_ROOT=<dir>    optional: default /root/android-sdk
set -euo pipefail

NDK_URL="https://github.com/lzhiyong/termux-ndk/releases/download/android-ndk/android-ndk-r29-aarch64.tar.xz"
SDK_ROOT="${SDK_ROOT:-/root/android-sdk}"
NDK_DIR="$SDK_ROOT/ndk"
NDK_SHA256="${NDK_SHA256:-02e10e4ddfe8deaeb0bd0cf29d04c981ed5bc8a5d6b560ebb9e7661f472d684b}"

if [ "$(uname -m)" != "aarch64" ]; then
    echo "This archive is aarch64 only; on x86_64 install the official NDK via sdkmanager." >&2
    exit 1
fi

if ! command -v cmake >/dev/null 2>&1 || ! command -v ninja >/dev/null 2>&1; then
    DEBIAN_FRONTEND=noninteractive apt-get update -qq && DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends cmake ninja-build xz-utils
fi

ensure_cmake_bridge() {
    mkdir -p "$SDK_ROOT/cmake/3.22.1/bin"
    ln -sf "$(command -v cmake)" "$SDK_ROOT/cmake/3.22.1/bin/cmake"
    ln -sf "$(command -v ctest)" "$SDK_ROOT/cmake/3.22.1/bin/ctest"
    ln -sf "$(command -v cpack)" "$SDK_ROOT/cmake/3.22.1/bin/cpack"
    ln -sf "$(command -v ninja)" "$SDK_ROOT/cmake/3.22.1/bin/ninja"
    printf 'Pkg.Desc = CMake 3.22.1 (system aarch64 bridge)\nPkg.Revision = 3.22.1\n' > "$SDK_ROOT/cmake/3.22.1/source.properties"
}

if [ "${1:-}" != "--force" ] && [ -n "$(ls -A "$NDK_DIR" 2>/dev/null)" ]; then
    ensure_cmake_bridge
    echo "NDK already present in $NDK_DIR (use --force to reinstall)"
    exit 0
fi

# Download and extract in a fresh dir on the SDK disk (/tmp is a small tmpfs), never in the project tree.
mkdir -p "$SDK_ROOT"
work="$(mktemp -d "$SDK_ROOT/.ndk-install.XXXXXX")"
trap 'rm -rf "$work"' EXIT
echo "Downloading $NDK_URL"
curl -fL --retry 3 -o "$work/ndk.tar.xz" "$NDK_URL"

digest="$(sha256sum "$work/ndk.tar.xz" | cut -d' ' -f1)"
echo "sha256: $digest"
if [ -n "${NDK_SHA256:-}" ] && [ "$NDK_SHA256" != "$digest" ]; then
    echo "SHA-256 mismatch (expected $NDK_SHA256)" >&2
    exit 1
fi

mkdir "$work/x"
tar --no-same-owner -xJf "$work/ndk.tar.xz" -C "$work/x"
src="$(find "$work/x" -maxdepth 2 -name source.properties -printf '%h\n' | head -1)"
[ -n "$src" ] || { echo "source.properties not found in archive" >&2; exit 1; }

# Gradle expects <sdk>/ndk/<Pkg.Revision>.
rev="$(sed -n 's/^Pkg.Revision *= *//p' "$src/source.properties" | head -1)"
[ -n "$rev" ] || { echo "Pkg.Revision missing" >&2; exit 1; }
mkdir -p "$NDK_DIR"
rm -rf "${NDK_DIR:?}/$rev"
mv "$src" "$NDK_DIR/$rev"
ensure_cmake_bridge
sdk_uid_gid="$(stat -c '%u:%g' "$SDK_ROOT" 2>/dev/null || echo '0:0')"
chown -R "$sdk_uid_gid" "$NDK_DIR" "$SDK_ROOT/cmake" 2>/dev/null || true
chmod -R a+rX "$NDK_DIR" "$SDK_ROOT/cmake" 2>/dev/null || true
printf '%s\n' "$rev" > /.nextcraft-ndk-installed 2>/dev/null || true
echo "Installed NDK $rev -> $NDK_DIR/$rev"
