# `.etbackup` — Encrypted Backup Format v1

**Status:** Stable (v1)
**Implemented by:** `app/src/main/java/dev/tuandoan/expensetracker/data/backup/BackupCrypto.kt`
**First shipped:** v3.9.0 (planned)

## TL;DR

`.etbackup` is a self-contained binary container for an Expense Tracker JSON backup encrypted with a user password. It uses **AES-256-GCM** for authenticated encryption and **PBKDF2-HMAC-SHA256** for key derivation. Wrong-password and tampering are indistinguishable to the caller — both surface as `WrongPassword`. Unknown versions are rejected.

## Byte layout

```
+----------+-----------+---------+---------+-------------------------------+
| 0..3 (4) | 4 (1)     | 5..20   | 21..32  | 33..N                         |
| magic    | version   | salt    | IV      | ciphertext || GCM auth tag    |
| "ETBK"   | 0x01 (v1) | 16 B    | 12 B    | len(plaintext) + 16 B tag     |
+----------+-----------+---------+---------+-------------------------------+
```

Fixed header length: **33 bytes**. Total file size: `33 + len(plaintext) + 16`.

| Offset | Length | Field      | Value / format                                      |
|-------:|-------:|------------|-----------------------------------------------------|
|     0  |     4  | magic      | ASCII `ETBK` — `0x45 0x54 0x42 0x4B`                |
|     4  |     1  | version    | Unsigned byte, current = `0x01`                     |
|     5  |    16  | salt       | CSPRNG-random, input to PBKDF2                      |
|    21  |    12  | IV / nonce | CSPRNG-random, input to AES-GCM                     |
|    33  |  rest  | body       | AES-256-GCM ciphertext with appended 128-bit tag    |

All multi-byte fields are raw bytes — no endianness applies.

## Crypto parameters

### Key derivation — PBKDF2-HMAC-SHA256

| Parameter   | Value                                      |
|-------------|--------------------------------------------|
| Algorithm   | `PBKDF2WithHmacSHA256` (JCE name)          |
| Iterations  | `200_000`                                  |
| Salt length | 16 bytes, per-file, random                 |
| Output key  | 256 bits (32 bytes)                        |

### Symmetric encryption — AES-256-GCM

| Parameter   | Value                                      |
|-------------|--------------------------------------------|
| Algorithm   | `AES/GCM/NoPadding` (JCE name)             |
| Key size    | 256 bits                                   |
| IV (nonce)  | 12 bytes, per-file, random                 |
| AAD         | None                                       |
| Tag size    | 128 bits, appended to ciphertext           |

The 16-byte GCM authentication tag is appended to the ciphertext by the JCE — it is not a separate field in the header.

## Version policy

- The version byte is the **only** forward-compat hook.
- A decoder that reads a version it does not support MUST fail fast with `UnsupportedVersion(version)` — no "best effort" parsing.
- Any breaking change to layout, KDF parameters, cipher, tag size, or AAD MUST bump the version and should be accompanied by a separate decoder branch.
- v1 is frozen: once shipped, its parameters do not change.

## Error cases

The reference implementation surfaces four distinct errors:

| Exception             | Trigger                                                              | User-facing meaning             |
|-----------------------|----------------------------------------------------------------------|---------------------------------|
| `MalformedHeader`     | File shorter than 33 bytes, or magic ≠ `ETBK`                        | Not an `.etbackup` file         |
| `UnsupportedVersion`  | Magic OK but version byte is not a known value                       | Created by a newer/older build  |
| `WrongPassword`       | GCM tag fails (`AEADBadTagException`) — wrong password OR tampering  | Incorrect password              |
| `DecryptionFailed`    | Any other unexpected JCE/crypto failure                              | Generic backup-decrypt failure  |

Wrong password and tampered ciphertext are intentionally indistinguishable: revealing which failed would leak oracle information. Both are reported as `WrongPassword`.

## Encrypt / decrypt pseudocode

### Encrypt

```text
salt  := random(16)
iv    := random(12)
key   := PBKDF2-HMAC-SHA256(password, salt, iterations=200_000, keyLen=256)
body  := AES-256-GCM-Encrypt(key, iv, plaintext)      // returns ciphertext || tag(16)
output := "ETBK" || 0x01 || salt || iv || body
zero(key)
```

### Decrypt

```text
assert len(input) >= 33
assert input[0..4] == "ETBK"                          // else MalformedHeader
assert input[4] == 0x01                               // else UnsupportedVersion
salt := input[5..21]
iv   := input[21..33]
key  := PBKDF2-HMAC-SHA256(password, salt, iterations=200_000, keyLen=256)
try:
    plaintext := AES-256-GCM-Decrypt(key, iv, input[33..])
except AEADBadTag:
    throw WrongPassword
finally:
    zero(key)
```

Implementations SHOULD derive the key on a worker thread — PBKDF2 at 200k iterations takes tens to hundreds of milliseconds on typical mobile CPUs.

## Threat model

**In scope:**

- Confidentiality of the encrypted payload against anyone without the password, including someone with read access to the user's device storage, cloud sync, or a shared backup folder.
- Integrity of the full file — any bit flip in body, IV, salt, or version byte is caught by GCM (for body/IV/key) or by strict header checks (for magic/version).

**Out of scope:**

- Protection against a compromised device while the user is entering the password. Keyloggers, screen-capture malware, and rooted/jailbroken inspection are not defended against.
- Forward secrecy — a leaked password reveals every past and future backup encrypted with that password.
- Password strength — enforcement lives in the UI layer (Task 2.11). This format is only as strong as the password chosen.
- Side-channels (timing, cache) from the stock JCE provider.

## Memory hygiene (reference implementation)

- The raw password `CharArray` is never mutated by `BackupCrypto` — callers keep ownership.
- `PBEKeySpec.clearPassword()` is called in a `finally` block so the spec's internal copy is wiped.
- The raw PBKDF2 output (`keyBytes`) is zeroed in `finally` before `SecretKeySpec`'s reference is released to the GC.
- Best-effort only: the JVM may have copied bytes into internal buffers that cannot be zeroed from user code.

## References

- Implementation: `app/src/main/java/dev/tuandoan/expensetracker/data/backup/BackupCrypto.kt`
- Exceptions: `app/src/main/java/dev/tuandoan/expensetracker/data/backup/BackupCryptoException.kt`
- Tests: `app/src/test/java/dev/tuandoan/expensetracker/data/backup/BackupCryptoTest.kt`
- Initial delivery: PR #74 (2026-04-19)
- Hardening follow-up: PR #75 (2026-04-20)
- NIST SP 800-38D — AES-GCM
- NIST SP 800-132 — PBKDF2 recommendations
