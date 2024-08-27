package io.github.dockyardmc.protocol

import cz.lukynka.Bindable
import cz.lukynka.prettylog.LogType
import cz.lukynka.prettylog.log
import io.github.dockyardmc.extentions.*
import io.github.dockyardmc.player.PlayerEncryptionContext
import io.github.dockyardmc.player.ProfilePropertyMap
import io.github.dockyardmc.protocol.packets.ProtocolState
import io.github.dockyardmc.protocol.packets.ServerboundPacket
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import java.lang.Exception
import java.nio.BufferUnderflowException
import java.util.zip.DataFormatException
import java.util.zip.Inflater

class PlayerSocketConnection(): ChannelInboundHandlerAdapter() {

    val state: Bindable<ProtocolState> = Bindable(ProtocolState.HANDSHAKE)
    val encryption: PlayerEncryptionContext? = null

    private val username: String? = null
    private val gameProfile: ProfilePropertyMap? = null
    private val serverAddress: String? = null
    private val serverPort = 0
    private val protocolVersion = 0

    val compressed: Boolean = false

    fun readPacket(buffer: ByteBuf): Pair<Int?, ByteBuf> {
        var remaining: ByteBuf? = null

        while(buffer.readableBytes() > 0) {
            try {
                val packetSize = buffer.readVarInt()
                val readerStart = buffer.readerIndex()
                if (buffer.readableBytes() < packetSize) throw BufferUnderflowException()

                var content: ByteBuf = buffer
                var decompressedSize = packetSize
                if (compressed) {
                    val dataLength = buffer.readVarInt()
                    val payloadLength = packetSize - (buffer.readerIndex() - readerStart)
                    if (payloadLength < 0) throw DataFormatException("Negative payload size: $payloadLength")
                    if (dataLength == 0) {
                        decompressedSize = payloadLength
                    } else {
                        content = buffer.wrap()
                        decompressedSize = dataLength
                        val inflater = Inflater()
                        inflater.setInput(buffer.asByteBuffer(buffer.readerIndex(), payloadLength))
                        inflater.inflate(content.asByteBuffer(0, dataLength))
                        inflater.reset()
                    }
                }

                val payload = content.asByteBuffer(content.readerIndex(), decompressedSize).toByteBuf()
                val packetId = payload.readVarInt()

                return packetId to payload

            } catch (ex: Exception) {
                buffer.resetReaderIndex()
                remaining = buffer.copy()
                log(ex)
                break
            }
        }
        return null to remaining!!
    }


    override fun channelRead(connection: ChannelHandlerContext, msg: Any?) {

        val buffer = msg as ByteBuf

        try {
            while (buffer.isReadable) {
                val data = readPacket(buffer)
                if(data.first == null) throw Exception("packet id is null")

                val packet: ServerboundPacket? = try {
                    PacketParser.parse(data.first!!, data.second, state.value, data.second.readableBytes())
                } catch (ex: Exception) {
                    // error when reading packet
                    log(ex)
                    null
                }
                if(packet != null) log("Received ${packet::class.simpleName}", LogType.NETWORK)
            }
        } catch (ex: Exception) {
            log(ex)
        }
    }
}