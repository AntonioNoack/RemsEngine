package me.anno.engine.scene

import me.anno.ecs.Entity
import me.anno.ecs.prefab.Prefab
import me.anno.ecs.prefab.change.Path
import me.anno.engine.scene.PrefabHelper.addC
import me.anno.engine.scene.PrefabHelper.addE
import me.anno.engine.ui.render.RenderView
import me.anno.io.files.FileRootRef
import me.anno.io.zip.InnerPrefabFile
import me.anno.ui.editor.color.spaces.HSLuv
import me.anno.utils.Clock
import me.anno.utils.OS
import me.anno.utils.OS.documents
import org.joml.Quaterniond
import org.joml.Vector3d
import org.joml.Vector3f
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

object ScenePrefab : InnerPrefabFile(
    "Scene.prefab",
    "Scene.prefab",
    FileRootRef,
    Prefab("Entity").apply {

        val clock = Clock()

        ensureMutableLists()

        set(Path.ROOT_PATH, "name", "Root")
        set(Path.ROOT_PATH, "description", "Contains the major components")
        set(Path.ROOT_PATH, "isCollapsed", false)

        val coreComponents = listOf(
            "Globally Shared" to "The world, which is shared",
            "Player Prefab" to "What a player in the global world looks like",
            "Locally Shared" to "If there is UI to be shared for local multiplayer, define it here",
            "Local Players" to "Populated at runtime with the players on this PC; can be trusted",
            "Remote Players" to "Populated at runtime with players from different PCs, states, continents; may not be trusted"
        )

        val root = Path.ROOT_PATH
        val instances = ArrayList<Path>()
        for ((name, description) in coreComponents) {
            val e = addE(this, root, name)
            set(e, "description", description)
            set(e, "isCollapsed", false)
            instances.add(e)
        }


        // root has bullet physics, because the players need physics as well
        addC(this, root, "BulletPhysics")

        clock.stop("main things")

        // just add stuff for debugging :)
        //////////////////
        // sample mesh //
        ////////////////
        val world = instances[0]

        val testCamera = addE(this, world, "Camera")
        addC(this, testCamera, "CameraComponent")

        val truck = addE(this, world, "VOX/Truck", OS.downloads.getChild("MagicaVoxel/vox/truck.vox"))
        val truckBody0 = truck.added("", 0, 'e')
        addC(this, truckBody0, "MeshCollider")
        set(truck, "isCollapsed", false)
        //val truckRigidbody = addC(this, truckBody0, "Rigidbody")
        //set(truckRigidbody, "mass", 100.0)

        clock.stop("truck")

        /////////////////////////
        // sample point light //
        ///////////////////////

        val lights = addE(this, world, "Lights")
        val ambient = addC(this, lights, "AmbientLight")
        set(ambient, "color", Vector3f(0.1f))

        val sun = addE(this, lights, "Sun")
        set(sun, "scale", Vector3d(50.0))
        set(sun, "position", Vector3d(0.0, -10.0, 0.0))
        set(sun, "rotation", Quaterniond().rotateY(0.8).rotateX(-0.8))
        val dl = addC(this, sun, "DirectionalLight")
        set(dl, "shadowMapCascades", 1)
        set(dl, "color", Vector3f(3f))

        clock.stop("lights")

        /*val env = addE(this, lights, "EnvMap")
        set(env, "scale", Vector3d(50.0))
        set(env, "position", Vector3d(0.0, 10.0, 0.0))
        addC(this, env, "EnvironmentMap")

        val plane = addE(this, lights, "Planar")
        addC(this, plane, "PlanarReflection")
        addE(this, plane, "Mirror", documents.getChild("mirror.fbx"))*/

        /*val sun2 = addE(this, lights, "Sun2")
        set(sun2, "scale", Vector3d(50.0))
        set(sun2, "position", Vector3d(0.0, -10.0, 0.0))
        set(sun2, "rotation", Quaterniond().rotateX(-0.5).rotateY(-0.5))
        val dl2 = addC(this, sun2, "DirectionalLight")
        set(dl2, "shadowMapCascades", 1)*/

        /*val spotLight = addE(this, lights, "Spot Light")
        set(spotLight, "scale", Vector3d(100.0))
        val dls = addC(this, spotLight, "SpotLight")
        set(dls, "shadowMapCascades", 1)
        set(dls, "color", Vector3f(70f))

        val pointLight = addE(this, lights, "Point Light")
        set(pointLight, "position", Vector3d(0.0, 50.0, 0.0))
        set(pointLight, "scale", Vector3d(80.0))
        val dlp = addC(this, pointLight, "PointLight")
        set(dlp, "shadowMapCascades", 1)
        set(dlp, "color", Vector3f(70f))*/

        if (true) {
            val ringOfLights = addE(this, lights, "Ring Of Lights")
            val superRings = 35
            val rlc0 = RenderView.MAX_FORWARD_LIGHTS - 4
            val elementSize = 300.0 / max(3, rlc0)
            val lightLevel = 20f
            val numColors = 3
            val colors = Array(numColors) {
                val angle = it / numColors.toFloat()
                val vec = Vector3f(angle, 1f, 0.7f)
                HSLuv.toRGB(vec, vec).mul(lightLevel)
            }
            val scale = Vector3d(elementSize)
            fun getRadius(j: Int) = 50.0 * (1.0 + j * 0.1)
            fun getLightCount(radius: Double) = (radius * 0.5).toInt()
            val instanceNames = Array(getLightCount(getRadius(superRings - 1))) { "Light[$it]" }
            for (j in 0 until superRings) {
                val superRing = if (superRings > 1) addE(this, ringOfLights, "Ring[$j]") else ringOfLights
                val radius = getRadius(j)
                val ringLightCount = getLightCount(radius)
                for (i in 0 until ringLightCount) {
                    val angle = 6.2830 * i.toDouble() / ringLightCount
                    val light = addE(this, superRing, instanceNames[i])
                    val position = Vector3d(radius * cos(angle), elementSize * 0.5, radius * sin(angle))
                    set(light, "position", position)
                    set(light, "scale", scale)
                    val c = addC(this, light, "PointLight")
                    set(c, "color", colors[(numColors * 3 * i / ringLightCount) % numColors])
                }
            }
        }

        clock.stop("ring of lights")

        ////////////////////
        // physics tests //
        //////////////////
        val physics = addE(this, world, "Physics")

        // vehicle
        val vehicle = addE(this, physics, "Vehicle")
        val vehicleComp = addC(this, vehicle, "Vehicle")
        set(vehicleComp, "centerOfMass", Vector3d(0.0, -1.0, 0.0))
        val vehicleCollider = addC(this, vehicle, "BoxCollider", "Box Collider")
        set(vehicleCollider, "halfExtends", Vector3d(1.0, 0.5, 2.0))
        addC(this, vehicle, "TestVehicleController")
        // this.add(ChangeAddEntity(Path(intArrayOf(0, physics, 0))))
        // add all wheels
        var ci = 1
        for (x in -1..1 step 2) {
            for (z in -1..1 step 2) {
                val wheel = addE(this, vehicle, "Wheel[$x,$z]")
                set(wheel, "position", Vector3d(x * 1.1, 0.0, z * 2.0))
                val wheelComp = addC(this, wheel, "VehicleWheel")
                // todo set forward and such
                set(wheelComp, "isFront", z > 0)
                ci++
            }
        }
        // todo add script for driving
        clock.stop("vehicle")


        // todo chain test:
        // todo create two static points, and a long chain in-between
        // todo when this works, create spider girl ...

        // todo test all constraints...
        // todo debug input mode to debug physics
        val constraints = addE(this, physics, "Constraints")

        // todo generic
        // todo hinge
        // todo point2point
        // todo cone twist

        // (except we need to much other stuff... UI, controls, click events, ...)

        /*var testChain = addE(this, world,"chain-0")
        for(i in 1 until 7){
            testChain = addE(this, testChain, "chain-$i")
        }*/

        // add a floor for testing
        val cubePath = documents.getChild("cube.obj")
        val cubePathNormals = documents.getChild("cube shield.glb")
        val floor = addE(this, physics, "Floor")
        set(floor, "position", Vector3d(0.0, -50.0, 0.0))
        set(floor, "scale", Vector3d(2000.0, 50.0, 2000.0))
        // set(floor, "scale", Vector3d(5.0))
        val floorBody = addC(this, floor, "Rigidbody")
        set(floorBody, "mass", 0.0) // static
        val floorCollider = addC(this, floor, "BoxCollider")
        set(floorCollider, "halfExtends", Vector3d(1.0))
        val floorMesh = addC(this, floor, "MeshComponent")
        set(floorMesh, "mesh", cubePath)

        clock.stop("floor")

        // add spheres for testing
        /*val sphereMesh = OS.documents.getChild("sphere.obj")
        for (i in 0 until 100) {
            val sphere = addE(this, physics, "Sphere[$i]", sphereMesh)
            set(sphere, "position", Vector3d(0.0, (i + 2) * 2.1, 0.0))
            addC(this, sphere, "Rigidbody")
            addC(this, sphere, "SphereCollider")
        }*/

        // add a cube of cubes for frustum testing
        /*val frustum = addE(this, world, "Frustum Testing")
        for (x in -5..5) {
            // for testing bounding boxes more
            val xGroup = addE(this, frustum, "Group-$x")
            for (y in -5..5) {
                for (z in -5..5) {
                    // meshes
                    val cube = addE(this, xGroup, "Cube[$x,$y,$z]", cubePathNormals)
                    set(cube, "position", Vector3d(x * 5.0, y * 5.0 + 30.0, z * 5.0))
                    // a little random rotation
                    // val q = Quaterniond(Math.random(), Math.random(), Math.random(), Math.random()).normalize()
                    // set(cube, "rotation", q)
                    // physics test
                    addC(this, cube, "BoxCollider")
                    addC(this, cube, "Rigidbody")
                    val meshComp = Path(cube, "MeshComponent", 0, 'c')
                    set(meshComp, "isInstanced", true)
                }
            }
        } // */

        // normal testing
        /*val normalTesting = addE(this, world, "Normal Testing")
        val l = 100
        for (i in 0 until l) {
            for (j in 0 until 2) {
                val cube = addE(this, normalTesting, "Cube[$i]", cubePathNormals)
                val angle = (i + (j.and(1) * 0.5)) * 6.2830 / l
                set(cube, "position", Vector3d(cos(angle) * l / 2, 1.0 + 2 * j, sin(angle) * l / 2))
                set(cube, "rotation", Quaterniond().rotateY(-angle).rotateX(j * 6.2830 / 8))
            }
        }*/


        // row of planets
        /*val spherePath = OS.documents.getChild("sphere.obj")
        val planets = addE(this, world, "Planets")
        for (i in -50..50) {
            val size = 10.0.pow(i.toDouble())
            val sphere = addE(this, planets, "Sphere 1e$i", spherePath)
            set(sphere, "position", Vector3d(0.0, 0.0, 3.0 * size))
            set(sphere, "scale", Vector3d(size))
        }*/ // */

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