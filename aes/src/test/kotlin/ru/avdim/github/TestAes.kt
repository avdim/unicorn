package ru.avdim.github

import aes.Base64Str
import aes.decrypt
import aes.encryptToBase64
import kotlin.test.Test
import kotlin.test.assertEquals

class TestAes {

  fun helperFull(value: String, key: String, encryptedCheck: Base64Str? = null) {
    val encrypted = value.encryptToBase64(key)
    println("value: $value")
    println("encrypted: $encrypted")
    if (encryptedCheck != null) {
      assertEquals(encryptedCheck, encrypted)
    }
    val decrypted = encrypted.decrypt(key)
    assertEquals(value, decrypted)
  }

  @Test
  fun testSimpleEncryptDecrypt() {
    helperFull("s", "k", Base64Str("l1pqAB0Dj4ekZKUImsGKgA=="))
  }

}
