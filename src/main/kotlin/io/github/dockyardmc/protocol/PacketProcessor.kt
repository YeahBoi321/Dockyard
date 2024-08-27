package io.github.dockyardmc.protocol

import cz.lukynka.prettylog.LogType
import cz.lukynka.prettylog.log
import io.github.dockyardmc.DockyardServer
import io.github.dockyardmc.ServerMetrics
import io.github.dockyardmc.events.Events
import io.github.dockyardmc.events.PacketReceivedEvent
import io.github.dockyardmc.events.PlayerDisconnectEvent
import io.github.dockyardmc.events.PlayerLeaveEvent
import io.github.dockyardmc.extentions.broadcastMessage
import io.github.dockyardmc.extentions.readVarInt
import io.github.dockyardmc.player.Player
import io.github.dockyardmc.player.PlayerManager
import io.github.dockyardmc.profiler.Profiler
import io.github.dockyardmc.protocol.packets.ProtocolState
import io.github.dockyardmc.protocol.packets.configurations.ConfigurationHandler
import io.github.dockyardmc.protocol.packets.handshake.HandshakeHandler
import io.github.dockyardmc.protocol.packets.login.LoginHandler
import io.github.dockyardmc.protocol.packets.play.PlayHandler
import io.github.dockyardmc.utils.debug
import io.ktor.util.network.*
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter

class PacketProcessor : ChannelInboundHandlerAdapter() {

    private var innerState = ProtocolState.HANDSHAKE
    var encrypted = false

    lateinit var player: Player
    lateinit var address: String
    var playerProtocolVersion: Int = 0

    var respondedToLastKeepAlive = true

    var state: ProtocolState
        get() = innerState
        set(value) {
            innerState = value
            val display = if (this::player.isInitialized) player.username else address
            debug("Protocol state for $display changed to $value")
        }

    var statusHandler = HandshakeHandler(this)
    var loginHandler = LoginHandler(this)
    var configurationHandler = ConfigurationHandler(this)
    var playHandler = PlayHandler(this)

    @OptIn(ExperimentalStdlibApi::class)
    override fun channelRead(connection: ChannelHandlerContext, msg: Any) {
        val profiler = Profiler()

        if (!this::address.isInitialized) address = connection.channel().remoteAddress().address
        val buf = msg as ByteBuf
        try {
            profiler.start("Read Packet Buf", 20)
            while (buf.isReadable) {
                buf.retain()

                buf.markReaderIndex()
                val packetSize = buf.readVarInt() - 1
                val packetId = buf.readVarInt()
                val packetIdByteRep = "0x${packetId.toByte().toHexString()}"


                if (buf.readableBytes() < packetSize) {
                    buf.discardReadBytes()
                    log("Received packet $packetId ($packetIdByteRep) which has less readable bytes than packet size specified (${buf.readableBytes()} < ${packetSize})", LogType.ERROR)
                    break
                }

                val packetData = buf.readSlice(packetSize)
                try {
                    val packet = PacketParser.parse(packetId, packetData, state, packetSize)

                    if(packet == null) {
                        buf.discardReadBytes()
                        log("Received unknown packet with id $packetId ($packetIdByteRep) during phase: ${state.name}", LogType.ERROR)
                        break
                    }

                    val className = packet::class.simpleName ?: packet::class.toString()
                    ServerMetrics.packetsReceived++
                    if (!DockyardServer.mutePacketLogs.contains(className)) {
                        debug("-> Received $className (${packetIdByteRep})", LogType.NETWORK)
                    }

                    val event = PacketReceivedEvent(packet, connection, packetSize, packetId)
                    Events.dispatch(event)
                    if (event.cancelled) {
                        buf.discardReadBytes()
                        break
                    }

                    DockyardServer.broadcastMessage("bytes left after ${packet::class.simpleName}:  ${buf.readableBytes()}")
                    event.packet.handle(this, event.connection, event.size, event.id)
                } finally {
                    packetData.release()
                }

            }
        } catch (ex: Exception) {
            handleException(connection, buf, ex)
        } finally {
            buf.release()  // Release the buffer after processing
            buf.clear()
            profiler.end()
            connection.flush()
        }
    }

    fun clearBuffer(connection: ChannelHandlerContext, buffer: ByteBuf) {
        buffer.release()
        buffer.clear()
        connection.flush()
    }

    fun handleException(connection: ChannelHandlerContext, buffer: ByteBuf, exception: Exception) {
        log(exception)
        clearBuffer(connection, buffer)
    }

    override fun handlerAdded(ctx: ChannelHandlerContext) {
        super.handlerAdded(ctx)
    }

    override fun handlerRemoved(ctx: ChannelHandlerContext) {
        if(this::player.isInitialized) {
            player.isConnected = false
            PlayerManager.remove(player)
            Events.dispatch(PlayerDisconnectEvent(player))
            if(player.isFullyInitialized) {
                Events.dispatch(PlayerLeaveEvent(player))
            }
        }
    }

    override fun channelReadComplete(ctx: ChannelHandlerContext) {
        ctx.flush()
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        log(cause as Exception)
        ctx.flush()
        ctx.close()
    }
}