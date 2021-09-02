package me.anno.engine.scene

import me.anno.ecs.Entity
import me.anno.ecs.prefab.CSet
import me.anno.ecs.prefab.Change
import me.anno.ecs.prefab.Path
import me.anno.ecs.prefab.Prefab
import me.anno.engine.scene.PrefabHelper.addC
import me.anno.engine.scene.PrefabHelper.addE
import me.anno.engine.scene.PrefabHelper.setC
import me.anno.engine.scene.PrefabHelper.setE
import me.anno.io.files.StaticRef
import me.anno.io.text.TextWriter
import me.anno.utils.OS
import org.joml.Quaterniond
import org.joml.Vector3d
import org.joml.Vector3f

object ScenePrefab : StaticRef("Scene.prefab", lazy {
    TextWriter.toText(Prefab("Entity").apply {
        val changes = ArrayList<Change>()
        this.changes = changes
        changes.add(CSet(Path.ROOT_PATH, "name", "Root"))
        changes.add(CSet(Path.ROOT_PATH, "desc", "Contains the major components"))
        val names = listOf("Globally Shared", "Player Prefab", "Locally Shared", "Local Players", "Remote Players")
        val descs = listOf(
            "The world, which is shared",
            "How a player in the global world looks like",
            "If there is UI to be shared for local multiplayer, define it here",
            "Populated at runtime with the players on this PC; can be trusted",
            "Populated at runtime with players from different PCs, states, continents; may not be trusted"
        )
        val root = Path.ROOT_PATH
        for (i in names.indices) {
            val e = addE(changes, root, names[i])
            setC(changes, e, "desc", descs[i])
        }

        // root has bullet physics, because the players need physics as well
        addC(changes, root, "BulletPhysics")

        // just add stuff for debugging :)
        //////////////////
        // sample mesh //
        ////////////////
        val world = root.added(names[0], 0, 'e')
        val truck = addE(changes, world, "VOX/Truck", OS.downloads.getChild("MagicaVoxel/vox/truck.vox"))
        val truckBody0 = truck.added("", 0, 'e')
        addC(changes, truckBody0, "MeshCollider")
        //val truckRigidbody = addC(changes, truckBody0, "Rigidbody")
        //setC(changes, truckRigidbody, "mass", 100.0)

        /////////////////////////
        // sample point light //
        ///////////////////////

        val lights = addE(changes, world, "Lights")
        val ambient = addC(changes, lights, "AmbientLight")
        setC(changes, ambient, "color", Vector3f(0.1f))

        /*val pointLight = addE(changes, lights, "Point Light")
        setE(changes, pointLight, "position", Vector3d(0.0, 50.0, 0.0))
        setE(changes, pointLight, "scale", Vector3d(80.0))
        addC(changes, pointLight, "PointLight")*/

        val sun = addE(changes, lights, "Sun")
        setE(changes, sun, "scale", Vector3d(50.0))
        setE(changes, sun, "position", Vector3d(0.0, -10.0, 0.0))
        setE(changes, sun, "rotation", Quaterniond().rotateX(-0.5))
        val dl = addC(changes, sun, "DirectionalLight")
        setE(changes, dl, "shadowMapCascades", 4)

        val sun2 = addE(changes, lights, "Sun2")
        setE(changes, sun2, "scale", Vector3d(50.0))
        setE(changes, sun2, "position", Vector3d(0.0, -10.0, 0.0))
        setE(changes, sun2, "rotation", Quaterniond().rotateX(-0.5).rotateY(-0.5))
        val dl2 = addC(changes, sun2, "DirectionalLight")
        setE(changes, dl2, "shadowMapCascades", 1)

        /*val spotLight = addE(changes, lights, "Spot Light")
        setE(changes, spotLight, "scale", Vector3d(100.0))
        addC(changes, spotLight, "SpotLight")*/

        /*val ringOfLights = addE(changes, lights, "Ring Of Lights")
        val ringLightCount = RenderView.MAX_LIGHTS - 4
        val lightLevel = 20f / max(3, ringLightCount)
        for (i in 0 until ringLightCount) {
            val angle = 6.2830 * i / ringLightCount
            val radius = 50.0
            val light = addE(changes, ringOfLights, "Light[$i]")
            setE(changes, light, "position", Vector3d(radius * cos(angle), 20.0, radius * sin(angle)))
            setE(changes, light, "scale", Vector3d(100.0))
            val c = addC(changes, light, "PointLight")
            setC(changes, c, "color", HSLuv.toRGB(Vector3f(angle.toFloat(), 0.7f, 0.7f)).mul(lightLevel))
        }*/

        ////////////////////
        // physics tests //
        //////////////////
        val physics = addE(changes, world, "Physics")

        // vehicle
        /*val vehicle = addE(changes, physics, "Vehicle")
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
        */

        /*var testChain = addE(changes, world,"chain-0")
        for(i in 1 until 7){
            testChain = addE(changes, testChain, "chain-$i")
        }*/

        // add a floor for testing
        val cubePath = OS.documents.getChild("cube.obj")
        val cubePathNormals = OS.documents.getChild("cube shield.glb")
        val floor = addE(changes, physics, "Floor", cubePath)
        setE(changes, floor, "position", Vector3d(0.0, -50.0, 0.0))
        setE(changes, floor, "scale", Vector3d(200.0, 50.0, 200.0))
        // setE(changes, floor, "scale", Vector3d(5.0))
        val floorBody = addC(changes, floor, "Rigidbody")
        setC(changes, floorBody, "mass", 0.0) // static
        val floorCollider = addC(changes, floor, "BoxCollider")
        setC(changes, floorCollider, "halfExtends", Vector3d(1.0))

        /*// add spheres for testing
        val sphereMesh = OS.documents.getChild("sphere.obj")
        for (i in 0 until 100) {
            val sphere = addE(changes, physics, "Sphere[$i]", sphereMesh)
            setE(changes, sphere, "position", Vector3d(0.0, (i + 2) * 2.1, 0.0))
            addC(changes, sphere, "Rigidbody")
            addC(changes, sphere, "SphereCollider")
        }

        // add a cube of cubes for frustum testing
        val frustum = addE(changes, world, "Frustum Testing")
        for (x in -5..5) {
            // for testing bounding boxes more
            val xGroup = addE(changes, frustum, "Group-$x")
            for (y in -5..5) {
                for (z in -5..5) {
                    // meshes
                    val cube = addE(changes, xGroup, "Cube[$x,$y,$z]", cubePathNormals)
                    setE(changes, cube, "position", Vector3d(x * 5.0, y * 5.0 + 30.0, z * 5.0))
                    // a little random rotation
                    // val q = Quaterniond(Math.random(), Math.random(), Math.random(), Math.random()).normalize()
                    // setE(changes, cube, "rotation", q)
                    // physics test
                    addC(changes, cube, "BoxCollider")
                    addC(changes, cube, "Rigidbody")
                }
            }
        }*/

        // normal testing
        /*val normalTesting = addE(changes, world, "Normal Testing")
        val l = 100
        for (i in 0 until l) {
            for (j in 0 until 2) {
                val cube = addE(changes, normalTesting, "Cube[$i]", cubePathNormals)
                val angle = (i + (j.and(1) * 0.5)) * 6.2830 / l
                setE(changes, cube, "position", Vector3d(cos(angle) * l / 2, 1.0 + 2 * j, sin(angle) * l / 2))
                setE(changes, cube, "rotation", Quaterniond().rotateY(-angle).rotateX(j * 6.2830 / 8))
            }
        }*/


        // row of planets
        /*val spherePath = OS.documents.getChild("sphere.obj")
        val planets = addE(changes, world, "Planets")
        for (i in -50..50) {
            val size = 10.0.pow(i.toDouble())
            val sphere = addE(changes, planets, "Sphere 1e$i", spherePath)
            setE(changes, sphere, "position", Vector3d(0.0, 0.0, 3.0 * size))
            setE(changes, sphere, "scale", Vector3d(size))
        }*/

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