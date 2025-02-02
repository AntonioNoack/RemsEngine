package me.anno.tests.terrain

import me.anno.Time
import me.anno.config.DefaultConfig
import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.material.Material
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshCache
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.terrain.TerrainUtils
import me.anno.ecs.prefab.PrefabInspector
import me.anno.engine.EngineBase
import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.EditorState
import me.anno.engine.ui.render.ECSMeshShader
import me.anno.engine.ui.render.PlayMode
import me.anno.engine.ui.render.RenderState
import me.anno.engine.ui.render.RenderView
import me.anno.engine.ui.render.SceneView
import me.anno.gpu.CullMode
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderFuncLib.randomGLSL
import me.anno.gpu.shader.ShaderLib.quatRot
import me.anno.gpu.shader.builder.ShaderStage
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.gpu.shader.renderer.Renderer
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.Filtering
import me.anno.gpu.texture.Texture2D
import me.anno.gpu.texture.TextureCache
import me.anno.gpu.texture.TextureLib.blackTexture
import me.anno.image.raw.FloatImage
import me.anno.io.files.FileReference
import me.anno.maths.Maths.TAU
import me.anno.maths.Maths.clamp
import me.anno.maths.Maths.max
import me.anno.maths.Maths.sq
import me.anno.maths.noise.PerlinNoise
import me.anno.sdf.SDFComposer.sdfConstants
import me.anno.sdf.modifiers.SDFNoise.Companion.generalNoise
import me.anno.sdf.modifiers.SDFNoise.Companion.perlinNoise
import me.anno.ui.custom.CustomList
import me.anno.ui.debug.TestEngine.Companion.testUI
import me.anno.utils.OS.documents
import me.anno.utils.OS.pictures
import me.anno.utils.pooling.JomlPools
import org.joml.AABBd
import org.joml.Matrix4x3d
import org.joml.Vector3d
import org.joml.Vector4f
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

class FoliageShader(
    var maxDensity: Float,
    val terrainTexture: Lazy<Texture2D>,
    val densitySource: FileReference,
    val previousStages: List<Mesh>,
    val mesh: Mesh,
    val rv: RenderView? = null
) : ECSMeshShader("foliage") {

    var proceduralBudget = mesh.proceduralLength

    override fun createVertexStages(key: ShaderKey): List<ShaderStage> {
        val animFunc = "" +
                "float sdfVoronoi(vec3,vec2);\n" +
                "float animCurve(vec2 uv0, float time){\n" +
                "   return 0.3 * sin((uv0.x+uv0.y)*30.0 + 3.0*time + sdfVoronoi(vec3(uv0*30.0,0.5*time), vec2(2.0, 0.5)));\n" +
                "}\n"
        val grassStage = ShaderStage(
            "vertex",
            listOf(
                Variable(GLSLType.S2D, "terrainTex"),
                Variable(GLSLType.S2D, "densityTex"),
                Variable(GLSLType.V1F, "camRotY"),
                Variable(GLSLType.V2F, "camPosXZ"),
                Variable(GLSLType.V1F, "fovQ"),
                Variable(GLSLType.V2F, "time"),
                Variable(GLSLType.V1F, "index0"),
                Variable(GLSLType.V1F, "invMaxDensity"),
                Variable(GLSLType.V1F, "temporalStabilityFactor"),
                Variable(GLSLType.V3F, "localPosition", VariableMode.OUT),
                Variable(GLSLType.V3F, "normal", VariableMode.INOUT),
                Variable(GLSLType.V4F, "tangent", VariableMode.INOUT),
                Variable(GLSLType.V3F, "prevLocalPosition", VariableMode.OUT),
                Variable(GLSLType.V4F, "vertexColor0", VariableMode.OUT)
            ), "" +
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
                    "mat2 bladeRot = rot(seed*6.2832*17.0);\n" +
                    "vec2 pos = bladeRot * (coords.xz * scale) + center;\n" +
                    "float animWeight = coords.y * coords.y * sign(coords.y);\n" +
                    "float anim1 = animWeight * animCurve(uv0,time.x);\n" +
                    "vec3 basePos1 = vec3(pos.x,terrain+coords.y*scale*mix(1.0,0.6366,abs(anim1)),pos.y);\n" +
                    "localPosition = basePos1 + vec3(anim1,0.0,anim1);\n" +
                    "#ifdef MOTION_VECTORS\n" +
                    "   float anim2 = animWeight * animCurve(uv0,time.y);\n" +
                    "   vec3 basePos2 = vec3(pos.x,terrain+coords.y*scale*mix(1.0,0.6366,abs(anim2)),pos.y);\n" +
                    "   prevLocalPosition = basePos2 + vec3(anim2,0.0,anim2);\n" +
                    "#endif\n" +
                    "#ifdef COLORS\n" +
                    "   normal.xz = bladeRot * normal.xz;\n" +
                    "   tangent.xz = bladeRot * tangent.xz;\n" +
                    "#endif\n" +
                    "#ifdef COLORS\n" +
                    "   vertexColor0 = mix(vec4(0.02,0.22,0.02,1),vec4(0.72,0.6,0.73,1), coords.y*0.5);\n" +
                    "#endif\n"
        ).add(animFunc).add(quatRot).add(randomGLSL).add(sdfConstants).add(generalNoise).add(perlinNoise)
        return createDefines(key) +
                loadVertex(key) +
                grassStage +
                transformVertex(key) +
                finishVertex(key)
    }

    override fun bind(shader: Shader, renderer: Renderer, instanced: Boolean) {
        super.bind(shader, renderer, instanced)
        val density = TextureCache[densitySource, false] ?: blackTexture
        terrainTexture.value.bind(shader, "terrainTex", Filtering.TRULY_LINEAR, Clamping.CLAMP)
        density.bind(shader, "densityTex", Filtering.TRULY_LINEAR, Clamping.CLAMP)
        shader.v2f("time", Time.gameTime.toFloat(), (Time.gameTime - Time.deltaTime).toFloat())

        val rv = rv ?: RenderView.currentInstance!!
        val pos = rv.cameraPosition
        val dir = rv.cameraDirection
        val rot = rv.cameraRotation
        val camRotY = rot.getEulerAnglesYXZ(JomlPools.vec3d.borrow()).y.toFloat()
        shader.v1f("camRotY", camRotY)

        val frustum = rv.pipeline.frustum
        val p0 = frustum.planes[0]
        val p1 = frustum.planes[1]
        var projectedFOV = PI - (atan2(p0.dirX, p0.dirZ) - atan2(p1.dirX, p1.dirZ))
        if (projectedFOV > TAU) projectedFOV -= TAU
        projectedFOV = max(projectedFOV, 0.09)

        // calculate, where the down-vector hits the plane
        // offset needs to be increased massively, when we look down
        val dirY = max(dir.y, -0.999)
        val fovY = rv.fovYRadians
        val forwardAngle = asin(dir.y)
        // val downAngle = clamp(forwardAngle - fovY * 0.5, -PI / 2, -0.001)
        val upAngle = clamp(forwardAngle + fovY * 0.5, -PI / 2, -0.001)

        val tn0 = dirY / sqrt(1 - sq(dirY)) // tan(asin(x)) = x/sqrt(1-x*x)
        val tn1 = -1.0 / tan(upAngle)
        val height = max(pos.y, 0.0)
        val z0h = height * tn0
        val z1h = max(pos.y, 1.0) * tn1

        // add extra offset for what the bottom sees compared to the horizon
        val extra = max(-dirY, 0.0) * tan(RenderState.fovYRadians * 0.5) * height

        val tanProjFOV = tan(projectedFOV * 0.5)
        shader.v2f(
            "camPosXZ",
            ((pos.x - (z0h - extra) * sin(camRotY)) * maxDensity).toFloat(),
            ((pos.z - (z0h - extra) * cos(camRotY)) * maxDensity).toFloat()
        )

        shader.v1f("fovQ", tanProjFOV)
        val z0 = max(0, -(z0h * maxDensity).toInt())
        val z1 = max(0.0, z1h * maxDensity)

        // twice as dense to prevent flickering;
        // I'd love a better solution...
        val temporalStabilityFactor = 0.5f

        // offset by number of meshes in other LODs
        val lodIndexOffset = temporalStabilityFactor * previousStages.sumOf { it.proceduralLength }

        // start index
        val index0 = sq(z0) * tanProjFOV + lodIndexOffset
        val index1 = sq((z1 + z0)) * 2.5f * tanProjFOV // 2.5 is just a guess... the formula is incorrect...

        shader.v1f("temporalStabilityFactor", temporalStabilityFactor)
        shader.v1f("index0", index0)
        shader.v1f("invMaxDensity", 1f / maxDensity)
        mesh.proceduralLength = min(max((index1 - index0).toInt(), 1), proceduralBudget)
    }
}

fun main() {

    OfficialExtensions.initForTests()

    // https://www.youtube.com/watch?v=jw00MbIJcrk

    // done render a plane of grass blades
    // done - gpu spawning based on density texture
    // done partially - gpu culling

    val densitySource = pictures.getChild("Maps/GardeningFloorPlan.png")
    val mesh0 = MeshCache[documents.getChild("GrassBlade0.obj")]!!.clone() as Mesh
    val mesh1 = MeshCache[documents.getChild("GrassBlade1.obj")]!!.clone() as Mesh

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
    TerrainUtils.generateRegularQuadHeightMesh(
        terrainSize, terrainSize, false, cellSize, terrainMesh,
        { xi, zi -> heightMap.getSmooth(xi.toFloat(), zi.toFloat()) }
    )
    TerrainUtils.fillUVs(terrainMesh)

    val scene = Entity()
    scene.add(Entity().apply {
        val dx = terrainSize * cellSize * 0.5
        position = Vector3d(dx, 0.0, dx)
        add(MeshComponent(terrainMesh))
    })

    testUI("Foliage") {

        val scene2 = scene.ref
        EditorState.prefabSource = scene2
        PrefabInspector.currentInspector = PrefabInspector(scene2)

        // we need a second camera for testing the first camera
        //  - render debug camera view
        //  - render mesh around tested camera

        val sv0 = SceneView(PlayMode.EDITING, DefaultConfig.style)
        val sv1 = SceneView(PlayMode.EDITING, DefaultConfig.style)
        val list = CustomList(false, DefaultConfig.style)
        list.add(sv0, 1f)
        list.add(sv1, 1f)

        val grassTranslucency = 0.9f
        mesh0.material = Material().apply {
            cullMode = CullMode.BOTH
            translucency = grassTranslucency
            shader = FoliageShader(maxDensity, terrainTexture, densitySource, emptyList(), mesh0, sv0.renderView)
        }.ref
        mesh1.material = Material().apply {
            cullMode = CullMode.BOTH
            translucency = grassTranslucency
            shader = FoliageShader(maxDensity, terrainTexture, densitySource, listOf(mesh0), mesh1, sv0.renderView)
        }.ref

        class AllMeshComp(mesh: Mesh) : MeshComponent(mesh) {
            override fun fillSpace(globalTransform: Matrix4x3d, dstUnion: AABBd): Boolean {
                localAABB.all()
                globalAABB.all()
                dstUnion.all()
                return true
            }
        }
        scene.add(AllMeshComp(mesh0))
        scene.add(AllMeshComp(mesh1))
        EngineBase.enableVSync = false

        list
    }
}