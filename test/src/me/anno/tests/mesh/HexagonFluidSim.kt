package me.anno.tests.mesh

import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.components.anim.AnimTexture
import me.anno.ecs.components.chunks.spherical.HexagonSphere
import me.anno.ecs.components.mesh.Material
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.sdf.SDFComponent
import me.anno.engine.ui.render.ECSMeshShader
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.gpu.GFX.flat01
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Renderer
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderLib
import me.anno.gpu.shader.ShaderLib.uvList
import me.anno.gpu.shader.builder.ShaderStage
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.gpu.texture.Texture2D
import me.anno.maths.Maths.ceilDiv
import me.anno.maths.Maths.min
import me.anno.maths.noise.PerlinNoise
import me.anno.studio.StudioBase
import me.anno.utils.Color.rgba
import org.joml.Vector4f
import kotlin.math.sqrt

// the simulation makes waves, but they are awkward...
fun main() {

    val n = 160
    val scene = Entity()
    val hexagons = HexagonSphere.createHexSphere(n)

    val fluidMesh = Mesh()
    createNiceMesh0(fluidMesh, hexagons) { hex, _ ->
        val i = hex.index
        rgba(
            i.and(255), i.shr(8).and(255),
            i.shr(16).and(255), i.shr(24).and(255)
        )
    }


    val terrainMesh = Mesh()
    val terrainPerlin = PerlinNoise(1234L, 8, 0.5f, 1f, 1f + 5f / (n + 1), Vector4f(n / 10f))
    val fluidPerlin = 1f / (n + 1)

    val inset = 0.2f
    createNiceMesh1(terrainMesh, n, hexagons, inset, inset, false, { hex, _ ->
        val i = hex.index
        rgba(
            i.and(255), i.shr(8).and(255),
            i.shr(16).and(255), i.shr(24).and(255)
        )
    }, { hex, v ->
        var h = terrainPerlin[v.x, v.y, v.z]
        for (i in hex.neighborIds) {
            if (i < 0) continue
            val vi = hexagons[i].center
            h = min(h, terrainPerlin[vi.x, vi.y, vi.z])
        }
        h - 0.01f
    }, { _, v -> terrainPerlin[v.x, v.y, v.z] })
    terrainMesh.makeFlatShaded()

    val terrain = MeshComponent(terrainMesh.ref)
    val fluid = MeshComponent(fluidMesh.ref)
    scene.add(Entity("Terrain").apply { add(terrain) })
    scene.add(Entity("Fluid").apply { add(fluid) })

    testSceneWithUI(scene) {
        StudioBase.instance?.enableVSync = true

        val w = sqrt(hexagons.size.toFloat()).toInt()
        val h = ceilDiv(hexagons.size, w)

        val dynamicLayout = arrayOf(
            TargetType.FloatTarget4, // flow x4
            TargetType.FloatTarget4, // flow x2, height, unused
        )

        var dynamicData0 = Framebuffer("data0", w, h, dynamicLayout)
        var dynamicData1 = Framebuffer("data1", w, h, dynamicLayout)
        val staticLayout = arrayOf(
            TargetType.U32x4, // neighbor ids
            TargetType.U32x2,
            TargetType.FloatTarget4 // height, xyz
        )

        val staticData = Array(staticLayout.size) {
            Texture2D("static$it", w, h, 1)
        }

        val s0 = IntArray(w * h * 4)
        for (i in hexagons.indices) {
            val hex = hexagons[i].neighborIds
            val j = i * 4
            s0[j] = hex[0]
            s0[j + 1] = hex[1]
            s0[j + 2] = hex[2]
            s0[j + 3] = hex[3]
        }
        staticData[0].create(staticLayout[0], s0)
        for (i in hexagons.indices) {
            val hex = hexagons[i].neighborIds
            val j = i * 2
            s0[j] = hex[4]
            s0[j + 1] = hex[5]
        }
        staticData[1].create(staticLayout[1], s0)

        val s1 = FloatArray(w * h * 4)
        for (i in hexagons.indices) {
            val hex = hexagons[i].center
            val j = i * 4
            s1[j] = terrainPerlin[hex.x, hex.y, hex.z]
            s1[j + 1] = hex.x
            s1[j + 2] = hex.y
            s1[j + 3] = hex.z
        }
        staticData[2].create(staticLayout[2], s1)

        s1.fill(0f)
        for (i in hexagons.indices) {
            s1[i * 4 + 2] = fluidPerlin
        }
        val initialHeight = Texture2D("height0", w, h, 1)
        initialHeight.create(TargetType.FloatTarget4, s1)

        // store initial data into compute buffers/textures
        dynamicData0.ensure()
        (dynamicData0.getTextureI(1) as Texture2D).copyFrom(initialHeight)
        initialHeight.destroy()

        val shader = Shader(
            "fluid", ShaderLib.coordsList, ShaderLib.simplestVertexShader, uvList,
            listOf(
                Variable(GLSLType.V1I, "totalSize"),
                Variable(GLSLType.V2I, "size"),
                Variable(GLSLType.S2D, "src0Tex"),
                Variable(GLSLType.S2D, "src1Tex"),
                Variable(GLSLType.S2DU, "stat0Tex"),
                Variable(GLSLType.S2DU, "stat1Tex"),
                Variable(GLSLType.S2D, "stat2Tex"),
                Variable(GLSLType.V4F, "pencil"), // xyz, strength
                Variable(GLSLType.V4F, "dst0", VariableMode.OUT),
                Variable(GLSLType.V4F, "dst1", VariableMode.OUT),
            ), "" +
                    "float pencil1(vec3 p){\n" +
                    "   p -= pencil.xyz;\n" +
                    "   return exp(-dot(p,p)) * pencil.w;\n" +
                    "}\n" +
                    "void main() {\n" +
                    "   ivec2 uvi = ivec2(gl_FragCoord);\n" +
                    "   int index = uvi.x + uvi.y * size.x;\n" +
                    "   if(index < totalSize){\n" +
                    "       vec4  src0 = texelFetch(src0Tex,uvi,0);\n" + // flow x4
                    "       vec4  src1 = texelFetch(src1Tex,uvi,0);\n" + // flow x2, height
                    "       uvec4 stat0 = texelFetch(stat0Tex,uvi,0);\n" +
                    "       uvec2 stat1 = texelFetch(stat1Tex,uvi,0).xy;\n" +
                    "       vec4 stat2 = texelFetch(stat2Tex,uvi,0);\n" + // terrain height, xyz
                    // for all neighbors and self, calculate the flow into us
                    "       uint neighborIds[6] = uint[](stat0.x,stat0.y,stat0.z,stat0.w,stat1.x,stat1.y);\n" +
                    "       float flows[6] = float[](src0.x,src0.y,src0.z,src0.w,src1.x,src1.y);" +
                    "       float h0 = src1.z;\n" +
                    "       float t0 = stat2.x + pencil1(stat2.yzw);\n" +
                    "       float hx = h0;\n" +
                    "       float maxSpeed = 0.10;\n" + // theoretical maximum: 1/6
                    "       for(int i=0;i<6;i++){\n" +
                    "           if(neighborIds[i] >= uint(totalSize)) continue;\n" +
                    "           int ni = int(neighborIds[i]);\n" +
                    "           uvi = ivec2(ni % size.x, ni / size.x);\n" +
                    "           float h1 = texelFetch(src1Tex,uvi,0).z;\n" +
                    "           vec4 stat2i = texelFetch(stat2Tex,uvi,0);\n" +
                    "           float t1 = stat2i.x + pencil1(stat2i.yzw);\n" +
                    "           float flowNew = (h1+t1)-(h0+t0);\n" +
                    "           float flowOld = flows[i];\n" +
                    "           flowNew = clamp(mix(flowOld,flowNew,0.001), -h0*maxSpeed, h1*maxSpeed);\n" +
                    "           hx += flowNew;\n" +
                    "           flows[i] = flowNew;\n" +
                    "       }\n" +
                    "       hx = clamp(hx,0.0,3.0-t0);\n" +
                    "       hx = hx > 0.0 ? hx : 1.0;\n" +
                    // apply this flow
                    "       dst0 = vec4(flows[0],flows[1],flows[2],flows[3]);\n" +
                    "       dst1 = vec4(flows[4],flows[5],hx,1.0);\n" +
                    "   } else dst0 = dst1 = vec4(0.0);\n" +
                    "}\n"
        ).apply {
            setTextureIndices(
                "stat0Tex", "stat1Tex", "stat2Tex",
                "src0Tex", "src1Tex",
            )
        }

        fluid.materials = listOf(Material().apply {
            this@apply.shader = object : ECSMeshShader("fluid") {
                override fun bind(shader: Shader, renderer: Renderer, instanced: Boolean) {
                    super.bind(shader, renderer, instanced)
                    dynamicData0.getTextureI(1).bindTrulyNearest(shader, "heightTex")
                    staticData[2].bindTrulyNearest(shader, "terrainTex")
                    shader.v2i("size", w, h)
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
                                    Variable(GLSLType.V2I, "size"),
                                    Variable(GLSLType.S2D, "heightTex"),
                                    Variable(GLSLType.S2D, "terrainTex")
                                )
                    val stage = ShaderStage(
                        "vertex",
                        variables, defines +
                                "int id = int(dot(colors0, vec4(255.0,255.0*256.0,255.0*65536.0,255.0*256.0*65536.0)));\n" +
                                "ivec2 uv0 = ivec2(id%size.x,id/size.x);\n" +
                                "vec3 coords1 = dot(coords,coords) < 1.5 ? coords : coords * (0.5 * (0.1 * texelFetch(heightTex,uv0,0).z + texelFetch(terrainTex,uv0,0).r));\n" +
                                "localPosition = coords1;\n" + // is output, so no declaration needed
                                "#ifdef MOTION_VECTORS\n" +
                                "   vec3 prevLocalPosition = coords1;\n" +
                                "#endif\n" +
                                instancedInitCode +
                                normalInitCode +
                                applyTransformCode +
                                "#ifdef COLORS\n" +
                                "   vertexColor0 = vec4(1.0);\n" +
                                "   vertexColor1 = vec4(1.0);\n" +
                                "   vertexColor2 = vec4(1.0);\n" +
                                "   vertexColor3 = vec4(1.0);\n" +
                                "   uv = uvs;\n" +
                                "#endif\n" +
                                "gl_Position = transform * vec4(finalPosition, 1.0);\n" +
                                motionVectorCode +
                                ShaderLib.positionPostProcessing
                    )
                    if (isAnimated && AnimTexture.useAnimTextures) stage.add(getAnimMatrix)
                    if (limitedTransform) stage.add(SDFComponent.quatRot)
                    return stage
                }
            }
            diffuseBase.set(0.7f, 0.9f, 1f)
        }.ref)

        scene.add(object : Component() {
            override fun clone(): Component = throw NotImplementedError()
            override fun onVisibleUpdate(): Boolean {

                terrainMesh.hasVertexColors = 0

                // calculate update :)
                useFrame(dynamicData1) {
                    shader.use()
                    for (i in staticData.indices) staticData[i].bindTrulyNearest(i)
                    dynamicData0.bindTrulyNearest(staticData.size)
                    shader.v1i("totalSize", hexagons.size)
                    shader.v2i("size", w, h)
                    // val dir = Vector3f(0f, 0f, 1f).rotateY(-Engine.gameTimeF)
                    // shader.v4f("pencil", dir.x, dir.y, dir.z, 1f / (n + 1))
                    flat01.draw(shader)
                }

                val tmp = dynamicData0
                dynamicData0 = dynamicData1
                dynamicData1 = tmp

                return true
            }
        })
    }
}