#!/usr/bin/env bash
# Installs an aarch64 Android NDK for local builds (Google ships no linux-aarch64 NDK).
# Source: lzhiyong/termux-ndk release "android-ndk" (third-party build, see THIRD_PARTY_NOTICES.md).
# app/build.gradle.kts picks it up from /root/android-sdk/ndk and pins ndkVersion to the folder name.
# Usage: scripts/install-ndk-local.sh [--force]
#   NDK_SHA256=<hex>  optional: abort when the archive digest differs
#   SDK_ROOT=<dir>    optional: default /root/android-sdk
set -euo pipefail

NDK_URL="https://github.com/lzhiyong/termux-ndk/releases/download/android-ndk/android-ndk-r29-aarch64.tar.xz"
SDK_ROOT="${SDK_ROOT:-/root/android-sdk}"
NDK_DIR="$SDK_ROOT/ndk"

if [ "$(uname -m)" != "aarch64" ]; then
    echo "This archive is aarch64 only; on x86_64 install the official NDK via sdkmanager." >&2
    exit 1
fi
if [ "${1:-}" != "--force" ] && [ -n "$(ls -A "$NDK_DIR" 2>/dev/null)" ]; then
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
tar -xJf "$work/ndk.tar.xz" -C "$work/x"
src="$(find "$work/x" -maxdepth 2 -name source.properties -printf '%h\n' | head -1)"
[ -n "$src" ] || { echo "source.properties not found in archive" >&2; exit 1; }

# Gradle expects <sdk>/ndk/<Pkg.Revision>.
rev="$(sed -n 's/^Pkg.Revision *= *//p' "$src/source.properties" | head -1)"
[ -n "$rev" ] || { echo "Pkg.Revision missing" >&2; exit 1; }
mkdir -p "$NDK_DIR"
rm -rf "${NDK_DIR:?}/$rev"
mv "$src" "$NDK_DIR/$rev"
echo "Installed NDK $rev -> $NDK_DIR/$rev"
