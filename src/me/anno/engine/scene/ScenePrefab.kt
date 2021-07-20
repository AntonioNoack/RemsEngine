package me.anno.engine.scene

import me.anno.ecs.Entity
import me.anno.ecs.prefab.*
import me.anno.engine.BulletPhysics
import me.anno.io.files.InvalidRef
import me.anno.io.files.StaticRef
import me.anno.io.text.TextWriter

object ScenePrefab : StaticRef("Scene.prefab", lazy {
    TextWriter.toText(EntityPrefab().apply {
        val changes = ArrayList<Change>()
        this.changes = changes
        changes.add(ChangeSetEntityAttribute(Path("name"), "Root"))
        changes.add(ChangeSetEntityAttribute(Path("desc"), "Contains the major components"))
        val names = listOf("Globally Shared", "Player Prefab", "Locally Shared", "Local Players", "Remote Players")
        val descs = listOf(
            "The world, which is shared",
            "How a player in the global world looks like",
            "If there is UI to be shared for local multiplayer, define it here",
            "Populated at runtime with the players on this PC; can be trusted",
            "Populated at runtime with players from different PCs, states, continents; may not be trusted"
        )
        for (i in names.indices) {
            changes.add(ChangeAddEntity(Path(), InvalidRef))
            changes.add(ChangeSetEntityAttribute(Path(i, "name"), names[i]))
            changes.add(ChangeSetEntityAttribute(Path(i, "desc"), descs[i]))
        }
        changes.add(ChangeAddComponent(Path(0), "BulletPhysics"))
    }, false).toByteArray()
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

    fun getPhysics(scene: Entity) = getWorld(scene).getComponent<BulletPhysics>(true)


    fun createLocalPlayer(entity: Entity, name: String): Entity {
        val instance = getPlayerPrefab(entity).clone()
        instance.name = name
        instance.isEnabled = true
        getLocalPlayers(entity).add(instance)
        return instance
    }

    fun createRemotePlayer(entity: Entity, name: String): Entity {
        val instance = getPlayerPrefab(entity).clone()
        instance.name = name
        instance.isEnabled = true
        getRemotePlayers(entity).add(instance)
        return instance
    }

    fun removePlayer(entity: Entity, player: Entity) {
        for (child in entity.children) {
            child.children.remove(entity)
        }
    }


}