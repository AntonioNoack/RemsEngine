package me.anno.tests.terrain

import me.anno.Engine
import me.anno.config.DefaultConfig
import me.anno.ecs.Entity
import me.anno.ecs.components.cache.MeshCache
import me.anno.ecs.components.mesh.Material
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.sdf.SDFComponent.Companion.quatRot
import me.anno.ecs.components.mesh.sdf.SDFComposer.sdfConstants
import me.anno.ecs.components.mesh.sdf.modifiers.SDFNoise.Companion.generalNoise
import me.anno.ecs.components.mesh.sdf.modifiers.SDFNoise.Companion.perlinNoise
import me.anno.ecs.components.mesh.terrain.TerrainUtils
import me.anno.ecs.prefab.PrefabInspector
import me.anno.engine.ui.EditorState
import me.anno.engine.ui.render.*
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Renderer
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderFuncLib.noiseFunc
import me.anno.gpu.shader.ShaderLib
import me.anno.gpu.shader.builder.ShaderStage
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.GPUFiltering
import me.anno.gpu.texture.Texture2D
import me.anno.gpu.texture.TextureLib.blackTexture
import me.anno.image.ImageGPUCache
import me.anno.image.raw.FloatImage
import me.anno.io.files.FileReference
import me.anno.maths.Maths.TAU
import me.anno.maths.Maths.max
import me.anno.maths.Maths.sq
import me.anno.maths.noise.PerlinNoise
import me.anno.studio.StudioBase
import me.anno.ui.custom.CustomList
import me.anno.ui.debug.TestStudio.Companion.testUI
import me.anno.utils.OS.documents
import me.anno.utils.OS.pictures
import me.anno.utils.pooling.JomlPools
import org.joml.Vector3d
import org.joml.Vector4f
import kotlin.math.*

class FoliageShader(
    var maxDensity: Float,
    val terrainTexture: Lazy<Texture2D>,
    val densitySource: FileReference,
    val previousStages: List<Mesh>,
    val rv: RenderView? = null
) : ECSMeshShader("foliage") {

    override fun createVertexStage(
        isInstanced: Boolean,
        isAnimated: Boolean,
        colors: Boolean,
        motionVectors: Boolean,
        limitedTransform: Boolean
    ): ShaderStage {
        val defines = createDefines(isInstanced, isAnimated, colors, motionVectors, limitedTransform)
        val variables =
            createVertexVariables(isInstanced, isAnimated, colors, motionVectors, limitedTransform) +
                    listOf(
                        Variable(GLSLType.S2D, "terrainTex"),
                        Variable(GLSLType.S2D, "densityTex"),
                        Variable(GLSLType.V1F, "camRotY"),
                        Variable(GLSLType.V2F, "camPosXZ"),
                        Variable(GLSLType.V1F, "fovQ"),
                        Variable(GLSLType.V1F, "time"),
                        Variable(GLSLType.V1F, "index0"),
                        Variable(GLSLType.V1F, "invMaxDensity"),
                        Variable(GLSLType.V1F, "temporalStabilityFactor")
                    )
        return ShaderStage(
            "vertex",
            variables, defines +
                    "float idXDensity = float(gl_InstanceID) * temporalStabilityFactor + index0;\n" +
                    "int   px0 = int(sqrt(idXDensity/fovQ));\n" +
                    "float pz0 = idXDensity - px0*(px0+1)*fovQ;\n" +
                    "int px = int(round(-sin(camRotY) * float(px0) + cos(camRotY) * pz0 + camPosXZ.x));\n" +
                    "int pz = int(round(-cos(camRotY) * float(px0) - sin(camRotY) * pz0 + camPosXZ.y));\n" +
                    "vec2 uv0 = vec2(px,pz) / 1000.0;\n" +
                    "float seed = random(uv0);\n" +
                    "bool zero = false;//texture(densityTex,uv0).x < fract(seed * 41.0);\n" +
                    "float terrain = abs(uv0.x-0.5)<0.5 && abs(uv0.y-0.5)<0.5 ? texture(terrainTex,uv0).x + 0.5 : 0.0;\n" + // todo why is +0.5 needed?
                    "vec2 center = vec2(px,pz)*invMaxDensity;\n" +
                    "center += vec2(0.5,0.0) * rot(seed*6.2832*31.0);\n" +
                    "float scale = zero ? 0.0 : mix(0.7, 1.2, seed);\n" +
                    "vec2 pos = (coords.xz * scale) * rot(seed*6.2832*17.0) + center;\n" +
                    "float animation = 0.3 * sin((uv0.x+uv0.y)*30.0 + 3.0*time + sdfVoronoi(vec3(uv0*30.0,0.5*time), vec2(2.0, 0.5)));\n" +
                    "localPosition = vec3(pos.x,terrain+coords.y*scale*mix(1.0,0.6366,abs(animation)),pos.y) + \n" +
                    "   vec3(1,0,1) * (coords.y * coords.y * sign(coords.y) * animation);\n" +
                    motionVectorInit +
                    normalInitCode +
                    applyTransformCode +
                    "#ifdef COLORS\n" +
                    "   vertexColor0 = mix(vec4(0.02,0.22,0.02,1),vec4(0.72,0.6,0.73,1), coords.y*0.5);\n" +
                    "   vertexColor1 = vec4(1.0);\n" +
                    "   vertexColor2 = vec4(1.0);\n" +
                    "   vertexColor3 = vec4(1.0);\n" +
                    "   uv = uvs;\n" +
                    "#endif\n" +
                    "gl_Position = transform * vec4(finalPosition, 1.0);\n" +
                    motionVectorCode +
                    ShaderLib.positionPostProcessing
        ).apply {
            add(quatRot)
            add(noiseFunc)
            add(sdfConstants)
            add(generalNoise)
            add(perlinNoise)
        }
    }

    override fun bind(shader: Shader, renderer: Renderer, instanced: Boolean) {
        super.bind(shader, renderer, instanced)
        val density = ImageGPUCache[densitySource, false] ?: blackTexture
        terrainTexture.value.bind(shader, "terrainTex", GPUFiltering.TRULY_LINEAR, Clamping.CLAMP)
        density.bind(shader, "densityTex", GPUFiltering.TRULY_LINEAR, Clamping.CLAMP)
        shader.v1f("time", Engine.gameTimeF)

        val rv = rv ?: RenderView.currentInstance!!
        val pos = rv.cameraPosition
        val dir = rv.cameraDirection
        val rot = rv.cameraRotation
        val camRotY = rot.getEulerAnglesYXZ(JomlPools.vec3d.borrow()).y.toFloat()
        shader.v1f("camRotY", camRotY)

        val frustum = rv.pipeline.frustum
        val p0 = frustum.planes[0]
        val p1 = frustum.planes[1]
        var angle2d = PI - (atan2(p0.x, p0.z) - atan2(p1.x, p1.z))
        if (angle2d > TAU) angle2d -= TAU
        angle2d = max(angle2d, 0.09)

        // calculate, where the down-vector hits the plane
        // offset needs to be increased massively, when we look down
        val dirY = max(dir.y, -0.999)
        val tn = dirY / sqrt(1 - sq(dirY)) // tan(asin(x)) = x/sqrt(1-x*x)
        val height = max(pos.y, 0.0)
        val offset = height * tn

        // add extra offset for what the bottom sees compared to the horizon
        val extra = max(-dirY, 0.0) * tan(RenderState.fovYRadians * 0.5) * height

        val fovQ = tan(angle2d * 0.5)
        shader.v2f(
            "camPosXZ",
            ((pos.x - (offset - extra) * sin(camRotY)) * maxDensity).toFloat(),
            ((pos.z - (offset - extra) * cos(camRotY)) * maxDensity).toFloat()
        )

        shader.v1f("fovQ", fovQ)
        val z0 = max(0, -(offset * maxDensity).toInt())

        // todo shorten procedural length by upper plane when looking down

        // twice as dense to prevent flickering;
        // I'd love a better solution...
        val temporalStabilityFactor = 0.5f

        // offset by number of meshes in other LODs
        val lodIndexOffset = temporalStabilityFactor * previousStages.sumOf { it.proceduralLength }
        shader.v1f("temporalStabilityFactor", temporalStabilityFactor)
        shader.v1f("index0", sq(z0) * fovQ + lodIndexOffset)
        shader.v1f("invMaxDensity", 1f / maxDensity)

    }
}

fun main() {

    // https://www.youtube.com/watch?v=jw00MbIJcrk

    // done render a plane of grass blades
    // done - gpu spawning based on density texture
    // done partially - gpu culling

    val densitySource = pictures.getChild("Maps/GardeningFloorPlan.png")
    val mesh0 = MeshCache[documents.getChild("GrassBlade0.obj")]!!.clone()
    val mesh1 = MeshCache[documents.getChild("GrassBlade1.obj")]!!.clone()

    val size = 1000
    // 50k detailed meshes + 10M just-triangles 😃
    mesh0.proceduralLength = 50_000
    mesh1.proceduralLength = 10_000_000

    val terrainSize = 64
    val heightMap = PerlinNoise(1234L, 3, 0.5f, -30f, 30f, Vector4f(3f / terrainSize))

    val terrainTexture = lazy {
        val terrainImage = FloatImage(terrainSize, terrainSize, 1)
        for (y in 0 until terrainSize) {
            for (x in 0 until terrainSize) {
                terrainImage.setValue(x, y, 0, heightMap.getSmooth(x.toFloat(), y.toFloat()))
            }
        }
        val terrainTexture = Texture2D("terrain", terrainSize, terrainSize, 1)
        terrainTexture.createMonochrome(terrainImage.data, false)
        terrainTexture
    }

    val maxDensity = 5f
    val invMaxDensity = 1f / maxDensity


    val terrainMesh = Mesh()
    terrainMesh.material = Material().apply {
        diffuseBase.set(0.02f, 0.22f, 0.02f, 1f)
        diffuseMap = densitySource
    }.ref

    val cellSize = size * invMaxDensity / (terrainSize - 1)
    TerrainUtils.generateRegularQuadHeightMesh(terrainSize, terrainSize, 0, terrainSize, false, cellSize, terrainMesh,
        { heightMap.getSmooth((it % terrainSize).toFloat(), (it / terrainSize).toFloat()) }, { -1 })
    TerrainUtils.fillUVs(terrainMesh)

    val scene = Entity()
    scene.add(Entity().apply {
        val dx = terrainSize * cellSize * 0.5
        position = Vector3d(dx, 0.0, dx)
        add(MeshComponent(terrainMesh))
    })

    testUI {

        val scene2 = scene.ref
        EditorState.prefabSource = scene2
        PrefabInspector.currentInspector = PrefabInspector(scene2)

        // we need a second camera for testing the first camera
        //  - render debug camera view
        //  - render mesh around tested camera

        val sv0 = SceneView(EditorState, PlayMode.EDITING, DefaultConfig.style)
        val sv1 = SceneView(EditorState, PlayMode.EDITING, DefaultConfig.style)
        val list = CustomList(false, DefaultConfig.style)
        list.add(sv0, 1f)
        list.add(sv1, 1f)

        mesh0.material = Material().apply {
            isDoubleSided = true
            shader = FoliageShader(maxDensity, terrainTexture, densitySource, emptyList(), sv0.renderer)
        }.ref
        mesh1.material = Material().apply {
            isDoubleSided = true
            shader = FoliageShader(maxDensity, terrainTexture, densitySource, listOf(mesh0), sv0.renderer)
        }.ref

        scene.add(MeshComponent(mesh0))
        scene.add(MeshComponent(mesh1))
        StudioBase.instance?.enableVSync = false

        list
    }

}