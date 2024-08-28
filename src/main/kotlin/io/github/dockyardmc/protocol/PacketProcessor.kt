package io.github.dockyardmc.protocol

import cz.lukynka.Bindable
import cz.lukynka.prettylog.log
import io.github.dockyardmc.events.Events
import io.github.dockyardmc.events.PlayerDisconnectEvent
import io.github.dockyardmc.events.PlayerLeaveEvent
import io.github.dockyardmc.extentions.readVarInt
import io.github.dockyardmc.player.Player
import io.github.dockyardmc.player.PlayerManager
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

    val state: Bindable<ProtocolState> = Bindable(ProtocolState.HANDSHAKE)
    var encrypted = false

    lateinit var player: Player
    lateinit var address: String
    var protocolVersion: Int = 0

    var respondedToLastKeepAlive = true

    init {
        state.valueChanged {
            val display = if (this::player.isInitialized) player.username else address
            debug("Protocol state for $display changed to ${it.newValue}")
        }
    }


    var statusHandler = HandshakeHandler(this)
    var loginHandler = LoginHandler(this)
    var configurationHandler = ConfigurationHandler(this)
    var playHandler = PlayHandler(this)

    private fun readPacket(buffer: ByteBuf, connection: ChannelHandlerContext) {
        // Check if we have enough bytes to read the packet header (size + id)
        if (buffer.readableBytes() < 5) { // VarInts can be up to 5 bytes each
            return // Wait for more data
        }

        buffer.markReaderIndex() // Mark in case we need to reset

        val packetSize = buffer.readVarInt() - 1
        val packetId = buffer.readVarInt()

        // Check if we have the entire packet
        if (buffer.readableBytes() < packetSize) {
            buffer.resetReaderIndex() // Reset to the marked position
            return // Wait for more data
        }

        val payload = buffer.readSlice(packetSize)
        payload.retain() // Increase reference count as we'll pass it around

        log("Incoming packet with id $packetId (size $packetSize)")

        // Process the payload (make sure to release it when done)
        processPayload(packetId, payload, connection)
        payload.release() // Decrease reference count
    }

    private fun processPayload(packetId: Int, payload: ByteBuf, connection: ChannelHandlerContext) {
        val size = payload.readableBytes()
        val packet = PacketParser.parse(packetId, payload, state.value, size) ?: throw java.lang.Exception("aaaeeoo")
        packet.handle(this, connection, packetId, size)
    }

    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        val buffer = msg as ByteBuf
        address = ctx.channel().remoteAddress().address
        try {
            readPacket(buffer, ctx)
        } finally {
            // Release the buffer after processing, even if there's an exception
            buffer.release()
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