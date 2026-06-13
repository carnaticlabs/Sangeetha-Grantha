package com.sangita.grantha.backend.api.support

import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test

class PasswordHasherTest {

    @Test
    fun `hash output is not the plaintext`() {
        val hashed = PasswordHasher.hash("correct horse battery staple")
        assertNotEquals("correct horse battery staple", hashed)
        assertTrue(hashed.startsWith("\$argon2id\$"), "expected argon2id PHC format, got: $hashed")
    }

    @Test
    fun `hashing the same password twice yields different hashes (random salt)`() {
        val a = PasswordHasher.hash("same-password")
        val b = PasswordHasher.hash("same-password")
        assertNotEquals(a, b)
    }

    @Test
    fun `verify accepts the correct password`() {
        val hashed = PasswordHasher.hash("s3cr3t-passphrase")
        assertTrue(PasswordHasher.verify("s3cr3t-passphrase", hashed))
    }

    @Test
    fun `verify rejects the wrong password`() {
        val hashed = PasswordHasher.hash("s3cr3t-passphrase")
        assertFalse(PasswordHasher.verify("not-the-password", hashed))
    }

    @Test
    fun `verify rejects a legacy plaintext stored value`() {
        // A plaintext credential must never satisfy the strict argon2 verify path.
        assertFalse(PasswordHasher.verify("plain-pw", "plain-pw"))
    }

    @Test
    fun `legacy plaintext is detected, argon2 hash is not`() {
        assertTrue(PasswordHasher.isLegacyPlaintext("plain-pw"))
        assertFalse(PasswordHasher.isLegacyPlaintext(PasswordHasher.hash("plain-pw")))
        assertTrue(PasswordHasher.isArgon2Hash(PasswordHasher.hash("x")))
    }

    @Test
    fun `verifyAllowingLegacy verifies an argon2 hash without requesting rehash`() {
        val hashed = PasswordHasher.hash("modern-pw")
        val result = PasswordHasher.verifyAllowingLegacy("modern-pw", hashed)
        assertTrue(result.verified)
        assertFalse(result.needsRehash)
    }

    @Test
    fun `verifyAllowingLegacy verifies a legacy plaintext and requests rehash`() {
        val result = PasswordHasher.verifyAllowingLegacy("legacy-pw", "legacy-pw")
        assertTrue(result.verified)
        assertTrue(result.needsRehash)
    }

    @Test
    fun `verifyAllowingLegacy rejects a wrong legacy plaintext`() {
        val result = PasswordHasher.verifyAllowingLegacy("wrong", "legacy-pw")
        assertFalse(result.verified)
        assertTrue(result.needsRehash)
    }
}
