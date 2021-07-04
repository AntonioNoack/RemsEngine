package me.anno.engine

import me.anno.ecs.Entity

class ECSWorld : Entity() {

    // will be shared over the network, and available to all clients
    // should be written only to by the server, or for particle effects
    val globallyShared = Entity("Globally Shared")

    // only enabled in scene editor
    val playerPrefab = Entity("Player Prefab")

    // can be created using the prefab
    val localPlayers = Entity("Local Players")//ArrayList<LocalPlayer>()

    // e.g. for custom ui layouts,
    // will not be shared with the server
    val locallyShared = Entity("Locally Shared")

    // remote players, will be controlled by the server connection
    // if the server is local, it may be controlled by the remotely playing
    val remotePlayers = Entity("Remote Players")//ArrayList<RemotePlayer>()

    init {
        addChild(globallyShared)
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

    override fun getClassName(): String = "RootEntity"

}