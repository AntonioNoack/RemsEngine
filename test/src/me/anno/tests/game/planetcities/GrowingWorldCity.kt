package me.anno.tests.game.planetcities

import me.anno.Time
import me.anno.config.DefaultConfig.style
import me.anno.ecs.Entity
import me.anno.ecs.components.camera.control.CameraController.Companion.setup
import me.anno.ecs.components.camera.control.OrbitControls
import me.anno.ecs.components.chunks.spherical.Hexagon
import me.anno.ecs.components.chunks.spherical.HexagonSphere
import me.anno.ecs.components.chunks.spherical.HexagonSphere.Companion.TRIANGLE_COUNT
import me.anno.ecs.components.mesh.Material
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshCache.transformMesh
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.engine.ECSRegistry
import me.anno.engine.EngineBase.Companion.workspace
import me.anno.engine.ui.ECSTreeView
import me.anno.engine.ui.EditorState
import me.anno.engine.ui.render.PlayMode
import me.anno.engine.ui.render.RenderView1
import me.anno.engine.ui.render.SceneView
import me.anno.input.Input
import me.anno.io.ISaveable
import me.anno.maths.Maths
import me.anno.maths.Maths.clamp
import me.anno.maths.Maths.fract
import me.anno.maths.Maths.random
import me.anno.maths.noise.PerlinNoise
import me.anno.tests.mesh.hexagons.createFaceMesh
import me.anno.ui.UIColors.dodgerBlue
import me.anno.ui.UIColors.paleGoldenRod
import me.anno.ui.base.components.AxisAlignment
import me.anno.ui.base.groups.NineTilePanel
import me.anno.ui.base.groups.PanelListX
import me.anno.ui.base.scrolling.ScrollPanelY
import me.anno.ui.base.text.UpdatingTextPanel
import me.anno.ui.custom.CustomList
import me.anno.ui.debug.TestEngine.Companion.testUI
import me.anno.ui.editor.PropertyInspector
import me.anno.utils.Color.black
import me.anno.utils.Color.mixARGB
import me.anno.utils.Color.white
import me.anno.utils.Color.withAlpha
import me.anno.utils.OS.documents
import me.anno.utils.types.Vectors.normalToQuaternionY
import org.joml.Matrix4x3d
import org.joml.Quaterniond
import org.joml.Quaternionf
import org.joml.Vector3d
import org.joml.Vector3f
import org.joml.Vector4f
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sign
import kotlin.math.sin

// todo you build a world on a HexSphere, on tiles with fixed sockets (?)
//  - you can copy tiles ?
//  - you can move tiles ? probably sensible...
//  - you build in triangle/hexagon voxels???
//  - for some amount of money, you can blow up your planet by one row to grow further, new tiles get added in between (mapping from one planet to another)
//  - ecologic balance, always keep some amount of water, trees, farmland, deserts, ... ?
//  - water simulation, water having sources and disappearing by sun (always) on the surface??
//  - earth is growing -> volcanoes, earth quakes? 😃
//  - build freely, grid free, but buildings cannot be placed on borders

// how big shall be a tile?
//  - small living region -> ~250m seems nice
//  - large enough for any house

// todo voxel editor for buildings???
//  - set a scale, so it can be placed properly
//  - should allow for easy fan additions
//  - features need to be paid for -> make all things balanced ^^, e.g. 5% buff -> 4% cost somewhere

// todo UI/lore system? we could write a nice story for it rn xD
//  dear planner, We finally found a new growing planet, and like before, you're to build another planetary city.
//  when we get the funds, we can fire up the planet's core, and it will expand. Gravity will stay the same, but
//  growing comes at the usual price of small potential mishaps.
//  build a civilisation that makes you proud, our pockets swallow, and our customizers happy

// todo -> pentagons are blocked tiles for filling up the planet with lava

// todo technicals:
//  - do we rotate the camera or the planet? planet seems easier
//  - save system? hierarchical db
//  - building blocks #visuals?
//    - we find a nice library
//    - we use Synty meshes

// todo economy simulation?
//  - how big can planets grow?
//  - it would be nice if we get from caring about families, to moving around districts
//  -> citizens will be planned together; if the user needs better UI, we can improvise

// todo terrain editing in the future? we need a good base grid for that,
//  then apply the same calculation to all hexagons in the affected area

// todo shake when a volcano breaks out xD

// todo controls:
//  - 2d ui at sides
//  - 3d ui in center,
//  click & drag/place
//   - select thing to build
//   - move it
//   - place it

// todo path finding, and traversal for transport (cars, trains, planes, ...)

enum class Biome(val color: Int) {
    DESERT(paleGoldenRod),
    PLAINS(0x4aff60),
    FOREST(0x1F6D2D),
    SNOW(white),
    WATER(dodgerBlue)
}

val noise = PerlinNoise(1234L, 3, 0.5f, -1f, 1f, Vector4f(2f))
fun getBiome(hexagon: Hexagon): Biome {
    val pos = Vector3f(hexagon.center).add(100f)
    val y = noise[pos.x * 5f, pos.y * 5f, pos.z * 5f] * 0.3f + (pos.y - 100f)
    return when {
        noise[pos.x, y, pos.z] > 0f -> Biome.WATER
        abs(y) > 0.9f -> Biome.SNOW
        abs(y) < 0.2f -> Biome.DESERT
        fract(noise[pos.x, y, pos.z] * 10f) > 0f -> Biome.FOREST
        else -> Biome.PLAINS
    }
}

fun main() {

    ECSRegistry.init()

    workspace = documents.getChild("RemsEngine/YandereSim")

    val trees = listOf(1, 2, 3, 4).map {
        workspace.getChild("Foliage/SM_Env_Tree_0$it.json")
    }

    val grass = listOf(1, 2, 3, 4).map {
        workspace.getChild("Foliage/SM_Env_Grass_0$it.json")
    }

    val tileSize = 250.0

    val n = 5
    val scene = Entity("Scene")
    val planetRotation = Quaterniond()
    val world = HexagonSphere(n, n)
    val scale = tileSize * PI / (2.0 * world.len)
    val planet = Entity("Planet", scene)
    planet.setPosition(0.0, -scale, 0.0)
    val tmpQ = Quaternionf()
    for (tri in 0 until TRIANGLE_COUNT) {
        val bySide = Entity("Side$tri", planet)
        for (s in 0 until world.chunkCount) {
            for (t in 0 until world.chunkCount - s) {
                val chunk = world.queryChunk(tri, s, t)
                for (hexagon in chunk) {
                    val mesh = createFaceMesh(Mesh(), chunk)
                    val biome = getBiome(chunk.first())
                    val color = mixARGB(
                        mixARGB(biome.color, white, random().toFloat() * 0.5f),
                        black, random().toFloat() * 0.5f
                    ).withAlpha(255)
                    mesh.material = Material.diffuse(color).ref
                    transformMesh(mesh, Matrix4x3d().scale(scale))
                    bySide.add(MeshComponent(mesh))

                    // interesting, motivating visuals:
                    //  - biomes: desert, plains, forest, snow, water
                    //  - biome decoration: nothing/pyramid, nothing, trees, nothing, nothing

                    val assets = when (biome) {
                        Biome.FOREST -> trees
                        Biome.PLAINS -> grass
                        else -> emptyList()
                    }
                    if (assets.isNotEmpty()) {
                        // place some assets
                        val c = hexagon.center
                        val dx = hexagon.corners[0]
                        val dy = hexagon.corners[1]
                        for (i in 0 until 27) {
                            val asset = assets[(random() * assets.size).toInt()]
                            // place asset randomly onto hexagon
                            // rotate it
                            val instance = Entity("Asset", MeshComponent(asset))
                            // todo this placement logic needs improvement
                            val pos = c.lerp(dx, random().toFloat() * 2f - 1f, Vector3f())
                            pos.lerp(dy, random().toFloat() * 2f - 1f)
                            instance.transform.localPosition = Vector3d(pos).mul(scale)
                            instance.transform.localRotation = instance.transform.localRotation
                                .set(c.normalToQuaternionY(tmpQ))
                            instance.transform.teleportUpdate()
                            bySide.add(instance)
                        }
                    }
                }
            }
        }
    }
    val ui = NineTilePanel(style)
    val rv = RenderView1(PlayMode.PLAYING, scene, style)

    val controls = object : OrbitControls() {
        override fun clampRotation() {
            this.rotation.x = clamp(this.rotation.x, -Maths.PIf * 0.5f, 0f)
        }

        override fun onMouseMoved(x: Float, y: Float, dx: Float, dy: Float): Boolean {
            val speed = 2.0 / rv.height
            if (Input.isRightDown) {
                // rotating camera
                super.onMouseMoved(x, y, 0f, dy)
                planetRotation.rotateLocalY(sign(y - (rv.y + rv.height / 2)) * dx * speed)
                planet.transform.localRotation = planetRotation
                planet.transform.smoothUpdate()
                planet.invalidateAABBsCompletely()
            }
            if (Input.isLeftDown) {
                // handle planet rotation
                planetRotation.rotateLocalZ(-dx * speed)
                planetRotation.rotateLocalX(+dy * speed)
                planet.transform.localRotation = planetRotation
                planet.transform.smoothUpdate()
                planet.invalidateAABBsCompletely()
            }
            return true
        }
    }

    controls.rotation.set(-1.57f, 0f, 0f)
    controls.needsClickToRotate = true
    controls.rotateRight = true
    controls.movementSpeed = 0f
    controls.radius = 50f
    controls.minRadius = 10f
    controls.maxRadius = controls.radius * 100f
    controls.position.set(0f, 0f, 0f) // todo this varies with terrain height...
    scene.add(setup(controls, rv))

    // rv.renderMode = RenderMode.SHOW_AABB

    val sv = SceneView(rv, style)
    ui.add(sv)
    val stats = PanelListX(style)
    stats.add(UpdatingTextPanel(250, style) { "Money: ${(1000 * sin(Time.gameTime)).toInt() * 10}" })
    stats.alignmentX = AxisAlignment.CENTER
    stats.alignmentY = AxisAlignment.MAX
    ui.add(stats)
    val ui2 = CustomList(false, style)
    ui2.add(ScrollPanelY(object :
        ECSTreeView(style) {
        override fun listSources(): List<ISaveable> {
            return listOf(scene)
        }
    }.apply {
        alignmentX = AxisAlignment.MIN
        weight = 1f
    }, style).apply {
        alignmentX = AxisAlignment.MIN
        weight = 1f
    }, 1f)
    ui2.add(ui, 5f)
    ui2.add(PropertyInspector({ EditorState.selection }, style).apply {
        alignmentX = AxisAlignment.MAX
    }, 1f)
    testUI("Planet Cities", ui2)
}