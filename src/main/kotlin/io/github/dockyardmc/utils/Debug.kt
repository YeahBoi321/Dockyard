package io.github.dockyardmc.utils

import cz.lukynka.prettylog.CustomLogType
import cz.lukynka.prettylog.LogType
import cz.lukynka.prettylog.log
import io.github.dockyardmc.DockyardServer
import io.github.dockyardmc.ServerMetrics
import io.github.dockyardmc.protocol.packets.ServerboundPacket

fun debug(text: String, logType: CustomLogType = LogType.DEBUG) {
    if(DockyardServer.debug) log(text, logType)
}

@OptIn(ExperimentalStdlibApi::class)
fun logIncomingPacket(packet: ServerboundPacket, id: Int, size: Int) {
    val className = packet::class.simpleName
    val packetIdByte = "0x${id.toByte().toHexString()}"
    ServerMetrics.packetsReceived++
    if (!DockyardServer.mutePacketLogs.contains(className)) {
        debug("-> Received $className (${packetIdByte})", LogType.NETWORK)
    }
}