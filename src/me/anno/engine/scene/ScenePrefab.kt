package me.anno.engine.scene

import me.anno.ecs.Entity
import me.anno.ecs.prefab.change.CSet
import me.anno.ecs.prefab.change.Path
import me.anno.ecs.prefab.Prefab
import me.anno.engine.scene.PrefabHelper.addC
import me.anno.engine.scene.PrefabHelper.addE
import me.anno.engine.scene.PrefabHelper.setX
import me.anno.engine.ui.render.RenderView
import me.anno.io.files.StaticRef
import me.anno.io.text.TextWriter
import me.anno.ui.editor.color.spaces.HSLuv
import me.anno.utils.OS
import org.joml.Quaterniond
import org.joml.Vector3d
import org.joml.Vector3f
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin

// todo ByteBuffer.allocateDirect is not tracked:
// todo either track the process memory
// todo or track the allocateDirect-allocations

object ScenePrefab : StaticRef("Scene.prefab", lazy {
    TextWriter.toText(Prefab("Entity").apply {
        val sets = ArrayList<CSet>()
        this.adds = ArrayList()
        this.sets = sets
        sets.add(CSet(Path.ROOT_PATH, "name", "Root"))
        sets.add(CSet(Path.ROOT_PATH, "desc", "Contains the major components"))
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
            val e = addE(this, root, names[i])
            setX(this, e, "desc", descs[i])
        }

        // root has bullet physics, because the players need physics as well
        addC(this, root, "BulletPhysics")

        // just add stuff for debugging :)
        //////////////////
        // sample mesh //
        ////////////////
        val world = root.added(names[0], 0, 'e')
        val truck = addE(this, world, "VOX/Truck", OS.downloads.getChild("MagicaVoxel/vox/truck.vox"))
        val truckBody0 = truck.added("", 0, 'e')
        addC(this, truckBody0, "MeshCollider")
        //val truckRigidbody = addC(this, truckBody0, "Rigidbody")
        //setX(this, truckRigidbody, "mass", 100.0)

        /////////////////////////
        // sample point light //
        ///////////////////////

        val lights = addE(this, world, "Lights")
        val ambient = addC(this, lights, "AmbientLight")
        setX(this, ambient, "color", Vector3f(0.1f))

        val sun = addE(this, lights, "Sun")
        setX(this, sun, "scale", Vector3d(50.0))
        setX(this, sun, "position", Vector3d(0.0, -10.0, 0.0))
        setX(this, sun, "rotation", Quaterniond().rotateY(0.8).rotateX(-0.8))
        val dl = addC(this, sun, "DirectionalLight")
        setX(this, dl, "shadowMapCascades", 1)
        setX(this, dl, "color", Vector3f(3f))

        /*val sun2 = addE(this, lights, "Sun2")
        setX(this, sun2, "scale", Vector3d(50.0))
        setX(this, sun2, "position", Vector3d(0.0, -10.0, 0.0))
        setX(this, sun2, "rotation", Quaterniond().rotateX(-0.5).rotateY(-0.5))
        val dl2 = addC(this, sun2, "DirectionalLight")
        setX(this, dl2, "shadowMapCascades", 1)*/

        /*val spotLight = addE(this, lights, "Spot Light")
        setX(this, spotLight, "scale", Vector3d(100.0))
        val dls = addC(this, spotLight, "SpotLight")
        setX(this, dls, "shadowMapCascades", 1)
        setX(this, dls, "color", Vector3f(70f))

        val pointLight = addE(this, lights, "Point Light")
        setX(this, pointLight, "position", Vector3d(0.0, 50.0, 0.0))
        setX(this, pointLight, "scale", Vector3d(80.0))
        val dlp = addC(this, pointLight, "PointLight")
        setX(this, dlp, "shadowMapCascades", 1)
        setX(this, dlp, "color", Vector3f(70f))*/

        if (true) {
            val ringOfLights = addE(this, lights, "Ring Of Lights")
            val superRings = 30
            val exponent = 1.1
            val rlc0 = RenderView.MAX_FORWARD_LIGHTS - 4
            val elementSize = 300.0 / max(3, rlc0)
            val lightLevel = 20f // max(3, ringLightCount)
            for (j in 0 until superRings) {
                val superRing = if (superRings > 1) addE(this, ringOfLights, "Ring Of Lights") else ringOfLights
                val ringLightCount = (rlc0 * exponent.pow(j)).toInt()
                val radius = 50.0 * (1.0 + j * 0.1)
                for (i in 0 until ringLightCount) {
                    val angle = 6.2830 * i / ringLightCount
                    val light = addE(this, superRing, "Light[$i]")
                    val position = Vector3d(radius * cos(angle), elementSize * 0.5, radius * sin(angle))
                    setX(this, light, "position", position)
                    setX(this, light, "scale", Vector3d(elementSize))
                    val c = addC(this, light, "PointLight")
                    setX(this, c, "color", HSLuv.toRGB(Vector3f(angle.toFloat(), 1f, 0.7f)).mul(lightLevel))
                }
            }

        }

        ////////////////////
        // physics tests //
        //////////////////
        val physics = addE(this, world, "Physics")

        // vehicle
        /*val vehicle = addE(this, physics, "Vehicle")
        val vehicleComp = addC(this, vehicle, "Vehicle")
        setX(this, vehicleComp, "centerOfMass", Vector3d(0.0, -1.0, 0.0))
        val vehicleCollider = addC(this, vehicle, "BoxCollider", "Box Collider")
        setX(this, vehicleCollider, "halfExtends", Vector3d(1.0, 0.5, 2.0))
        // this.add(ChangeAddEntity(Path(intArrayOf(0, physics, 0))))
        // add all wheels
        var ci = 1
        for (x in -1..1 step 2) {
            for (z in -1..1 step 2) {
                val wheel = addE(this, vehicle, "Wheel[$x,$z]")
                setX(this, wheel, "position", Vector3d(x * 1.1, 0.0, z * 2.0))
                val wheelComp = addC(this, wheel, "VehicleWheel", null)
                // todo set forward and such
                setX(this, wheelComp, "isFront", z > 0)
                ci++
            }
        }
        // todo add script for driving
        */

        /*var testChain = addE(this, world,"chain-0")
        for(i in 1 until 7){
            testChain = addE(this, testChain, "chain-$i")
        }*/

        // add a floor for testing
        val cubePath = OS.documents.getChild("cube.obj")
        val cubePathNormals = OS.documents.getChild("cube shield.glb")
        val floor = addE(this, physics, "Floor", cubePath)
        setX(this, floor, "position", Vector3d(0.0, -50.0, 0.0))
        setX(this, floor, "scale", Vector3d(200.0, 50.0, 200.0))
        // setX(this, floor, "scale", Vector3d(5.0))
        val floorBody = addC(this, floor, "Rigidbody")
        setX(this, floorBody, "mass", 0.0) // static
        val floorCollider = addC(this, floor, "BoxCollider")
        setX(this, floorCollider, "halfExtends", Vector3d(1.0))

        // add spheres for testing
        /*val sphereMesh = OS.documents.getChild("sphere.obj")
        for (i in 0 until 100) {
            val sphere = addE(this, physics, "Sphere[$i]", sphereMesh)
            setX(this, sphere, "position", Vector3d(0.0, (i + 2) * 2.1, 0.0))
            addC(this, sphere, "Rigidbody")
            addC(this, sphere, "SphereCollider")
        }

        // add a cube of cubes for frustum testing
        val frustum = addE(this, world, "Frustum Testing")
        for (x in -5..5) {
            // for testing bounding boxes more
            val xGroup = addE(this, frustum, "Group-$x")
            for (y in -5..5) {
                for (z in -5..5) {
                    // meshes
                    val cube = addE(this, xGroup, "Cube[$x,$y,$z]", cubePathNormals)
                    setX(this, cube, "position", Vector3d(x * 5.0, y * 5.0 + 30.0, z * 5.0))
                    // a little random rotation
                    // val q = Quaterniond(Math.random(), Math.random(), Math.random(), Math.random()).normalize()
                    // setX(this, cube, "rotation", q)
                    // physics test
                    addC(this, cube, "BoxCollider")
                    addC(this, cube, "Rigidbody")
                    val meshComp = Path(cube, "MeshComponent", 0, 'c')
                    setX(this, meshComp, "isInstanced", true)
                }
            }
        }*/

        // normal testing
        /*val normalTesting = addE(this, world, "Normal Testing")
        val l = 100
        for (i in 0 until l) {
            for (j in 0 until 2) {
                val cube = addE(this, normalTesting, "Cube[$i]", cubePathNormals)
                val angle = (i + (j.and(1) * 0.5)) * 6.2830 / l
                setX(this, cube, "position", Vector3d(cos(angle) * l / 2, 1.0 + 2 * j, sin(angle) * l / 2))
                setX(this, cube, "rotation", Quaterniond().rotateY(-angle).rotateX(j * 6.2830 / 8))
            }
        }*/


        // row of planets
        /*val spherePath = OS.documents.getChild("sphere.obj")
        val planets = addE(this, world, "Planets")
        for (i in -50..50) {
            val size = 10.0.pow(i.toDouble())
            val sphere = addE(this, planets, "Sphere 1e$i", spherePath)
            setX(this, sphere, "position", Vector3d(0.0, 0.0, 3.0 * size))
            setX(this, sphere, "scale", Vector3d(size))
        }*/

    }).toByteArray()
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