package me.anno.engine

import me.anno.ecs.Entity

class RootEntity : Entity() {

    val world = Entity()

    // only enabled in scene editor
    val playerPrefab = Entity()

    // can be created using the prefab
    val localPlayers = Entity()//ArrayList<LocalPlayer>()

    // e.g. for custom ui layouts
    val locallyShared = Entity()

    // what about their UI? mmh...
    val remotePlayers = Entity()//ArrayList<RemotePlayer>()

    init {
        addChild(world)
        addChild(playerPrefab)
        addChild(locallyShared)
        addChild(localPlayers)
        addChild(remotePlayers)
    }

    fun createLocalPlayer(info: Any): Entity {
        // todo set info ...
        val instance = playerPrefab.clone()
        instance.isEnabled = true
        localPlayers.add(instance)
        return instance
    }

    fun createRemotePlayer(info: Any): Entity {
        // todo set info ...
        val instance = playerPrefab.clone()
        instance.isEnabled = true
        remotePlayers.add(instance)
        return instance
    }

    fun removePlayer(player: Entity) {
        localPlayers.remove(player)

    }

}