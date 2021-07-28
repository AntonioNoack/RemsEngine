package me.anno.engine.scene

import me.anno.ecs.Entity
import me.anno.ecs.prefab.*
import me.anno.engine.scene.PrefabHelper.addC
import me.anno.engine.scene.PrefabHelper.addE
import me.anno.engine.scene.PrefabHelper.setC
import me.anno.engine.scene.PrefabHelper.setE
import me.anno.io.files.InvalidRef
import me.anno.io.files.StaticRef
import me.anno.io.text.TextWriter
import me.anno.utils.OS
import org.joml.Quaterniond
import org.joml.Vector3d

object ScenePrefab : StaticRef("Scene.prefab", lazy {
    TextWriter.toText(Prefab("Entity").apply {
        val changes = ArrayList<Change>()
        this.changes = changes
        changes.add(CSet(Path(), "name", "Root"))
        changes.add(CSet(Path(), "desc", "Contains the major components"))
        val names = listOf("Globally Shared", "Player Prefab", "Locally Shared", "Local Players", "Remote Players")
        val descs = listOf(
            "The world, which is shared",
            "How a player in the global world looks like",
            "If there is UI to be shared for local multiplayer, define it here",
            "Populated at runtime with the players on this PC; can be trusted",
            "Populated at runtime with players from different PCs, states, continents; may not be trusted"
        )
        for (i in names.indices) {
            changes.add(CAdd(Path(), 'e', "Entity", InvalidRef))
            changes.add(CSet(Path(i), "name", names[i]))
            changes.add(CSet(Path(i), "desc", descs[i]))
        }

        // root has bullet physics, because the players need physics as well
        changes.add(CAdd(Path(), 'c', "BulletPhysics"))


        // just add stuff for debugging :)
        //////////////////
        // sample mesh //
        ////////////////
        val world = Path(0, 'e')
        val truck = addE(changes, world, "VOX/Truck", OS.downloads.getChild("MagicaVoxel/vox/truck.vox"))
        val truckBody0 = truck + (0 to 'e')
        addC(changes, truckBody0, "MeshCollider")
        val truckRigidbody = addC(changes, truckBody0, "Rigidbody")
        setC(changes, truckRigidbody, "mass", 100.0)

        /////////////////////////
        // sample point light //
        ///////////////////////
        val light = addE(changes, world, "Point Light")
        setE(changes, light, "position", Vector3d(0.0, 50.0, 0.0))
        setE(changes, light, "scale", Vector3d(80.0))
        addC(changes, light, "PointLight")

        ////////////////////
        // physics tests //
        //////////////////
        val physics = addE(changes, world, "Physics")

        // vehicle
        val vehicle = addE(changes, physics, "Vehicle")
        val vehicleComp = addC(changes, vehicle, "Vehicle")
        setC(changes, vehicleComp, "centerOfMass", Vector3d(0.0, -1.0, 0.0))
        val vehicleCollider = addC(changes, vehicle, "BoxCollider", "Box Collider")
        setC(changes, vehicleCollider, "halfExtends", Vector3d(1.0, 0.5, 2.0))
        // changes.add(ChangeAddEntity(Path(intArrayOf(0, physics, 0))))
        // add all wheels
        var ci = 1
        for (x in -1..1 step 2) {
            for (z in -1..1 step 2) {
                val wheel = addE(changes, vehicle, "Wheel[$x,$z]")
                setE(changes, wheel, "position", Vector3d(x * 1.1, 0.0, z * 2.0))
                val wheelComp = addC(changes, wheel, "VehicleWheel", null)
                // todo set forward and such
                setC(changes, wheelComp, "isFront", z > 0)
                ci++
            }
        }
        // todo add script for driving

        /*var testChain = addE(changes, world,"chain-0")
        for(i in 1 until 7){
            testChain = addE(changes, testChain, "chain-$i")
        }*/

        // add a floor for testing
        val cubePath = OS.documents.getChild("cube.obj")
        val floor = addE(changes, physics, "Floor", cubePath)
        setE(changes, floor, "position", Vector3d(0.0, -50.0, 0.0))
        setE(changes, floor, "scale", Vector3d(200.0, 50.0, 200.0))
        // setE(changes, floor, "scale", Vector3d(5.0))
        val floorBody = addC(changes, floor, "Rigidbody")
        setC(changes, floorBody, "mass", 0.0) // static
        val floorCollider = addC(changes, floor, "BoxCollider")
        setC(changes, floorCollider, "halfExtends", Vector3d(1.0))

        // add spheres for testing
        val sphereMesh = OS.documents.getChild("sphere.obj")
        for (i in 0 until 4) {
            val sphere = addE(changes, physics, "Sphere[$i]", sphereMesh)
            setE(changes, sphere, "position", Vector3d(0.0, (i + 2) * 2.1, 0.0))
            addC(changes, sphere, "Rigidbody")
            addC(changes, sphere, "SphereCollider")
        }

        // add a cube of cubes for frustum testing
        val frustum = addE(changes, world, "Frustum Testing")
        for (x in -5..5) {
            for (y in -5..25) {
                for (z in -5..5) {
                    // meshes
                    val cube = addE(changes, frustum, "Cube[$x,$y,$z]", cubePath)
                    setE(changes, cube, "position", Vector3d(x * 5.0, y * 5.0 + 30.0, z * 5.0))
                    // a little random rotation
                    val q = Quaterniond(Math.random(), Math.random(), Math.random(), Math.random()).normalize()
                    setE(changes, cube, "rotation", q)
                    // physics test
                    addC(changes, cube, "CylinderCollider")
                    addC(changes, cube, "Rigidbody")
                }
            }
        }

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

    fun getPhysics(scene: Entity) = scene.physics

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
            child.remove(entity)
        }
    }

}