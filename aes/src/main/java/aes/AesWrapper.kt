package aes

import com.soywiz.krypto.encoding.base64
import com.soywiz.krypto.encoding.fromBase64

private object AesWrapper {

  val aes = Aes()

  fun encryptToBase64(strToEncrypt: String, secretKey: String): String {
    val encrypted = aes.encrypt(strToEncrypt.toByteArray(), AesStringSecret(secretKey))
    return encrypted.base64
  }

  fun decryptFromBase64(secretKey: String, strToDecrypt: String): String {
    try {
      val decrypt = aes.decrypt(strToDecrypt.fromBase64(), AesStringSecret(secretKey))
      return String(decrypt)
    } catch (t: Throwable) {
      return "decrypt fail with exception"
    }
  }

}

fun String.aesEncryptToBase64(key: String): Base64Str {
  val result = Base64Str(
    AesWrapper.encryptToBase64(this, key)
  )
  val check = result.aesDecrypt(key)
  if (this != check) {
    throw IllegalStateException("aesEncryptToBase64: this != check")
  }
  return result
}

fun Base64Str.aesDecrypt(key: String): String =
  AesWrapper.decryptFromBase64(key, this.str)

data class Base64Str(val str: String) {
  override fun toString(): String {
    return """Base64Str("$str")"""
  }
}
