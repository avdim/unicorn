package ru.avdim.github

import aes.Base64Str
import aes.decrypt
import aes.encryptToBase64
import kotlin.test.Test
import kotlin.test.assertEquals

class TestAes {

  fun helper(value: String, key: String, encryptedCheck: Base64Str? = null) {
    val encrypted = value.encryptToBase64(key)
    println("--------------------------")
    println("value: $value")
    println("encrypted: $encrypted")
    println("--------------------------")
    if (encryptedCheck != null) {
      assertEquals(encryptedCheck, encrypted)
    }
    val decrypted = encrypted.decrypt(key)
    assertEquals(value, decrypted)
  }

  @Test
  fun testSimpleEncryptDecrypt() {
    helper("s", "k", Base64Str("l1pqAB0Dj4ekZKUImsGKgA=="))
  }

  @Test
  fun testEmpty() {
    helper("", "k", Base64Str("ZvsXCY2EwX6S41f9DNCsGg=="))
    helper("s", "", Base64Str("5VPxCtEV5mpz7po4HTLRxA=="))
    helper("", "", Base64Str("aRnt2s+HosA/GTiIRUDkbQ=="))
  }

  @Test
  fun testNormal() {
    helper("normal value", "normal key", Base64Str("pkbSzGXaBo2KNnNd9htfnA=="))
  }

  @Test
  fun testBig() {
    helper(
      "test big big values sdfghfdsfghfdsfgdsafghfdsad dfsg sdg dsf gdsf dfs dsfg sdf fghfdsaf",
      "test big key sdfdvb sdf sdfg sdg dsfg sdg 4567u dsfg sdfg dsfg dsg sdfg dsfg dsfg sdg sdf ",
      Base64Str("H/zFZ/yaULG7LOu0pb7oy72rB8k1fayGtGvWYsy+4MNd84MCya8RKoH+ieTqcZEpT6LV9vSuvLLem3zbAuq6uhxCa9jkFoshIBx4tx6sZmqYbF+gcV7106rcXmSpkPtx")
    )
  }

}
