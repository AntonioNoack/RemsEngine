package me.anno.tests.terrain

import me.anno.Engine
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
import me.anno.engine.ui.render.ECSMeshShader
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
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
import me.anno.maths.noise.PerlinNoise
import me.anno.studio.StudioBase
import me.anno.utils.OS.documents
import me.anno.utils.OS.pictures
import org.joml.Vector3d
import org.joml.Vector4f

fun main() {

    // https://www.youtube.com/watch?v=jw00MbIJcrk

    // todo render a plane of grass blades
    // todo - gpu spawning based on density texture
    // todo - gpu culling

    val densitySource = pictures.getChild("Maps/GardeningFloorPlan.png")
    val mesh = MeshCache[documents.getChild("GrassBlade.obj")]!!.clone()

    val size = 1000
    mesh.proceduralLength = size * size

    val terrainSize = 64
    val heightMap = PerlinNoise(1234L, 3, 0.5f, -30f, 30f, Vector4f(3f / terrainSize))

    val invMaxDensity = 0.2f
    mesh.material = Material().apply {
        isDoubleSided = true // for debugging
        shader = object : ECSMeshShader("foliage") {

            val terrainTexture by lazy {
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
                                Variable(GLSLType.V1F, "time"),
                            )
                return ShaderStage(
                    "vertex",
                    variables, defines +
                            "int px = gl_InstanceID % 1000;\n" +
                            "int py = gl_InstanceID / 1000;\n" +
                            "vec2 uv0 = vec2(px,py) / 1000.0;\n" +
                            "float seed = random(uv0);\n" +
                            "bool zero = texture(densityTex,uv0).x < fract(seed * 41.0);\n" +
                            "float terrain = texture(terrainTex,uv0).x + 0.5;\n" + // why is +0.1 needed?
                            "vec2 center = vec2(px,py)*$invMaxDensity;\n" +
                            "center += vec2(1,0) * rot(seed*6.2832*31.0);\n" +
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
                terrainTexture.bind(shader, "terrainTex", GPUFiltering.TRULY_LINEAR, Clamping.CLAMP)
                density.bind(shader, "densityTex", GPUFiltering.TRULY_LINEAR, Clamping.CLAMP)
                shader.v1f("time", Engine.gameTimeF)
            }
        }
    }.ref

    val scene = Entity()
    scene.add(MeshComponent(mesh))

    val terrainMesh = Mesh()
    terrainMesh.material = Material().apply {
        diffuseBase.set(0.02f, 0.22f, 0.02f, 1f)
        diffuseMap = densitySource
    }.ref

    val cellSize = size * invMaxDensity / (terrainSize - 1)
    TerrainUtils.generateRegularQuadHeightMesh(terrainSize, terrainSize, 0, terrainSize, false, cellSize, terrainMesh,
        { heightMap.getSmooth((it % terrainSize).toFloat(), (it / terrainSize).toFloat()) }, { -1 })
    TerrainUtils.fillUVs(terrainMesh)

    scene.add(Entity().apply {
        val dx = terrainSize * cellSize * 0.5
        position = Vector3d(dx, 0.0, dx)
        add(MeshComponent(terrainMesh))
    })

    testSceneWithUI(scene) {
        StudioBase.instance?.enableVSync = false
    }

}