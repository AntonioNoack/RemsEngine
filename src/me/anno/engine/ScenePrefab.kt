package me.anno.engine

import me.anno.ecs.Entity
import me.anno.ecs.prefab.Prefab
import me.anno.ecs.prefab.change.Path.Companion.ROOT_PATH
import me.anno.io.files.FileRootRef
import me.anno.io.zip.InnerLazyPrefabFile
import org.apache.logging.log4j.LogManager

/*
* todo for the shipped game, pack all scene files into a new zip file,
* todo and preload all (?) values, so we get faster access times
* todo for installing, it may be wise to split large textures/videos/audios and the rest:
* todo these large files would be unzipped into a folder, and the rest can be loaded at start, and just be kept in memory
* we could even convert video formats, so we only need support for a single format :3
*/

object ScenePrefab : InnerLazyPrefabFile(
    "Scene.prefab",
    "Scene.prefab",
    FileRootRef,
    lazy {
        Prefab("Entity").apply {

            val logger = LogManager.getLogger(ScenePrefab::class)

            logger.debug("creating ScenePrefab from thread {}", Thread.currentThread().name)

            ensureMutableLists()

            set("name", "Root")
            set("description", "Contains the major components")
            set("isCollapsed", false)

            val coreComponents = listOf(
                "Globally Shared" to "The world, which is shared",
                "Player Prefab" to "What a player in the global world looks like",
                "Locally Shared" to "If there is UI to be shared for local multiplayer, define it here",
                "Local Players" to "Populated at runtime with the players on this PC; can be trusted",
                "Remote Players" to "Populated at runtime with players from different PCs, states, continents; may not be trusted"
            )

            for ((name, description) in coreComponents) {
                val path = add(ROOT_PATH, 'e', "Entity", name)
                set(path, "description", description)
                set(path, "isCollapsed", false)
            }

            sealFromModifications()

            logger.debug("finished ScenePrefab")

        }
    }) {

    val worldIndex = 0
    val playerPrefabIndex = 1
    val locallySharedIndex = 2
    val localPlayersIndex = 3
    val remotePlayersIndex = 4

    /**
     * will be shared over the network, and available to all clients
     * should be written only to by the server, or for particle effects
     * */
    fun getWorld(scene: Entity) = scene.children[worldIndex]

    /**
     * only enabled in scene editor
     * the base for all players
     * */
    fun getPlayerPrefab(scene: Entity) = scene.children[playerPrefabIndex]

    /**
     * e.g. for custom ui layouts,
     * will not be shared with the server
     * */
    fun getLocallyShared(scene: Entity) = scene.children[locallySharedIndex]

    /**
     * a list of players,
     * can be created using the playerPrefab
     * players on this computer
     * */
    fun getLocalPlayers(scene: Entity) = scene.children[localPlayersIndex]

    /**
     * a list of players,
     * remote players, will be controlled by the server connection
     * if the server is local, it may be controlled by the remotely playing
     * */
    fun getRemotePlayers(scene: Entity) = scene.children[remotePlayersIndex]

    fun getPhysics(scene: Entity) = scene.physics

    fun createLocalPlayer(entity: Entity, name: String): Entity {
        val instance = getPlayerPrefab(entity).clone() as Entity
        instance.name = name
        instance.isEnabled = true
        getLocalPlayers(entity).add(instance)
        return instance
    }

    fun createRemotePlayer(entity: Entity, name: String): Entity {
        val instance = getPlayerPrefab(entity).clone() as Entity
        instance.name = name
        instance.isEnabled = true
        getRemotePlayers(entity).add(instance)
        return instance
    }

    fun removePlayer(entity: Entity, player: Entity) {
        for (child in entity.children) {
            child.remove(player)
        }
    }

}