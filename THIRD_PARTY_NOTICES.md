# Third-Party Notices & Conceptual Acknowledgements

## 1. Licensing of NextCraft Forge (`com.nextcraft.forge`)
**NextCraft Forge** as a combined work is distributed under the **GNU General Public License v3.0 only** (`LICENSE`, SPDX: `GPL-3.0-only`), because it links the GPLv3 Termux terminal emulator libraries listed below.

NextCraft Forge began as a derivative of **Mobile Harness** (`techjarves/Mobile-Harness`, MIT; see Section 3) and has since been extensively modified and extended. A file-level comparison against upstream commit `96b1da0` found 36 same-named Kotlin files sharing 4,188 of 54,763 non-test lines (~7.6%, after normalising the package name), with no file identical and the closest at ~75% (`DshRuntimeBridge.kt`). The native JNI spawner `forge_spawn.c` also shares lines with upstream `pocket_spawn.c`. The remaining code, the Forge MCP server, and the Reference Index (Section 4) are original work of the NextCraft Forge authors.

| Component | License | Notice Location |
| :--- | :--- | :--- |
| **NextCraft Forge (combined work)** | `GPL-3.0-only` | `LICENSE` |
| **Termux `terminal-emulator` & `terminal-view` (`v0.118.0`)** | `GPL-3.0-only` (Apache-2.0 portions by Jack Palevich) | Section 2 below |
| **Mobile Harness (upstream of derived files + MIT Notice)** | `MIT` | Section 3 below |
| **XERJ (`xerj-org/xerj` — Conceptual Inspiration for Reference Indexing)** | Conceptual Inspiration | Section 4 below |
| **Maven runtime dependencies** | `Apache-2.0`, `BSD-2-Clause` | Section 6 below |
| **Runtime bundles downloaded on first run** | various (see table) | Section 7 below |

---

## 2. Termux Terminal Libraries (`GPL-3.0-only`)
- **Component:** `com.github.termux.termux-app:terminal-emulator` and `com.github.termux.termux-app:terminal-view`, version `v0.118.0` (https://github.com/termux/termux-app), integrated for interactive PTY sessions (`ForgeTerminalClients.kt`, `TerminalScreen.kt`).
- **License:** The `termux/termux-app` repository is released under **GNU GPLv3 only** (https://www.gnu.org/licenses/gpl-3.0.html), with portions derived from [Terminal Emulator for Android](https://github.com/jackpal/Android-Terminal-Emulator) by Jack Palevich under the Apache License 2.0 (https://www.apache.org/licenses/LICENSE-2.0).
- **Distribution Requirement:** Any binary or source distribution of NextCraft Forge must remain under **GPL-3.0-only** accompanied by complete corresponding source code and this notice. See `docs/TERMINAL.md`.
- **Copyright:** The Termux contributors; Jack Palevich for the Android-Terminal-Emulator portions.

---

## 3. Mobile Harness (`techjarves/Mobile-Harness` — Upstream of Derived Files)
- **Acknowledgement:** Parts of NextCraft Forge are derived from **Mobile Harness** (https://github.com/techjarves/Mobile-Harness), including the runtime bridges, installer/setup services, API-key vault, loopback format gateways, GitHub client, Markdown renderer, and the native spawner, which were renamed from `com.jarves.mh` to `com.nextcraft.forge` and then modified. The upstream MIT copyright and permission notice is retained in full below, as the MIT License requires for copies or substantial portions of the Software. MIT is compatible with distributing the combined work under GPL-3.0-only.

```text
MIT License

Copyright (c) 2026 Mobile Harness Contributors

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

---

## 4. XERJ (`xerj-org/xerj` — Conceptual Inspiration)
- **Acknowledgement:** The **Forge Reference Index (`forge_reference_index.py`)** and its 5 MCP tools (`forge_reference_autoindex`, `forge_reference_search`, `forge_reference_def`, `forge_reference_batch_search`, `forge_reference_gain`) were written 100% from scratch in Python 3 using standard-library `sqlite3` (FTS5 BM25) and custom regex symbol extractors. We credit **`xerj-org/xerj`** purely as conceptual inspiration for equipping coding agents with local BM25 passage search, symbol definition lookup, and token-efficiency telemetry. No source code, schema, or assets from `xerj` are used or distributed in NextCraft Forge.

---

## 5. PRoot and talloc (no longer bundled)
Earlier builds shipped **PRoot v5.5.0** (`GPL-2.0-or-later`) statically linked with **talloc 2.4.3** (`LGPL-3.0-or-later`) as `libproot.so` and `libproot_loader.so` for a non-root fallback engine. That engine, the binaries, and `scripts/runtime-bundles/build-proot-static.sh` were removed; NextCraft Forge is now root-only and does not distribute PRoot or talloc. Releases built before the removal keep the obligations of those licenses; their source is the upstream tag `v5.5.0` of `proot-me/proot` and `talloc-2.4.3.tar.gz` from samba.org.

---

## 6. Maven Runtime Dependencies
Licenses below were read from the artifacts' POM metadata (`<license>` element).

| Artifact | Version | License |
| :--- | :--- | :--- |
| `androidx.*` (core-ktx, work, appcompat, lifecycle, activity-compose, Compose BOM libraries incl. material3) | see `app/build.gradle.kts` | `Apache-2.0` |
| `com.google.android.material:material` | `1.14.0` | `Apache-2.0` |
| `org.jetbrains.kotlinx:kotlinx-coroutines-core` / `-android` | `1.11.0` | `Apache-2.0` |
| `org.apache.commons:commons-compress` | `1.28.0` | `Apache-2.0` |
| `io.github.kyant0:backdrop` | `2.0.1` | `Apache-2.0` |
| `com.github.luben:zstd-jni` | `1.5.7-24` | `BSD-2-Clause` |

The Gradle Wrapper (`gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar`) is `Apache-2.0`, copyright the original Gradle authors.

---

## 7. Runtime Bundles (not inside the APK; downloaded on first run)
The APK contains only the app, `libforgespawn.so`, and scripts. The first-run installer downloads the `.tar.zst` bundles below (Core, Android, Python, DeepSeek) from the project release `https://github.com/bymeopen/nextcraft-forge/releases/download/runtime-2026.10.2` (`RuntimeInstaller.kt`, `RuntimeBundle`), verified by pinned SHA-256. Hosting them there is **redistribution** by this project. Builders live in `scripts/runtime-bundles/`.

| Bundle | Contents (per build script) | License / status |
| :--- | :--- | :--- |
| `nextcraft-core-arm64` | Ubuntu Base 24.04.5 (`ubuntu-base-24.04.5-base-arm64.tar.gz`), Node.js `v24.22.1`, `git curl wget unzip zip xz-utils zstd jq ripgrep` | Ubuntu packages: GPL/LGPL/others, per-package copyright in `/usr/share/doc/*/copyright` inside the rootfs. Node.js: MIT. Corresponding source for GPL/LGPL packages is available from Ubuntu (`archive.ubuntu.com`); keep this reference with any redistribution. |
| `nextcraft-android-arm64` | Temurin JDK `21.0.12.1+1`, Gradle `9.8.1`, Android SDK Platform `37.2`, Build-Tools `36.0.0` (Java parts from Google; native `aapt`, `aapt2`, `aidl`, `zipalign`, ... from `lzhiyong/android-sdk-tools` `35.0.2`), offline Maven repository | JDK: GPL-2.0 with Classpath Exception. Gradle: Apache-2.0. Android SDK components: Android Software Development Kit License (Google). `lzhiyong/android-sdk-tools`: Apache-2.0 (GitHub repository metadata). Maven repository: per-artifact licenses. |
| `nextcraft-python-arm64` | Python runtime (`build-python-bundle.sh`) | Overlay of the files `apt-get install python3 python3-pip python3-venv` adds to the Core rootfs (Ubuntu noble, Python 3.12): PSF License for CPython, plus the licenses of the other Ubuntu packages (see Core row). Not itemised per package. |
| `nextcraft-dsh-arm64` | DeepSeek harness `dsh` `0.2.1-rc.2` | npm package `@deepseek-ai/dsh` (repo `deepseek-ai/deepseek-harness`) declares `MIT` for the top-level package; its npm dependencies are resolved at build time and their licenses are **not itemised**. |

**Not bundled (downloaded from the vendor):** Claude Code (`downloads.claude.ai`, SHA-256 from the vendor manifest) and Antigravity CLI (`storage.googleapis.com/antigravity-public`, SHA-512 from the vendor manifest). Both are proprietary (Claude Code: `© Anthropic PBC. All rights reserved`, use subject to Anthropic's legal agreements); this project neither builds nor hosts their binaries, and users accept the vendors' terms when they sign in and use them.

Additional tools installed by the user inside the guest through `apt`/`pip`/`npm` (for example `qemu-user`, `frida`, `unicorn`, `openssh-client`, `gh` v2.100.0) are fetched from their official sources and are not redistributed by this project.

---

## 8. Distribution Compliance (GitHub Releases only)
- **Channel:** APK and runtime bundles are distributed only through GitHub Releases of `bymeopen/nextcraft-forge`; the app is not published on Google Play. GPL obligations apply to GitHub distribution just as to any other channel.
- **Source offer (GPL-3.0):** The complete corresponding source of NextCraft Forge is the repository at the release tag. Source for the Ubuntu packages inside the Core/Python bundles is available from `archive.ubuntu.com` (Ubuntu 24.04 `noble`). Each release must attach or link `LICENSE` and this file.
- **Termux terminal libraries (GPL-3.0):** linked into the APK; the APK is therefore GPL-3.0-only and must be accompanied by this notice and the source (see Section 2).
- **Trademarks:** Claude, Claude Code, Anthropic, Antigravity, Google, DeepSeek, Ubuntu, Canonical, Termux, and Android are trademarks of their respective owners. This project is independent and unaffiliated; names are used only to identify compatibility.
- **Proprietary third-party agents (resolved):** Claude Code and Antigravity CLI are not re-hosted. The installer downloads them directly from the vendors' official URLs and verifies checksums (`installClaudeRelease`, `installAgyRelease`), so no redistribution permission is needed. The earlier project-hosted bundles `nextcraft-claude-arm64-2026.10.2.tar.zst` and `nextcraft-agy-arm64-2026.10.2.tar.zst` were removed from the `runtime-2026.10.2` GitHub release; both URLs returned HTTP 404 on 2026-10-29. Do not re-introduce project-hosted bundles of these binaries; `OfficialAgentSourcesTest` guards the source tree.
- **REUSE compliance:** `reuse lint` (REUSE Specification 3.3, tool v6.2.0) reports the project compliant: every tracked file has copyright and license information via `REUSE.toml` (524 of 524 when checked), and the texts of `Apache-2.0`, `GPL-2.0-or-later`, `GPL-3.0-only`, `LGPL-3.0-or-later` and `MIT` are in `LICENSES/`. Re-run it when files or dependencies change.
- **Still open (not blocking a beta):** licenses of the transitive npm dependencies of `@deepseek-ai/dsh` and of each Ubuntu package inside the Core/Python bundles are not itemised here.
- **User-supplied credentials:** The app stores and uses API keys and accounts supplied by the user; users must comply with each provider's terms of service.
