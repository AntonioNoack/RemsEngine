package me.anno.tests.gfx

import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.IMesh
import me.anno.ecs.components.mesh.Material
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.MeshComponentBase
import me.anno.ecs.components.mesh.shapes.IcosahedronModel
import me.anno.engine.ui.render.RenderState
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.gpu.DepthMode
import me.anno.gpu.GFX
import me.anno.gpu.GFXState
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.M4x3Delta.m4x3delta
import me.anno.gpu.buffer.Attribute
import me.anno.gpu.buffer.AttributeType
import me.anno.gpu.buffer.Buffer
import me.anno.gpu.deferred.DeferredSettings
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.framebuffer.Framebuffer.Companion.drawBuffersN
import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.pipeline.Pipeline
import me.anno.gpu.shader.BufferCompute.createAccessors
import me.anno.gpu.shader.ComputeShader
import me.anno.gpu.shader.ComputeTextureMode
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.texture.Texture2D
import me.anno.gpu.texture.TextureLib.whiteTexture
import me.anno.input.Input
import me.anno.mesh.Shapes.flatCube
import me.anno.utils.structures.maps.LazyMap
import me.anno.utils.types.Floats.toRadians
import org.joml.*

data class ShaderKey(
    val target: DeferredSettings?,
    val meshAttr: List<Attribute>,
    val instAttr: List<Attribute>,
    val indexed: AttributeType?
)

// todo create a software rasterizer for compute shaders
//  - Unreal Engine devs said it was more efficient for small triangles -> let's do the same to render millions of tiny triangles
fun main() {

    // done first step: create an IMesh
    // done create lots of small triangles for testing

    val shaders = LazyMap<ShaderKey, ComputeShader> { (target, meshAttr, instAttr, indexed) ->
        // todo if indexed, use indices
        ComputeShader(
            "rasterizer", Vector3i(1024, 1, 1), listOf(
                Variable(GLSLType.V1I, "numTriangles"),
                Variable(GLSLType.V1I, "numInstances"),
                Variable(GLSLType.M4x3, "localTransform"),
                Variable(GLSLType.M4x4, "transform"),
            ), "" +
                    createAccessors(meshAttr, listOf(Attribute("coords", 3)), "Mesh", 0, false) +
                    createAccessors(instAttr, listOf(), "Inst", 1, false) +
                    "layout(r32f, binding = 0) writeonly uniform image2D depthTex;\n" +
                    (target?.layers2 ?: emptyList()).withIndex().joinToString("") { (idx, layer) ->
                        "layout(rgba32f, binding = ${idx + 1}) uniform image2D ${layer.name};\n"
                    } +
                    "void drawPixel(vec3 coord){\n" +
                    // todo coord -> localPosition -> finalPosition -> gl_Position -> /w [-1,1] -> [0,w]
                    "   coord = matMul(localTransform,vec4(coord,1.0));\n" +
                    "   vec4 glPosition = matMul(transform,vec4(coord,1.0));\n" +
                    "   if(!(glPosition.z > 0.0 && glPosition.z < glPosition.w)){\n" +
                    "       return;\n" +
                    "   }\n" +
                    "   glPosition.xyz /= glPosition.w;\n" +
                    "   ivec2 size = imageSize(depthTex);\n" +
                    "   ivec2 uv = ivec2((glPosition.xy*.5+.5)*vec2(size));\n" +
                    "   if(uv.x >= 0 && uv.y >= 0 && uv.x < size.x && uv.y < size.y){\n" +
                    "       imageStore(depthTex, uv, vec4(glPosition.z));\n" +
                    // todo write proper values into all targets
                    (target?.layers2 ?: emptyList()).joinToString("") { layer ->
                        "   imageStore(${layer.name}, uv, vec4(1.0));\n"
                    } +
                    "   }\n" +
                    "}\n" +
                    "void main(){\n" +
                    "   uint triangleId = gl_GlobalInvocationID.x;\n" +
                    "   uint instanceId = gl_GlobalInvocationID.y;\n" +
                    "   if(triangleId < numTriangles && instanceId < numInstances){\n" +
                    "       vec3 ca = getMeshCoords(triangleId*3u);\n" +
                    "       vec3 cb = getMeshCoords(triangleId*3u+1u);\n" +
                    "       vec3 cc = getMeshCoords(triangleId*3u+2u);\n" +
                    "       drawPixel(ca);\n" +
                    "       drawPixel(cb);\n" +
                    "       drawPixel(cc);\n" +
                    "   }\n" +
                    "}\n"
        ).apply {
            printCode()
        }
    }

    val mesh = IcosahedronModel.createIcosphere(5)
    lateinit var component: Component
    val iMesh = object : IMesh {

        override val numPrimitives: Long
            get() = mesh.numPrimitives

        override fun ensureBuffer() {
            mesh.ensureBuffer()
        }

        override fun getBounds(): AABBf {
            return mesh.getBounds()
        }

        override fun draw(shader: Shader, materialIndex: Int, drawLines: Boolean) {
            mesh.draw(shader, materialIndex, drawLines)
            // todo implement this
        }

        override fun drawInstanced(shader: Shader, materialIndex: Int, instanceData: Buffer, drawLines: Boolean) {
            if (Input.isShiftDown) {
                mesh.drawInstanced(shader, materialIndex, instanceData, drawLines)
            } else {
                mesh.ensureBuffer()
                val rm = GFXState.currentRenderer
                val ds = rm.deferredSettings
                val triBuffer = mesh.triBuffer
                val target = GFXState.currentBuffer

                val writeableDepth = FBStack[
                    "depth", target.width, target.height,
                    TargetType.FloatTarget1, 1, DepthBufferType.NONE
                ]

                // copy depth to writeableDepth
                useFrame(writeableDepth) {
                    GFXState.depthMode.use(DepthMode.ALWAYS) {
                        GFX.copy(target.depthTexture!!)
                    }
                }

                instanceData.ensureBuffer()

                val key = ShaderKey(ds, mesh.buffer!!.attributes, instanceData.attributes, triBuffer?.elementsType)
                val shader = shaders[key]
                shader.use()
                shader.v1i("numTriangles", mesh.numPrimitives.toInt())
                shader.v1i("numInstances", instanceData.drawLength)

                // bind all buffers
                shader.bindBuffer(0, mesh.buffer!!)
                shader.bindBuffer(1, instanceData)
                if (triBuffer != null) {
                    shader.bindBuffer(2, triBuffer)
                }

                // todo bind all uniforms
                shader.m4x3delta("localTransform", Matrix4x3d())
                shader.m4x4("transform", RenderState.cameraMatrix)

                // bind all targets
                shader.bindTexture(0, writeableDepth.getTexture0() as Texture2D, ComputeTextureMode.READ_WRITE)
                if (ds != null) {
                    for (i in 0 until ds.layers2.size) {
                        shader.bindTexture(i + 1, target.getTextureI(i) as Texture2D, ComputeTextureMode.WRITE)
                    }
                }

                shader.runBySize(mesh.numPrimitives.toInt(), instanceData.drawLength)

                // copy depth from writable depth
                // disable all colors being written
                drawBuffersN(0)
                GFX.copyColorAndDepth(whiteTexture, writeableDepth.getTexture0())
                drawBuffersN(target.numTextures)
            }
        }

        override fun fill(pipeline: Pipeline, entity: Entity, clickId: Int): Int {
            val material = Material.defaultMaterial
            pipeline.findStage(material)
                .add(component, this, entity, material, 0)
            return clickId + 1
        }
    }
    val comp = object : MeshComponentBase() {
        override fun getMeshOrNull() = iMesh
    }
    component = comp

    val scene = Entity("Scene")
    for (i in 0 until 40) {
        val da = (45.0 * i).toRadians()
        val db = (30.0 * (i / 8 - 2)).toRadians()
        scene.add(
            Entity(MeshComponent(flatCube.linear(Vector3f(), Vector3f(1f)).front))
                .apply {
                    position = Vector3d(0.0, 0.0, 2.0)
                        .rotateX(db)
                        .rotateY(da)
                }
                .setScale(0.6)
        )
    }
    scene.add(comp)
    testSceneWithUI("Compute Rasterizer", scene)
}