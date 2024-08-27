package io.github.dockyardmc.protocol.cryptography

import io.github.dockyardmc.player.PlayerEncryptionContext
import io.netty.buffer.ByteBuf
import io.netty.channel.*
import io.netty.handler.codec.MessageToMessageDecoder


class PacketDecryptionHandler(private val playerEncryptionContext: PlayerEncryptionContext): MessageToMessageDecoder<ByteBuf>() {

    private val encryptionBase = EncryptionBase(EncryptionUtil.getDecryptionCipherInstance(playerEncryptionContext))

    override fun decode(ctx: ChannelHandlerContext, msg: ByteBuf, out: MutableList<Any>) {
        if (!playerEncryptionContext.isConnectionEncrypted) {
            out.add(msg.retain())
            return
        }
        val byteBuf = encryptionBase.decrypt(ctx, msg.retain())
        out.add(byteBuf)
        msg.release()
    }
}