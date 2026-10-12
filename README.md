# ZX Agent (Android)

Agent parental control untuk HP anak — konsumen API `/api/agent` dari web ZX Parenting (https://zxparenting.zomet.my.id).

## Fitur
- **Pairing**: masukkan deviceToken dari dashboard ortu (satu kali).
- **Ping loop**: foreground service, tiap 5 menit POST `aksi=ping` → terima aturan (sesi, curfew), permintaan GPS, zona.
- **Blokir** (fase 3): layar blokir saat sesi habis/curfew, Device Admin `lockNow` + anti-uninstall.
- **GPS** (fase 4): kirim posisi saat diminta (`aksi=lokasi`); geo-fence dihitung server.
- **Blokir per-app** (fase 5): Accessibility service deteksi app depan → overlay blokir utk app dalam daftar `appBlokir` dari server.

## Rilis Play Store
Keystore release **tidak committed ke repo** — disimpan di GitHub Secrets:
`KEYSTORE_B64` (`base64 -w0 rilis.keystore`), `KEYSTORE_PASSWORD`,
`KEYSTORE_ALIAS`, `KEYSTORE_KEY_PASSWORD`. Workflow rilis baca dari Secrets
dulu, fallback ke file repo (legacy).

Signature lama (v2.3.0 ke bawah) compromised — keystore lama pernah committed
publik. Mulai v2.4.0 pakai keystore baru (rotasi). User harus reinstall app
(signature beda = Android reject update dari versi lama).

## Kontrak API (web sudah live)
```
POST /api/agent
Header: x-device-token: <token>
Body:   {"aksi": "ping"|"lokasi"|"activity", "detail": {...}}
```

## Build & rilis
- WSL tak ada toolchain Android → compile dijajakan CI.
- **CI** (`.github/workflows/ci.yml`): `assembleDebug` tiap push — judge compile.
- **Rilis** (`rilis.yml`): workflow_dispatch + input `versi` → bump `alamat.json` → `bundleRelease` + `assembleRelease` (AAB utk Play + APK utk pasang langsung, keystore Secrets-first) → attach AAB & APK ke GitHub Release.

## Struktur
```
app/src/main/java/com/zxparenting/agent/
  PasangActivity.kt   # layar pairing (mockup layar 1)
  StatusActivity.kt   # status koneksi (mockup layar 2)
  BlokirActivity.kt   # layar blokir (mockup layar 3-4)
  AgentService.kt     # foreground service ping loop
  Api.kt              # klien /api/agent
  Simpanan.kt         # token & status lokal
  AdminReceiver.kt    # device admin
```
Mockup UI: repo `zx_parenting` → `design/agent-ui-mockup.html`.
