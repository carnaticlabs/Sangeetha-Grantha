package com.sangita.grantha.backend.api.support

import com.password4j.Argon2Function
import com.password4j.Password
import com.password4j.types.Argon2

/**
 * Central password hashing for the backend (TRACK-114, north-star finding N1).
 *
 * Uses **argon2id** with self-describing PHC-format output
 * (`$argon2id$v=19$m=...,t=...,p=...$<salt>$<hash>`), so the salt and cost
 * parameters travel with every stored hash and no separate columns are needed.
 *
 * The same helper is used by the (future) interactive login path and by the
 * env-driven admin bootstrap in TRACK-110, so both produce byte-identical
 * hash formats.
 *
 * **Scope note:** the interactive login that would call [verify] /
 * [verifyAllowingLegacy] (and thus exercise rehash-on-login) does not exist yet
 * — today login is shared-admin-token gated and verifies no password. Building
 * that login, login throttling, and the shared-token/roles hardening is
 * deferred to the OAuth/OTP track (TRACK-119). This helper only guarantees that
 * nothing is stored in plaintext at rest.
 *
 * ### Cost parameters
 * Per OWASP's argon2id minimum (memory ≥ 19 MiB, t ≥ 2, p = 1):
 * - memory:      19456 KiB (19 MiB)
 * - iterations:  2
 * - parallelism: 1
 * - hash length: 32 bytes
 * password4j generates a cryptographically-secure random salt per hash.
 */
object PasswordHasher {
    private const val MEMORY_KIB = 19_456
    private const val ITERATIONS = 2
    private const val PARALLELISM = 1
    private const val HASH_LENGTH = 32

    private val argon2: Argon2Function =
        Argon2Function.getInstance(MEMORY_KIB, ITERATIONS, PARALLELISM, HASH_LENGTH, Argon2.ID)

    /** Hash [plaintext] with argon2id into a self-describing PHC string. */
    fun hash(plaintext: String): String =
        Password.hash(plaintext).with(argon2).result

    /** True if [stored] is an argon2 PHC hash (vs a legacy plaintext value). */
    fun isArgon2Hash(stored: String): Boolean = stored.startsWith("\$argon2")

    /** True if [stored] is a legacy plaintext credential that predates argon2id. */
    fun isLegacyPlaintext(stored: String): Boolean = !isArgon2Hash(stored)

    /**
     * Verify [plaintext] against an argon2id [stored] hash. Returns false for
     * legacy plaintext records — use [verifyAllowingLegacy] when the caller is
     * prepared to transparently rehash a legacy credential.
     */
    fun verify(plaintext: String, stored: String): Boolean =
        isArgon2Hash(stored) && Password.check(plaintext, stored).with(argon2)

    /** Outcome of a verification that tolerates legacy plaintext records. */
    data class VerifyResult(val verified: Boolean, val needsRehash: Boolean)

    /**
     * Verify [plaintext] against [stored], accepting either an argon2id hash or
     * a legacy plaintext value. When a legacy value verifies, [VerifyResult.needsRehash]
     * is true so the caller can transparently re-store an argon2id hash
     * (rehash-on-login). Wiring this into an actual login is TRACK-119 work.
     */
    fun verifyAllowingLegacy(plaintext: String, stored: String): VerifyResult =
        if (isArgon2Hash(stored)) {
            VerifyResult(verified = Password.check(plaintext, stored).with(argon2), needsRehash = false)
        } else {
            VerifyResult(verified = stored == plaintext, needsRehash = true)
        }
}
