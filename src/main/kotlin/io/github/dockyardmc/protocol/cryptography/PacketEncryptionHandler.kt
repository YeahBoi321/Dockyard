package io.github.dockyardmc.protocol.cryptography

import io.github.dockyardmc.player.PlayerEncryptionContext
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToByteEncoder

class PacketEncryptionHandler(private val playerEncryptionContext: PlayerEncryptionContext): MessageToByteEncoder<ByteBuf>() {

    private val encryptionBase = EncryptionBase(EncryptionUtil.getEncryptionCipherInstance(playerEncryptionContext))
    override fun encode(ctx: ChannelHandlerContext, msg: ByteBuf, out: ByteBuf) {
        if(!playerEncryptionContext.isConnectionEncrypted) { out.writeBytes(msg.retain()); return}

        encryptionBase.encrypt(msg.retain(), out)
        msg.release()
    }
}