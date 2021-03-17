package ru.avdim.github

import aes.Base64Str
import aes.aesDecrypt
import aes.aesEncryptToBase64
import kotlin.test.Test
import kotlin.test.assertEquals

class TestAes {

  fun helper(value: String, key: String, encryptedCheck: Base64Str? = null) {
    val encrypted = value.aesEncryptToBase64(key)
    println("--------------------------")
    println("value: $value")
    println("encrypted: $encrypted")
    println("--------------------------")
    if (encryptedCheck != null) {
      assertEquals(encryptedCheck, encrypted)
    }
    val decrypted = encrypted.aesDecrypt(key)
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

  @Test
  fun testMultiline() {
    helper("""
      1
      2
    """.trimIndent(), "normal key",  Base64Str("73+XsYn4roOWJv+ZyWVKRA=="))
  }

  @Test
  fun testFastlaneJson() {
    // fake data (not secure)
    helper("""
      {
        "key_id": "ASDF445345DF45F",
        "issuer_id": "asd34324sdf-34sdf43-sdfsdf45435-45sdf3-asfdw453sdf34sdf",
        "key": "-----BEGIN FAKE KEY-----\nASD34\ncd/DEFDF34/WDFF3\ntDF445DF+c34DFDF++SDF/Go\nDF%d/2OP\n-----END FAKE KEY-----",
        "duration": 1200,
        "in_house": false
      }      
    """.trimIndent(), "normal key",
      Base64Str("i/esf9gHm3GuNgNjmQTWs9OEXN3R3oQy2YlR/JtE9wjCCd51OWJ1cP13pPoJ8OY2U1ld2rrNa/9DHAMKgrzpy/SXQT7zEcFaWx1UdwD31osOSYjqEtB6I5aQA4r7G2chq4aSKaH7b2qHZIyjH6XtoCokCt3nOULM21tqyoUuUKxcJpBOkkfZCm/TAK/w4CmffZxvWyCzxgWqXTbXpb+6Sdgr6Zxzx4z3ma9s7KWcVfW5CdMNZVU+BIKw9hXGILPq7Vn1H5m4o/8dN73LJAjlQLHcpAjTehrLD5tatkCEOo2i/iLHMxAbyg5AHYKV4kvnyHOrGTMhkC+vnNG+lkcIOYeGLyxnYQnYxAVJ7C7/ZjsmckGGwE26PZd91em/PQMi")
    )
  }

  @Test
  fun testFastlaneBadJson() {
    // fake data (not secure)
    helper("""
        {
          "issuer_id": "asd34324sdf-34sdf43-sdfsdf45435-45sdf3-asfdw453sdf34sdf",
          "key": "-----",
          "duration": 1200,
          "in_house": false
        }
    """.trimIndent(), "normal key",
      Base64Str("eDIk/7fFUytq9h7HhiVRRz99FGS+Ymo8MhQa/NexBIB0/IXPSwC5bJJuLQygAazLeIJ/2E9h8NWKe1A3MwXt6WiJATxATzmDVw9NDHpMQjouHRHEu8Gk5C9FWSnRvkQzAk99uR9Vj3inPySXdHfgnZgLqm4G1L3/PDIZXsHVndLos4l5ofB7FBQEqUe4LY4w")
    )
  }

}
