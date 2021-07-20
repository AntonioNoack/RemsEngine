package me.anno.engine

import me.anno.ecs.Entity
import me.anno.utils.types.Strings.isBlank2

class ECSWorld(val world: Entity) {

    init {
        if (world.name.isBlank2()) world.name = "Game World"
        val names = listOf("Globally Shared", "Player Prefab", "Local Players", "Locally Shared", "Remote Players")
        for (i in world.children.size until 5) {
            world.children.add(Entity(names[i]))
        }
    }

    // will be shared over the network, and available to all clients
    // should be written only to by the server, or for particle effects
    val globallyShared = world.children[0]

    /**
     * only enabled in scene editor
     * the base for all players
     * */
    val playerPrefab = world.children[1]

    /**
     * a list of players,
     * can be created using the playerPrefab
     * players on this computer
     * */
    val localPlayers = world.children[2]

    /**
     * e.g. for custom ui layouts,
     * will not be shared with the server
     * */
    val locallyShared = world.children[3]

    /**
     * a list of players,
     * remote players, will be controlled by the server connection
     * if the server is local, it may be controlled by the remotely playing
     * */
    val remotePlayers = world.children[4]

    init {
        world.addChild(globallyShared)
        world.addChild(playerPrefab)
        world.addChild(locallyShared)
        world.addChild(localPlayers)
        world.addChild(remotePlayers)
    }

    fun createLocalPlayer(name: String): Entity {
        val instance = playerPrefab.clone()
        instance.name = name
        instance.isEnabled = true
        localPlayers.add(instance)
        return instance
    }

    fun createRemotePlayer(name: String): Entity {
        val instance = playerPrefab.clone()
        instance.name = name
        instance.isEnabled = true
        remotePlayers.add(instance)
        return instance
    }

    fun removePlayer(player: Entity) {
        localPlayers.remove(player)
        remotePlayers.remove(player)
    }

    var physics = BulletPhysics()

}