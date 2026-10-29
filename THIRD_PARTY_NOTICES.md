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
| **PRoot `v5.5.0` (bundled `libproot.so`, `libproot_loader.so`)** | `GPL-2.0-or-later` | Section 5 below |
| **talloc `2.4.3` (statically linked into PRoot)** | `LGPL-3.0-or-later` | Section 5 below |
| **Maven runtime dependencies** | `Apache-2.0`, `BSD-2-Clause` | Section 6 below |

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

## 5. PRoot and talloc (bundled native binaries)
- **Component:** `app/src/main/jniLibs/arm64-v8a/libproot.so` and `libproot_loader.so`, static ARM64 build of **PRoot v5.5.0** (https://github.com/proot-me/proot), statically linked with **talloc 2.4.3** (https://www.samba.org/ftp/talloc/talloc-2.4.3.tar.gz). Built reproducibly by `scripts/runtime-bundles/build-proot-static.sh`, which pins the source versions and the talloc SHA-256.
- **License:** PRoot is `GPL-2.0-or-later` (the binary reports "licensed under GPL v2 or later"); talloc is `LGPL-3.0-or-later`. Both are compatible with GPL-3.0-only distribution.
- **Distribution Requirement:** Ship the corresponding source (or a written offer) for PRoot and talloc together with any binary distribution. The build script above plus the pinned upstream tag/tarball satisfy this for the unmodified sources.
- **Copyright:** The PRoot Developers; the Samba Team (talloc).

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
