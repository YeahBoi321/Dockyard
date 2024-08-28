package io.github.dockyardmc.protocol.packets.play.serverbound

import io.github.dockyardmc.annotations.ServerboundPacketInfo
import io.github.dockyardmc.protocol.PacketProcessor
import io.github.dockyardmc.protocol.packets.ProtocolState
import io.github.dockyardmc.protocol.packets.ServerboundPacket
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext

@ServerboundPacketInfo(0x08, ProtocolState.PLAY)
class ServerboundChunkBatchReceived(var chunksPerTick: Float): ServerboundPacket {

    override fun handle(processor: PacketProcessor, connection: ChannelHandlerContext, size: Int, id: Int) {
    }

    companion object {
        fun read(buf: ByteBuf): ServerboundChunkBatchReceived {
            return ServerboundChunkBatchReceived(buf.readFloat())
        }
    }
}