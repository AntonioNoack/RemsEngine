package me.anno.tests.game

import me.anno.config.DefaultConfig.style
import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.components.camera.control.CameraController
import me.anno.ecs.components.camera.control.OrbitControls
import me.anno.ecs.components.mesh.Material
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.shapes.CylinderModel
import me.anno.ecs.components.mesh.terrain.TerrainUtils
import me.anno.ecs.components.shaders.AutoTileableMaterial
import me.anno.ecs.components.shaders.AutoTileableShader
import me.anno.ecs.components.shaders.PlanarMaterialBase
import me.anno.ecs.interfaces.InputListener
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.ui.render.ECSMeshShader
import me.anno.engine.ui.render.PlayMode
import me.anno.engine.ui.render.RenderView1
import me.anno.engine.ui.render.SceneView
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderFuncLib
import me.anno.gpu.shader.ShaderLib
import me.anno.gpu.shader.builder.ShaderStage
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.GPUFiltering
import me.anno.maths.Maths.sq
import me.anno.maths.noise.PerlinNoise
import me.anno.sdf.SDFGroup
import me.anno.sdf.shapes.SDFBezierCurve
import me.anno.tests.engine.text.TextMeshComponent
import me.anno.ui.base.Font
import me.anno.ui.base.components.AxisAlignment
import me.anno.ui.debug.TestStudio.Companion.testUI3
import me.anno.utils.Color.black
import me.anno.utils.OS.pictures
import me.anno.utils.structures.arrays.IntArrayList
import me.anno.utils.types.Floats.toRadians
import org.joml.Vector2f
import org.joml.Vector3d
import org.joml.Vector4f
import kotlin.math.sin
import kotlin.math.sqrt

// todo develop a typing tower defense game like Tantan does
//  - different mobs
//    - they get stronger
//    - they drop better loot
//  - different towers
//    - you get stronger
//    - things get pricier
//  - collect coin / gems
//  - use them to buy towers
//  - defend your castle (?)

// todo load/find castle meshes from Synty

object AutoTileableShader2 : ECSMeshShader("auto-tileable") {
    override fun createFragmentStages(key: ShaderKey): List<ShaderStage> {
        val vars = createFragmentVariables(key).filter { it.name != "uv" }.toMutableList()
        vars.add(Variable(GLSLType.V1B, "anisotropic"))
        vars.add(Variable(GLSLType.V3F, "tileOffset"))
        vars.add(Variable(GLSLType.V3F, "tilingU"))
        vars.add(Variable(GLSLType.V3F, "tilingV"))
        vars.add(Variable(GLSLType.V1B, "anisotropic"))
        vars.add(Variable(GLSLType.S2D, "invLUTTex0"))
        vars.add(Variable(GLSLType.S2D, "invLUTTex1"))
        vars.add(Variable(GLSLType.M2x2, "worldToLat"))
        vars.add(Variable(GLSLType.M2x2, "latToWorld"))
        vars.add(Variable(GLSLType.V3F, "localPosition"))
        return listOf(
            ShaderStage(
                "material", vars,
                discardByCullingPlane +
                        // step by step define all material properties
                        "vec3 colorPos = finalPosition - tileOffset;\n" +
                        "vec2 uv = vec2(dot(colorPos, tilingU), dot(colorPos, tilingV));\n" +
                        "vec3 stone = sampleAutoTileableTexture(emissiveMap, invLUTTex1, uv).rgb;\n" +
                        "vec3 grass = sampleAutoTileableTexture(diffuseMap, invLUTTex0, uv).rgb;\n" +
                        "float grassy = smoothstep(0.1, 0.8, localPosition.y);\n" +
                        "vec3 color = mix(stone * 0.35 * vec3(0.5,0.4,0.2), grass, grassy);\n" +
                        normalTanBitanCalculation +
                        "color = mix(color, stone * 0.5, smoothstep(0.32, 0.55, dot(finalNormal.xz,finalNormal.xz)));\n" +
                        "finalColor = mix(color, vec3(brightness(color)), 0.5);\n" + // desaturate
                        "finalAlpha = 1.0;\n" +
                        "mat3 tbn = mat3(finalTangent, finalBitangent, finalNormal);\n" +
                        "finalEmissive  = vec3(0.0);\n" +
                        "finalOcclusion = 0.0;\n" +
                        "finalMetallic  = 0.0;\n" +
                        "finalRoughness = 1.0;\n" +
                        reflectionCalculation +
                        v0 + sheenCalculation +
                        clearCoatCalculation +
                        finalMotionCalculation
            ).add(ShaderLib.rgb2yuv).add(ShaderLib.yuv2rgb).add(ShaderLib.anisotropic16)
                .add(ShaderFuncLib.randomGLSL).add(ShaderLib.brightness)
                .add(AutoTileableShader.getTexture).add(AutoTileableShader.sampleTile)
        )
    }
}

class AutoTileableMaterial2 : PlanarMaterialBase() {

    var anisotropic = true

    init {
        shader = AutoTileableShader2
    }

    override fun bind(shader: Shader) {
        super.bind(shader)
        shader.v1b("anisotropic", anisotropic)
        // could be customizable, but who would use that?
        shader.m2x2("latToWorld", AutoTileableShader.latToWorld)
        shader.m2x2("worldToLat", AutoTileableShader.worldToLat)
        AutoTileableMaterial.lookUp(diffuseMap).bind(shader, "invLUTTex0", GPUFiltering.TRULY_LINEAR, Clamping.CLAMP)
        AutoTileableMaterial.lookUp(emissiveMap).bind(shader, "invLUTTex1", GPUFiltering.TRULY_LINEAR, Clamping.CLAMP)
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        dst as AutoTileableMaterial
        dst.anisotropic = anisotropic
    }

    override val className: String get() = "AutoTileableMaterial"
}

fun main() {

    val towerSpawnPlaces = listOf(
        // top towers
        Vector2f(25f, 15f),
        Vector2f(50f, 10f),
        Vector2f(80f, 18f),
        Vector2f(130f, 8f),
        Vector2f(180f, 15f),
        // middle left
        Vector2f(20f, 42f),
        Vector2f(35f, 43f),
        Vector2f(49f, 47f),
        Vector2f(62f, 60f),
        Vector2f(28f, 75f),
        Vector2f(50f, 72f),
        // middle right
        Vector2f(120f, 40f),
        Vector2f(150f, 59f),
        Vector2f(180f, 55f),
        // bottom towers
        Vector2f(22f, 105f),
        Vector2f(48f, 100f),
        Vector2f(100f, 105f),
        Vector2f(130f, 90f),
        Vector2f(152f, 85f),
    )

    val size = Vector2f(200f, 120f)

    val sc = Vector4f(size.x, (size.x + size.y) * 0.5f, size.y, 1f)
    val roads = SDFGroup().apply {
        smoothness = 20f
        addChild(SDFBezierCurve().apply {
            points = arrayListOf(
                Vector4f(-1.3f, 0f, -2.9f, 0.2f),
                Vector4f(-1.3f, 0f, -0.1f, 0.1f),
                Vector4f(-0.3f, 0f, 0.3f, 0f),
            ).apply { for (v in this) v.mul(sc) }
        })
        addChild(SDFBezierCurve().apply {
            points = arrayListOf(
                Vector4f(-0.3f, 0f, 0.3f, 0f),
                Vector4f(0.45f, 0f, 0.1f, 0f),
                Vector4f(0.46f, 0f, 0.5f, 0f),
            ).apply { for (v in this) v.mul(sc) }
        })
        addChild(SDFBezierCurve().apply {
            points = arrayListOf(
                Vector4f(-2.20f, 0f, 0.5f, 0.2f),
                Vector4f(-1.30f, 0f, -0.3f, 0.1f),
                Vector4f(-0.3f, 0f, 0.7f, 0f),
            ).apply { for (v in this) v.mul(sc) }
        })
        addChild(SDFBezierCurve().apply {
            points = arrayListOf(
                Vector4f(-0.3f, 0f, 0.7f, 0f),
                Vector4f(0.70f, 0f, 0.85f, 0f),
                Vector4f(0.46f, 0f, 0.5f, 0f),
            ).apply { for (v in this) v.mul(sc) }
        })
        addChild(SDFBezierCurve().apply {
            points = arrayListOf(
                Vector4f(0.4f, 0f, 0.55f, 0f),
                Vector4f(0.65f, 0f, 0.65f, 0f),
                Vector4f(0.65f, 0f, 0.75f, 0f),
            ).apply { for (v in this) v.mul(sc) }
        })
        addChild(SDFBezierCurve().apply {
            points = arrayListOf(
                Vector4f(0.65f, 0f, 0.75f, 0f),
                Vector4f(0.55f, 0f, 0.1f, 0f),
                Vector4f(1.3f, 0f, 0.3f, 0f),
            ).apply { for (v in this) v.mul(sc) }
        })
    }

    val scene = Entity()
    val terrain = Entity("Terrain", scene)

    // generate terrain like Tantan did, or similar...
    //  -> he just used a texture, but I don't want to save big images for tests...

    val lod0Terrain = Mesh()
    val lod1Terrain = Mesh()
    val terrainMaterial = AutoTileableMaterial2().apply {
        diffuseMap = pictures.getChild("Textures/grass.jpg")
        emissiveMap = pictures.getChild("Textures/pattern-9.jpg")
    }
    lod0Terrain.material = terrainMaterial.ref
    lod1Terrain.material = terrainMaterial.ref

    val perlin = PerlinNoise(1234L, 7, 0.6f, -2f, 12f, Vector4f(0.02f))
    val seeds = IntArrayList(0)

    val rx = 512
    val ry = (rx * size.y / size.x).toInt()

    fun heightCurve(x: Float): Float {
        return 0.5f + (x - 1f) / (1f + sq(x - 1f)) + x * (sin(x) + 2) / 20
    }

    fun heightCurve2(xi: Int, zi: Int): Float {
        val distanceToRoad = roads.computeSDF(Vector4f(xi * size.x / rx, 0f, zi * size.y / ry, 0f), seeds)
        return heightCurve(distanceToRoad * 0.1f) * perlin.getSmooth(xi.toFloat(), zi.toFloat())
    }

    TerrainUtils.generateRegularQuadHeightMesh(
        rx * 2, ry * 2, false, size.x / (rx - 1),
        lod0Terrain, { x, y ->
            val xi = x - rx / 2
            val zi = y - ry
            heightCurve2(xi, zi)
        }, null
    )

    val lod1Factor = 3
    TerrainUtils.generateRegularQuadHeightMesh(
        rx * 2, ry * 2, false, size.x / (rx - 1) * lod1Factor,
        lod1Terrain, { x, y ->
            val xi = x * lod1Factor - rx * (lod1Factor + 2) / 2
            val zi = y * lod1Factor - ry * lod1Factor
            heightCurve2(xi, zi)
        }, null
    )

    terrain.setPosition(0.0, 0.0, -size.y * 0.5)
    terrain.add(MeshComponent(lod0Terrain))
    terrain.add(Entity().apply {
        setPosition(0.0, -0.5, 0.0)
        add(MeshComponent(lod1Terrain))
    })

    val towers = Entity("Towers", scene)

    val towerBaseMesh = CylinderModel.createMesh(
        6, 2, true, false, emptyList(), 1f, Mesh(),
        -9f, -1f, 5f
    )
    towerBaseMesh.makeFlatShaded()
    val dark = Material.diffuse(0x333333 or black)
    towerBaseMesh.materials = listOf(dark.ref, dark.ref, dark.ref)

    val font = Font("Verdana", 20f, false, false)
    for ((idx, place) in towerSpawnPlaces.withIndex()) {
        val position = Vector3d(
            place.x - size.x * 0.5,
            heightCurve2(
                (place.x * rx / size.x).toInt(),
                (place.y * ry / size.y).toInt()
            ) + 4.0,
            place.y - size.y * 0.5
        )
        val entity = Entity(towers)
            .add(MeshComponent(towerBaseMesh))
        entity.transform.localPosition = position
        val tm = TextMeshComponent("${'A' + idx}", font, AxisAlignment.CENTER)
        entity.add(tm)
    }

    val monsters = Entity("Monsters", scene)
    val ui = SceneView(RenderView1(PlayMode.PLAYING, scene, style), style)
    val typingListener = object : Component(), InputListener {
        override fun onCharTyped(codepoint: Int): Boolean {
            return super.onCharTyped(codepoint)
        }
    }
    val camera = object : OrbitControls() {
        override fun clampRotation() {
            // allow rotation, just very limited,
            //  todo and make it feel soft
            rotation = rotation
                .max((-66f).toRadians(), (-10f).toRadians(), 0f)
                .min((-54f).toRadians(), (+10f).toRadians(), 0f)
        }
    }
    camera.rotationSpeed = 0.03f
    camera.mouseWheelSpeed = 0.03f
    camera.minRadius = 150f
    camera.maxRadius = 700f
    camera.position.set(0.0, 0.0, 0.0)
    camera.radius = sqrt(camera.minRadius * camera.maxRadius)
    camera.rotation.set((-60f).toRadians(), 0f.toRadians(), 0f)
    val camEntity = CameraController.setup(camera, ui.renderer)
    val cam1 = ui.renderer.localPlayer!!.cameraState.currentCamera!!
    // slim fov to be close to orthographic
    cam1.fovY = 20f

    scene.add(camEntity)
    scene.add(typingListener)
    if (false) {
        testUI3("Typing Tower Defense", ui)
    } else {
        testSceneWithUI("Typing Tower Defense", scene)
    }
}