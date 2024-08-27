package io.github.dockyardmc.protocol.cryptography

import io.github.dockyardmc.player.PlayerEncryptionContext
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec

object EncryptionUtil {
    fun getDecryptionCipherInstance(playerEncryptionContext: PlayerEncryptionContext): Cipher {
        val cipher = Cipher.getInstance("AES/CFB8/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, playerEncryptionContext.sharedSecret, IvParameterSpec(playerEncryptionContext.sharedSecret!!.encoded))
        return cipher
    }
    fun getEncryptionCipherInstance(playerEncryptionContext: PlayerEncryptionContext): Cipher {
        val cipher = Cipher.getInstance("AES/CFB8/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, playerEncryptionContext.sharedSecret, IvParameterSpec(playerEncryptionContext.sharedSecret!!.encoded))
        return cipher
    }
}