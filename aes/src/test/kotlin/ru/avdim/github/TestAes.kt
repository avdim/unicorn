package ru.avdim.github

import aes.Base64Str
import aes.decrypt
import aes.encryptToBase64
import kotlin.test.Test
import kotlin.test.assertEquals

class TestAes {



  @Test
  fun testSimpleEncryptDecrypt() {
    val initStr = "s"
    val key = "k"
    val encrypted = initStr.encryptToBase64(key)
    assertEquals(Base64Str("l1pqAB0Dj4ekZKUImsGKgA=="), encrypted)
    val decrypted = encrypted.decrypt(key)
    println("encrypted: $encrypted")
    println("decrypted: $decrypted")
    assertEquals(initStr, decrypted)
  }
}
