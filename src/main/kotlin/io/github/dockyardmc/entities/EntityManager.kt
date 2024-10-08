package io.github.dockyardmc.entities

import io.github.dockyardmc.events.Events
import io.github.dockyardmc.events.ServerTickEvent
import io.github.dockyardmc.player.PlayerManager
import io.github.dockyardmc.world.World
import java.util.concurrent.atomic.AtomicInteger

object EntityManager {

    var entityIdCounter = AtomicInteger()
    val entities: MutableList<Entity> = mutableListOf()

    fun World.spawnEntity(entity: Entity, noViewers: Boolean = false): Entity {
        this@EntityManager.entities.add(entity)
        this.entities.add(entity)
        if(!noViewers) PlayerManager.players.forEach(entity::addViewer)
        return entity
    }

    fun World.despawnEntity(entity: Entity) {
        this@EntityManager.entities.remove(entity)
        PlayerManager.players.forEach {
            entity.removeViewer(it, false)
        }
    }

    init {
        Events.on<ServerTickEvent> {
            entities.forEach { if(it.tickable) it.tick() }
        }
    }
}