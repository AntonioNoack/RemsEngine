package me.anno.tests.gfx

import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.*
import me.anno.ecs.components.mesh.shapes.IcosahedronModel
import me.anno.engine.ui.render.RenderState
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.gpu.CullMode
import me.anno.gpu.GFX
import me.anno.gpu.GFXState
import me.anno.gpu.GFXState.renderPurely
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.M4x3Delta.m4x3delta
import me.anno.gpu.buffer.*
import me.anno.gpu.deferred.DeferredSettings
import me.anno.gpu.drawing.DrawRectangles.drawRect
import me.anno.gpu.drawing.DrawTextures.drawDepthTexture
import me.anno.gpu.drawing.DrawTextures.drawTexture
import me.anno.gpu.framebuffer.*
import me.anno.gpu.framebuffer.Framebuffer.Companion.drawBuffersN
import me.anno.gpu.pipeline.Pipeline
import me.anno.gpu.shader.BufferCompute.createAccessors
import me.anno.gpu.shader.ComputeShader
import me.anno.gpu.shader.ComputeTextureMode
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.texture.ITexture2D
import me.anno.gpu.texture.Texture2D
import me.anno.gpu.texture.TextureCache
import me.anno.gpu.texture.TextureLib.whiteTexture
import me.anno.input.Input
import me.anno.io.files.FileReference.Companion.getReference
import me.anno.mesh.Shapes.flatCube
import me.anno.tests.shader.drawMovablePoints
import me.anno.ui.debug.TestDrawPanel.Companion.testDrawing
import me.anno.utils.Color
import me.anno.utils.structures.arrays.ExpandingFloatArray
import me.anno.utils.structures.maps.LazyMap
import me.anno.utils.types.Floats.toRadians
import org.joml.*
import kotlin.math.max
import kotlin.math.min

data class ShaderKey(
    val shader: Shader,
    val target: DeferredSettings?,
    val meshAttr: List<Attribute>,
    val instAttr: List<Attribute>,
    val indexed: AttributeType?,
    val drawMode: DrawMode
)

// todo create a software rasterizer for compute shaders
//  - Unreal Engine devs said it was more efficient for small triangles -> let's do the same to render millions of tiny triangles
fun main() {
    if (false) {
        testCopyColorToDepth()
    } else if (false) {
        testRasterizerAlgorithm()
    } else {
        computeRasterizer()
    }
}

fun testCopyColorToDepth() {
    val w = 256
    val h = 256
    val depthDst = Framebuffer("dd", w, h, emptyArray(), DepthBufferType.TEXTURE)
    val colorDst = Framebuffer("cd", w, h, arrayOf(TargetType.UByteTarget4), DepthBufferType.NONE)
    testDrawing("Copy Depth") {
        val depthSrc = TextureCache[getReference("res://icon.png"), false]
        if (depthSrc != null) {
            useFrame(depthDst) {
                depthDst.clearColor(0, true)
                if (!Input.isAltDown) {
                    GFX.copyColorAndDepth(whiteTexture, depthSrc)
                }
            }
            drawDepthTexture(it.x, it.y + h, w, -h, depthDst.depthTexture!!)
            useFrame(colorDst) {
                colorDst.clearColor(0, false)
                if (Input.isShiftDown) {
                    GFX.copy(depthDst.depthTexture!!)
                } else if (!Input.isControlDown) {
                    GFX.copyColorAndDepth(depthDst.depthTexture!!, whiteTexture)
                }
            }
            drawTexture(it.x + w, it.y, w, h, colorDst.getTexture0())
        }
    }
}

fun testRasterizerAlgorithm() {
    drawMovablePoints("CPU Rasterizer", 3) { panel, points ->
        val points1 = points.map { Vector2i(it.x.toInt(), it.y.toInt()) }
        val ua = points1[0]
        val ub = points1[1]
        val uc = points1[2]
        val minX = max(min(ua.x, min(ub.x, uc.x)), panel.x)
        val maxX = min(max(ua.x, max(ub.x, uc.x)), panel.x + panel.width - 1)
        val minY = max(min(ua.y, min(ub.y, uc.y)), panel.y)
        val maxY = min(max(ua.y, max(ub.y, uc.y)), panel.y + panel.height - 1)
        for (y in minY..maxY) {
            var minX1 = maxX
            var maxX1 = minX
            fun union(a: Vector2i, b: Vector2i) {
                if (y <= max(a.y, b.y) && y >= min(a.y, b.y)) {
                    if (a.y == b.y) {
                        minX1 = min(minX1, min(a.x, b.x))
                        maxX1 = max(maxX1, max(a.x, b.x))
                    } else {
                        val x = a.x + (b.x - a.x) * (y - a.y) / (b.y - a.y)
                        minX1 = min(minX1, x)
                        maxX1 = max(maxX1, x)
                    }
                }
            }
            union(ua, ub)
            union(ub, uc)
            union(uc, ua)
            drawRect(minX1, y, maxX1 - minX1 + 1, 1, -1)
        }
        for (i in 0 until 3) {
            val c3 = 0x00ff00 or Color.black
            val scale = panel.scale
            val r = (3f * scale.toFloat()).toInt()
            drawRect(points1[i].x, points1[i].y, r, r, c3)
        }
    }
}

fun Mesh.createUniqueIndices() {
    data class Vertex(
        val x: Float,
        val y: Float,
        val z: Float,
        val nx: Float,
        val ny: Float,
        val nz: Float,
        val u: Float,
        val v: Float,
    )

    val vertices = HashMap<Vertex, Int>()
    val newIndices = IntArray(numPrimitives.toInt() * 3)
    var ii = 0
    val pos = positions!!
    val nor = normals!!
    val uv = uvs!!
    val newPos = ExpandingFloatArray(pos.size)
    val newNor = ExpandingFloatArray(nor.size)
    val newUVs = ExpandingFloatArray(uv.size)
    forEachTriangleIndex { ai, bi, ci ->
        for (i in intArrayOf(ai, bi, ci)) {
            val x = pos[i * 3]
            val y = pos[i * 3 + 1]
            val z = pos[i * 3 + 2]
            val nx = nor[i * 3]
            val ny = nor[i * 3 + 1]
            val nz = nor[i * 3 + 2]
            val u = uv[i * 2]
            val v = uv[i * 2 + 1]
            newIndices[ii++] = vertices.getOrPut(Vertex(x, y, z, nx, ny, nz, u, v)) {
                newPos.add(x, y, z)
                newNor.add(nx, ny, nz)
                newUVs.add(u, v)
                vertices.size
            }
        }
    }
    positions = newPos.toFloatArray()
    normals = newNor.toFloatArray()
    uvs = newUVs.toFloatArray()
    indices = newIndices
    invalidateGeometry()
}

fun computeRasterizer() {

    // done first step: create an IMesh
    // done create lots of small triangles for testing

    val shaders = LazyMap<ShaderKey, ComputeShader> { (shader, target, meshAttr, instAttr, indexed, drawMode) ->

        // todo respect draw mode

        ComputeShader(
            "rasterizer", Vector3i(1024, 1, 1), listOf(
                Variable(GLSLType.V1I, "numPrimitives"),
                Variable(GLSLType.V1I, "numInstances"),
                Variable(GLSLType.M4x3, "localTransform"),
                Variable(GLSLType.M4x4, "transform"),
                Variable(GLSLType.V2I, "viewportSize"),
            ), "" +
                    createAccessors(meshAttr, listOf(Attribute("coords", 3)), "Mesh", 0, false) +
                    createAccessors(instAttr, listOf(), "Inst", 1, false) +
                    "layout(r32f, binding = 0) coherent uniform image2D depthTex;\n" +
                    (target?.layers2 ?: emptyList()).withIndex().joinToString("") { (idx, layer) ->
                        "layout(rgba32f, binding = ${idx + 1}) uniform image2D ${layer.name};\n"
                    } +
                    (if (indexed != null) {
                        "layout(std430, set = 0, binding = 2) buffer IndexBuffer {\n" +
                                "    uint data[];\n" +
                                "} Indices;\n"
                    } else "") +
                    "struct Pixel {\n" +
                    "   ivec2 uv;\n" +
                    "   vec4 glPosition;\n" +
                    // todo add all varyings
                    "};\n" +
                    "Pixel projectPixel(uint vertexId){\n" +
                    // todo load all attributes, that we need
                    "   vec3 coord = getMeshCoords(vertexId);\n" +
                    // todo coord -> localPosition -> finalPosition -> gl_Position -> /w [-1,1] -> [0,w]
                    "   coord = matMul(localTransform,vec4(coord,1.0));\n" +

                    // todo execute vertex shader

                    "   vec4 glPosition = matMul(transform,vec4(coord,1.0));\n" +
                    "   glPosition.xyz /= glPosition.w;\n" +
                    "   ivec2 uv = ivec2((glPosition.xy*.5+.5)*vec2(viewportSize));\n" +
                    "   return Pixel(uv,glPosition);\n" +
                    "}\n" +
                    "void setPixel(Pixel pixel){\n" +
                    "   ivec2 uv = pixel.uv;\n" +
                    "   float depth = pixel.glPosition.z;\n" +
                    "   if(uv.x >= 0 && uv.y >= 0 && uv.x < viewportSize.x && uv.y < viewportSize.y){\n" +
                    "       if(imageLoad(depthTex,uv).x < depth){\n" +
                    "           imageStore(depthTex, uv, vec4(depth));\n" +
                    // todo write proper values into all targets
                    (target?.layers2 ?: emptyList()).joinToString("") { layer ->
                        "   imageStore(${layer.name}, uv, vec4(1.0));\n"
                    } +
                    "       }\n" +
                    "   }\n" +
                    "}\n" +
                    "void union1(ivec2 a, ivec2 b, int y, inout int minX, inout int maxX){\n" +
                    "   if(y <= max(a.y,b.y) && y >= min(a.y,b.y)){\n" +
                    "       if(a.y == b.y){\n" +
                    "           minX = min(minX,min(a.x,b.x));\n" +
                    "           maxX = max(maxX,max(a.x,b.x));\n" +
                    "       } else {\n" +
                    "           int x = a.x + (b.x-a.x) * (y-a.y)/(b.y-a.y);\n" +
                    "           minX = min(minX,x);\n" +
                    "           maxX = max(maxX,x);\n" +
                    "       }\n" +
                    "   }\n" +
                    "}\n" +
                    // todo change algorithm for line strips and triangle strips (?)
                    "uint getIndex(uint triIdx){\n" +
                    when (indexed) {
                        AttributeType.UINT32 -> "return Indices.data[triIdx];\n"
                        AttributeType.UINT16 -> "return (Indices.data[triIdx >> 1] << (16-16*(triIdx&1))) >> 16;\n" // correct order?
                        else -> "return triIdx;\n"
                    } +
                    "}\n" +
                    "void main(){\n" +
                    "   if(gl_GlobalInvocationID.x >= numPrimitives * numInstances) return;\n" +
                    "   uint instanceId = gl_GlobalInvocationID.x / numPrimitives;\n" +
                    "   uint primitiveId = gl_GlobalInvocationID.x % numPrimitives;\n" +
                    when (drawMode) {
                        DrawMode.POINTS -> {
                            "setPixel(projectPixel(getIndex(primitiveId)));\n"
                        }
                        DrawMode.LINES, DrawMode.LINE_STRIP -> {
                            "" +
                                    "   Pixel ua = projectPixel(getIndex(primitiveId*2u));\n" +
                                    "   Pixel ub = projectPixel(getIndex(primitiveId*2u+1u));\n" +
                                    // backside culling
                                    "   if(min(ua.glPosition.w,ub.glPosition.w) <= 0.0) return;\n" +
                                    "   int minX = max(min(ua.uv.x,ub.uv.x),0);\n" +
                                    "   int maxX = min(max(ua.uv.x,ub.uv.x),viewportSize.x-1);\n" +
                                    "   int minY = max(min(ua.uv.y,ub.uv.y),0);\n" +
                                    "   int maxY = min(max(ua.uv.y,ub.uv.y),viewportSize.y-1);\n" +
                                    "   if(maxY-minY > maxX-minX){\n" +
                                    "       for(int y=minY;y<=maxY;y++){\n" +
                                    "           int x = ua.uv.x + (ub.uv.x-ua.uv.x) * (y-ua.uv.y) / (ub.uv.y-ua.uv.y);\n" +
                                    // todo interpolate correctly
                                    "           setPixel(Pixel(ivec2(x,y),ua.glPosition));\n" +
                                    "       }\n" +
                                    "   } else if(maxX > minX){\n" +
                                    "       for(int x=minX;x<=maxX;x++){\n" +
                                    "           int y = ua.uv.y + (ub.uv.y-ua.uv.y) * (x-ua.uv.x) / (ub.uv.x-ua.uv.x);\n" +
                                    // todo interpolate correctly
                                    "           setPixel(Pixel(ivec2(x,y),ua.glPosition));\n" +
                                    "       }\n" +
                                    "   } else {\n" +
                                    "       setPixel(ua);\n" +
                                    "   }\n"
                        }
                        DrawMode.TRIANGLES, DrawMode.TRIANGLE_STRIP -> {
                            "" +
                                    "   Pixel ua = projectPixel(getIndex(primitiveId*3u));\n" +
                                    "   Pixel ub = projectPixel(getIndex(primitiveId*3u+1u));\n" +
                                    "   Pixel uc = projectPixel(getIndex(primitiveId*3u+2u));\n" +
                                    // backside culling
                                    "   if(min(ua.glPosition.w,min(ub.glPosition.w,uc.glPosition.w)) <= 0.0) return;\n" +
                                    // backface culling
                                    "   if(cross(vec3(ub.uv-ua.uv,0.0), vec3(uc.uv-ua.uv,0.0)).z <= 0.0) return;\n" +
                                    "   int minX = max(min(ua.uv.x,min(ub.uv.x,uc.uv.x)),0);\n" +
                                    "   int maxX = min(max(ua.uv.x,max(ub.uv.x,uc.uv.x)),viewportSize.x-1);\n" +
                                    "   int minY = max(min(ua.uv.y,min(ub.uv.y,uc.uv.y)),0);\n" +
                                    "   int maxY = min(max(ua.uv.y,max(ub.uv.y,uc.uv.y)),viewportSize.y-1);\n" +
                                    "   if((maxY-minY+1)*(maxX-minX+1) > 1000) return;\n" + // discard too large triangles
                                    "   for(int y=minY;y<=maxY;y++){\n" +
                                    // calculate minX and maxX on this line
                                    "       int minX1 = maxX, maxX1 = minX;\n" +
                                    // todo each line only has two corresponding lines, find them (?)
                                    "       union1(ua.uv,ub.uv,y,minX1,maxX1);\n" +
                                    "       union1(ub.uv,uc.uv,y,minX1,maxX1);\n" +
                                    "       union1(uc.uv,ua.uv,y,minX1,maxX1);\n" +
                                    "       minX1 = max(minX1,minX);\n" +
                                    "       maxX1 = min(maxX1,maxX);\n" +
                                    "       for(int x=minX1;x<=maxX;x++){\n" +
                                    // todo interpolate correctly
                                    "           setPixel(Pixel(ivec2(x,y),ua.glPosition));\n" +
                                    "       }\n" +
                                    "   }\n"
                        }
                        else -> {}
                    } +
                    // todo rasterize big triangles as a group
                    //  assumption: there won't be many of them
                    "}\n"
        )
    }

    val mesh = IcosahedronModel.createIcosphere(7)
    // mesh.createUniqueIndices()

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
            if (Input.isShiftDown) {
                mesh.draw(shader, materialIndex, drawLines)
            } else {
                drawInstanced0(shader, materialIndex, null, drawLines)
            }
        }

        override fun drawInstanced(shader: Shader, materialIndex: Int, instanceData: Buffer, drawLines: Boolean) {
            if (Input.isShiftDown) {
                mesh.drawInstanced(shader, materialIndex, instanceData, drawLines)
            } else {
                drawInstanced0(shader, materialIndex, instanceData, drawLines)
            }
        }

        fun drawInstanced0(shader: Shader, materialIndex: Int, instanceData: Buffer?, drawLines: Boolean) {
            GFXState.cullMode.use(CullMode.BOTH) {
                renderPurely {
                    drawInstanced1(shader, materialIndex, instanceData, drawLines)
                }
            }
        }

        fun drawInstanced1(shader: Shader, materialIndex: Int, instanceData: Buffer?, drawLines: Boolean) {

            mesh.ensureBuffer()
            val deferredSettings = GFXState.currentRenderer.deferredSettings
            if (drawLines) mesh.ensureDebugLines()
            val triBuffer = if (drawLines) mesh.debugLineBuffer else mesh.triBuffer
            val target = GFXState.currentBuffer

            // copy depth to color
            val depthAsColor = getDepthTarget(target)

            val key = ShaderKey(
                shader, deferredSettings, mesh.buffer!!.attributes,
                instanceData?.attributes ?: emptyList(),
                triBuffer?.elementsType,
                if (drawLines) DrawMode.LINES else mesh.drawMode
            )

            val rasterizer = shaders[key]
            rasterizer.use()

            bindBuffers(rasterizer, instanceData, triBuffer)
            bindUniforms(rasterizer, materialIndex, instanceData, target, drawLines)
            bindTargets(rasterizer, depthAsColor, deferredSettings, target)

            rasterizer.runBySize(mesh.numPrimitives.toInt() * (instanceData?.drawLength ?: 1))

            writeDepth(target, depthAsColor)
        }

        private fun getDepthTarget(target: IFramebuffer): Texture2D {
            val depthAsColor = FBStack[
                "depthAsColor", target.width, target.height,
                arrayOf(TargetType.FloatTarget1), 1, DepthBufferType.NONE
            ]
            useFrame(depthAsColor) {
                GFX.copy(target.depthTexture!!)
            }
            return depthAsColor.getTexture0() as Texture2D
        }

        private fun writeDepth(target: IFramebuffer, depthAsColor: ITexture2D) {
            // copy depth from writable depth
            // disable all colors being written
            drawBuffersN(0)
            GFX.copyColorAndDepth(whiteTexture, depthAsColor)
            drawBuffersN(target.numTextures)
        }

        private fun bindBuffers(rasterizer: ComputeShader, instanceBuffer: Buffer?, indexBuffer: IndexBuffer?) {
            rasterizer.bindBuffer(0, mesh.buffer!!)
            if (instanceBuffer != null) {
                instanceBuffer.ensureBuffer()
                rasterizer.bindBuffer(1, instanceBuffer)
            }
            if (indexBuffer != null) {
                rasterizer.bindBuffer(2, indexBuffer)
            }
        }

        private fun bindUniforms(
            rasterizer: ComputeShader, materialIndex: Int,
            instanceData: Buffer?, target: IFramebuffer,
            drawLines: Boolean,
        ) {
            val material = MaterialCache[mesh.materials.getOrNull(materialIndex), Material.defaultMaterial]
            material.bind(rasterizer)
            rasterizer.v1i(
                "numPrimitives",
                if (drawLines) mesh.debugLineBuffer!!.elementCount // todo why is soo much missing???
                else mesh.numPrimitives.toInt()
            )
            rasterizer.v1i("numInstances", instanceData?.drawLength ?: 1)
            rasterizer.v2i("viewportSize", target.width, target.height)
            rasterizer.m4x3delta("localTransform", Matrix4x3d())
            rasterizer.m4x4("transform", RenderState.cameraMatrix)
        }

        private fun bindTargets(
            rasterizer: ComputeShader, depthAsColor: Texture2D,
            deferredSettings: DeferredSettings?,
            target: IFramebuffer
        ) {
            rasterizer.bindTexture(0, depthAsColor, ComputeTextureMode.READ_WRITE)
            if (deferredSettings != null) {
                for (i in 0 until deferredSettings.layers2.size) {
                    rasterizer.bindTexture(i + 1, target.getTextureI(i) as Texture2D, ComputeTextureMode.WRITE)
                }
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