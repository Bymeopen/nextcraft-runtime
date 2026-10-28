# Security audit report - NextCraft Forge (x debug variant)

- Source: commit 6193b79, branch release/2026-10-28-autosync-off, clean worktree
- Method: cloudflare/security-audit-skill, profile `quick` (partial coverage)
- Run status: **incomplete** (reason: simplified unit plan, no canonical coverage ledger; see Gaps)
- Records: 1 confirmed, 1 needs_validation, 0 rejected (`findings.json` passes `validate-findings.cjs`)

## Confirmed

### restore-prefs-provider-base-url-not-reset - severity medium (likelihood low, impact high), confidence high

Restoring an unencrypted backup can redirect the provider base URL and send the user's API key to an attacker host.

Restore writes every key of `prefs/nextcraft_preferences.xml` from the archive into the live preferences and resets only `android_host_tools`, `auto_approve_all_tools` and `interactive_high_risk_approvals` (`data/BackupManager.kt:189-199, 276-280`). `provider_kind` and `provider_base_url` survive; `AppPreferences.loadProvider` returns them (`data/AppPreferences.kt:309-352`), `ProviderApiClient.attachAuthHeaders` sends the vault key to that URL and only refuses plain http to non-local hosts (`network/ProviderApiClient.kt:176-190, 231-265`), and `RuntimeBridge` exports the URL as `ANTHROPIC_BASE_URL` to the guest agent (`runtime/RuntimeBridge.kt:301, 306`).

Conditions: the victim restores an attacker-supplied backup (no password needed when unencrypted); the provider kind has `fixedBaseUrl=false` and the victim already holds its key.
Observed: a local JVM probe applied the restore's own parse and reset to a crafted file; the three flags were reset and `provider_base_url=https://attacker.example` survived. The remaining hops were read in source, not executed.
Fix: reset or drop every `provider_*` and agent-scoped endpoint key (`base_url`, `dsh_api`, `openrouter_order`) before `putAll`, or validate a restored base URL against the kind's default; add a unit test beside `PrefsXml.withPrivilegedReset`.

## Needs validation

### agent-binary-checksum-from-same-origin-manifest

`updateClaude` and `updateAgy` verify the downloaded agent binary against a checksum served by the same host as the binary, and the result runs as root in the chroot (or as the app uid under PRoot). Unresolved: whether the vendor manifest is signed, and whether vendor-origin trust is intended in this project's threat model. No severity is assigned.

## Checked, no candidate (as reported by the hunters)

- Host bridge request files are parsed as JSON and never reach a shell string; response paths cannot traverse; the guest is already root in the chroot, so the android-shell script is not a boundary the app enforces.
- Loopback gateways bind to 127.0.0.1 with a per-session random token compared in constant time; the MCP server is stdio; the web-preview fetch tool pins the resolved address and re-validates redirects.
- Root helpers: `RootPathGuard`, unmount-before-delete, `chown -h`, temp-file-then-`mv` writes, snapshot restore zip-slip checks.
- Backup restore: zip-slip blocked, entry and byte limits, name validation, doctype disabled in the XML parser, schedule and tool prefs only from encrypted backups.
- Network clients disable redirects, so credentials are not forwarded; error logs are in-memory and bounded; `allowBackup=false`; only `MainActivity` is exported.
- Runtime bundles carry in-app SHA-256 pins; the updater checks https, the APK hash, the signing certificate and a higher versionCode; workflows pin actions to commit SHAs and avoid `pull_request_target`.

## Gaps (this is partial coverage, not a clean bill of health)

- No canonical coverage ledger and no coverage-critic wave; units were hand-defined and hunted once.
- Only grepped or skimmed, not read in depth: `AntigravityRuntimeBridge.kt`, `DshRuntimeBridge.kt`, `GuestProcess.kt`, `NativeSpawnProcess.kt`, `ShellQuote.kt`, `RuntimeExecutionService.kt`, `WorkspaceCheckpoints.kt`, `SnapshotsController.kt`, `ChrootRunner.kt`, `RuntimeSetupService.kt`, `ApiKeyVault.kt` internals, the OAuth/PKCE handling in the three auth controllers, `AndroidAppInstallReceiver.kt`, `ChatImages.kt`, `MainActivity.kt` intent handling, `SilentSelfUpdate.install` (possible TOCTOU between fingerprint and install), rootfs download and archive extraction code, and npm transitive dependencies and postinstall scripts.
- `network/LocalNetwork.kt` was named in the brief but the hunter reported it missing; the brief's path may have been wrong.
- The needs_validation record was not independently re-verified; nothing was run on a device or the network.
