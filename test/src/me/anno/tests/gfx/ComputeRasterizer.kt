package me.anno.tests.gfx

import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.IMesh
import me.anno.ecs.components.mesh.material.Material
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.MeshComponentBase
import me.anno.ecs.components.mesh.shapes.IcosahedronModel
import me.anno.engine.EngineBase
import me.anno.engine.ui.render.RenderView
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.gpu.CullMode
import me.anno.gpu.GFX
import me.anno.gpu.GFXState
import me.anno.gpu.GFXState.renderPurely
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.buffer.Attribute
import me.anno.gpu.buffer.AttributeType
import me.anno.gpu.buffer.Buffer
import me.anno.gpu.buffer.DrawMode
import me.anno.gpu.buffer.IndexBuffer
import me.anno.gpu.deferred.DeferredSettings
import me.anno.gpu.drawing.DrawRectangles.drawRect
import me.anno.gpu.drawing.DrawTextures.drawDepthTexture
import me.anno.gpu.drawing.DrawTextures.drawTexture
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.framebuffer.Framebuffer.Companion.drawBuffersN
import me.anno.gpu.framebuffer.IFramebuffer
import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.pipeline.Pipeline
import me.anno.gpu.pipeline.PipelineStageImpl.Companion.bindRandomness
import me.anno.gpu.pipeline.PipelineStageImpl.Companion.initShader
import me.anno.gpu.pipeline.PipelineStageImpl.Companion.setupLights
import me.anno.gpu.pipeline.PipelineStageImpl.Companion.setupLocalTransform
import me.anno.gpu.shader.BufferCompute.createAccessors
import me.anno.gpu.shader.ComputeShader
import me.anno.gpu.shader.ComputeTextureMode
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.gpu.texture.ITexture2D
import me.anno.gpu.texture.Texture2D
import me.anno.gpu.texture.TextureCache
import me.anno.gpu.texture.TextureLib.whiteTexture
import me.anno.input.Input
import me.anno.io.files.Reference.getReference
import me.anno.mesh.Shapes.flatCube
import me.anno.tests.shader.drawMovablePoints
import me.anno.ui.debug.TestDrawPanel.Companion.testDrawing
import me.anno.utils.Color
import me.anno.utils.types.Strings.titlecase
import me.anno.utils.structures.arrays.FloatArrayList
import me.anno.utils.structures.maps.LazyMap
import me.anno.utils.types.Floats.toRadians
import org.joml.AABBd
import org.joml.AABBf
import org.joml.Vector2i
import org.joml.Vector3d
import org.joml.Vector3f
import org.joml.Vector3i
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

// create a software rasterizer for compute shaders
//  - Unreal Engine devs said it was more efficient for small triangles -> let's do the same to render millions of tiny triangles
// todo -> this doesn't work by just using compute; we need something like dynamic LOD or meshlets, too

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
    val depthDst = Framebuffer("dd", w, h, 1, emptyList(), DepthBufferType.TEXTURE)
    val colorDst = Framebuffer("cd", w, h, TargetType.UInt8x4, DepthBufferType.NONE)
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
            val scale = panel.scale.y
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
    val newPos = FloatArrayList(pos.size)
    val newNor = FloatArrayList(nor.size)
    val newUVs = FloatArrayList(uv.size)
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

fun replaceTerms(name: String): String {
    return name
        .replace("gl_Position", "glPosition")
        .replace("gl_FragCoord", "glFragCoord")
        // they are signed in OpenGL because legacy OpenGL didn't have unsigned types
        .replace("gl_VertexID", "int(glVertexID)")
        .replace("gl_InstanceID", "int(glInstanceID)")
        .replace("gl_FrontFacing", "true") // not yet supported properly
        .replace("dFdx", "zero")
        .replace("dFdy", "zero")
        .replace("discard;", "return;")
}

fun extractMain(source: String): Pair<String, String> {
    val prefix = "void main(){"
    val idx = source.indexOf(prefix)
    val end = source.lastIndexOf('}')
    if (idx < 0 || end < idx) throw IllegalArgumentException("Missing main()")
    return replaceTerms(source.substring(0, idx)) to
            replaceTerms(source.substring(idx + prefix.length, end))
}

fun computeRasterizer() {

    // done first step: create an IMesh
    // done create lots of small triangles for testing

    val shaders =
        LazyMap<ShaderKey, Pair<ComputeShader, List<Variable>>> { (shader, target, meshAttr, instAttr, indexed, drawMode) ->
            val varyings = listOf(
                Variable(GLSLType.V4F, "glFragCoord"),
                Variable(GLSLType.V2I, "glFragCoordI"),
            ) + shader.varyings
            val attributes = shader.vertexVariables
                .filter { it.isAttribute }
            val instNames = instAttr.map { it.name }.toHashSet()
            val (vertexFunctions, vertexMain) = extractMain(shader.vertexShader)
            val (pixelFunctions, pixelMain) = extractMain(shader.fragmentShader)
            val uniformNames = HashSet<String>()
            val uniforms = (shader.vertexVariables +
                    shader.fragmentVariables)
                .filter { !it.isAttribute && it.isInput }
                .filter { uniformNames.add(it.name) }
                .sortedBy { it.size }
            val uniformTextures = uniforms
                .filter { it.type.isSampler }
            val outputs = (target?.storageLayers?.map { Variable(GLSLType.V4F, it.name) }
                ?: shader.fragmentVariables.filter { it.inOutMode == VariableMode.OUT })
            ComputeShader(
                "rasterizer", Vector3i(512, 1, 1), listOf(
                    Variable(GLSLType.V1I, "numPrimitives"),
                    Variable(GLSLType.V1I, "numInstances"),
                    Variable(GLSLType.V2I, "viewportSize"),
                ), "" +
                        createAccessors(
                            meshAttr, attributes
                                .filter { it.name !in instNames }
                                .map { Attribute(it.name, it.type.components) },
                            "Mesh", 0, false
                        ) +
                        createAccessors(instAttr, attributes
                            .filter { it.name in instNames }
                            .map { Attribute(it.name, it.type.components) },
                            "Inst", 1, false
                        ) +
                        "layout(r32f, binding = 0) coherent uniform image2D depthTex;\n" +
                        outputs.withIndex().joinToString("") { (idx, layer) ->
                            "layout(rgba32f, binding = ${idx + 1}) uniform image2D ${layer.name}Tex;\n"
                        } +
                        (if (indexed != null) {
                            "layout(std430, set = 0, binding = 2) buffer IndexBuffer {\n" +
                                    "    uint data[];\n" +
                                    "} Indices;\n"
                        } else "") +
                        "struct Pixel {\n" +
                        varyings.joinToString("") {
                            "  ${it.type.glslName} ${it.name};\n"
                        } +
                        "};\n" +
                        "float zero(float x){ return 0.0; }\n" +
                        "vec2 zero(vec2 x){ return vec2(0.0); }\n" +
                        "vec3 zero(vec3 x){ return vec3(0.0); }\n" +
                        "vec4 zero(vec4 x){ return vec4(0.0); }\n" +
                        uniforms.joinToString("") {
                            val tmp = StringBuilder()
                            it.declare(tmp, "uniform", false)
                            tmp.toString()
                        } +
                        vertexFunctions +
                        pixelFunctions +
                        // todo theoretically needs perspective-corrected lerping...
                        //  -> disable that when the triangle is far enough
                        "Pixel lerpPixels(Pixel a, Pixel b, float f, ivec2 glFragCoordI){\n" +
                        "   if(!(f > 0.0)) f = 0.0;\n" + // avoid NaN/Infinity
                        "   if(!(f < 1.0)) f = 1.0;\n" +
                        "   Pixel lerped;\n" +
                        "   lerped.glFragCoordI = glFragCoordI;\n" +
                        varyings
                            .filter { it.name != "glFragCoordI" }
                            .joinToString("") {
                                when (it.type) {
                                    GLSLType.V1I, GLSLType.V2I, GLSLType.V3I, GLSLType.V4I -> {
                                        val saveType = it.type
                                        val workType = when (saveType) {
                                            GLSLType.V1I -> GLSLType.V1F
                                            GLSLType.V2I -> GLSLType.V2F
                                            GLSLType.V3I -> GLSLType.V3F
                                            GLSLType.V4I -> GLSLType.V4F
                                            else -> throw NotImplementedError()
                                        }.glslName
                                        "lerped.${it.name} = ${saveType.glslName}(mix(" +
                                                "$workType(a.${it.name})," +
                                                "$workType(b.${it.name}),f));\n"
                                    }
                                    else -> {
                                        "lerped.${it.name} = mix(a.${it.name},b.${it.name},f);\n"
                                    }
                                }
                            } +
                        "   return lerped;\n" +
                        "}\n" +
                        "Pixel projectPixel(uint glVertexID, uint glInstanceID){\n" +
                        // load all attributes
                        attributes.joinToString("") {
                            val isInstanced = it.name in instNames
                            val srcBuffer = if (isInstanced) "Inst" else "Mesh"
                            val srcIndex = if (isInstanced) "glInstanceID" else "glVertexID"
                            "${it.type.glslName} ${it.name} = get$srcBuffer${it.name.titlecase()}($srcIndex);\n"
                        } +
                        // "   vec3 coord = coords;\n" +
                        // coord -> localPosition -> finalPosition -> gl_Position -> /w [-1,1] -> [0,w]
                        // "   coord = matMul(localTransform,vec4(coord,1.0));\n" +
                        varyings.joinToString("") { "${it.type.glslName} ${it.name} = ${it.type.glslName}(0);\n" } +
                        "   vec4 glPosition;\n" +
                        "{\n" +
                        // execute vertex shader
                        vertexMain +
                        "}\n" +
                        // "    = matMul(transform,vec4(coord,1.0));\n" +
                        "   glFragCoord = vec4(glPosition.xyz / glPosition.w, glPosition.w);\n" +
                        "   glFragCoord.xy = (glFragCoord.xy*.5+.5)*vec2(viewportSize);\n" +
                        "   glFragCoordI = ivec2(glFragCoord.xy);\n" +
                        "   return Pixel(${varyings.joinToString { it.name }});\n" +
                        "}\n" +
                        "void drawPixel(Pixel pixel){\n" +
                        "   ivec2 glFragCoordI = pixel.glFragCoordI;\n" +
                        "   float depth = pixel.glFragCoord.z;\n" +
                        "   if(glFragCoordI.x >= 0 && glFragCoordI.y >= 0 && glFragCoordI.x < viewportSize.x && glFragCoordI.y < viewportSize.y){\n" +
                        "       if(imageLoad(depthTex,glFragCoordI).x < depth){\n" +
                        // output variables
                        outputs.joinToString("") { layer ->
                            "vec4 ${layer.name} = vec4(0.0);\n"
                        } +
                        // load all varyings from Pixel
                        varyings
                            .filter { it.name != "glFragCoordI" }
                            .joinToString("") {
                                "${it.type.glslName} ${it.name} = pixel.${it.name};\n"
                            } +
                        // execute fragment shader
                        pixelMain +
                        // some time has passed, so check depth again ^^ (it's unsafe without atomics anyway...)
                        //"       if(imageLoad(depthTex,glFragCoordI).x < depth){\n" +
                        "           imageStore(depthTex, glFragCoordI, vec4(depth));\n" +
                        outputs.joinToString("") { layer ->
                            "       imageStore(${layer.name}Tex, glFragCoordI, ${layer.name});\n"
                        } +
                        "           }\n" +
                        //"       }\n" +
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
                        "vec3 findBarycentrics(ivec2 p0, ivec2 p1, ivec2 p2, ivec2 px){\n" +
                        "   ivec2 v0 = p1-p0, v1 = p2-p0, v2 = px-p0;\n" +
                        "   float d00=dot(v0,v0),d01=dot(v0,v1),d11=dot(v1,v1),d20=dot(v2,v0),d21=dot(v2,v1);\n" +
                        "   float div = d00*d11-d01*d01;\n" +
                        "   float d = 1.0/div;\n" +
                        "   float v = clamp(float(d11 * d20 - d01 * d21) * d,0.0,1.0);\n" +
                        "   float w = clamp(float(d00 * d21 - d01 * d20) * d,0.0,1.0);\n" +
                        "   float u = 1.0 - v - w;\n" +
                        "   return u+v+w < 1.0 ? vec3(u,v,w) : vec3(1.0,0.0,0.0);\n" +
                        "}\n" +
                        "Pixel lerpBarycentrics(Pixel a, Pixel b, Pixel c, ivec2 glFragCoordI){\n" +
                        "   vec3 bary = findBarycentrics(a.glFragCoordI,b.glFragCoordI,c.glFragCoordI,glFragCoordI);\n" +
                        "   Pixel ab = lerpPixels(a,b,bary.y/(bary.x+bary.y),glFragCoordI);\n" +
                        "   return lerpPixels(ab,c,bary.z,glFragCoordI);\n" +
                        "}\n" +
                        // todo change indexing for line strips and triangle strips (?)
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
                                "drawPixel(projectPixel(getIndex(primitiveId),instanceId));\n"
                            }
                            DrawMode.LINES, DrawMode.LINE_STRIP -> {
                                "" +
                                        "   Pixel ua = projectPixel(getIndex(primitiveId*2u),instanceId);\n" +
                                        "   Pixel ub = projectPixel(getIndex(primitiveId*2u+1u),instanceId);\n" +
                                        "   if(ua.glFragCoordI == ub.glFragCoordI) return;\n" + // line is in gaps
                                        // backside culling
                                        "   if(min(ua.glFragCoord.w,ub.glFragCoord.w) <= 0.0) return;\n" +
                                        "   int minX = max(min(ua.glFragCoordI.x,ub.glFragCoordI.x),0);\n" +
                                        "   int maxX = min(max(ua.glFragCoordI.x,ub.glFragCoordI.x),viewportSize.x-1);\n" +
                                        "   int minY = max(min(ua.glFragCoordI.y,ub.glFragCoordI.y),0);\n" +
                                        "   int maxY = min(max(ua.glFragCoordI.y,ub.glFragCoordI.y),viewportSize.y-1);\n" +
                                        "   float invSteps = 1.0 / float(max(maxX-minX,maxY-minY));\n" +
                                        "   if(maxY-minY > maxX-minX){\n" +
                                        "       for(int y=minY;y<=maxY;y++){\n" +
                                        "           float f = float(y-ua.glFragCoordI.y) * invSteps;\n" +
                                        "           int x = ua.glFragCoordI.x + int((ub.glFragCoordI.x-ua.glFragCoordI.x) * f);\n" +
                                        "           drawPixel(lerpPixels(ua,ub,f,ivec2(x,y)));\n" +
                                        "       }\n" +
                                        "   } else if(maxX > minX){\n" +
                                        "       for(int x=minX;x<=maxX;x++){\n" +
                                        "           float f = float(x-ua.glFragCoordI.x) * invSteps;\n" +
                                        "           int y = ua.glFragCoordI.y + int((ub.glFragCoordI.y-ua.glFragCoordI.y) * f);\n" +
                                        "           drawPixel(lerpPixels(ua,ub,f,ivec2(x,y)));\n" +
                                        "       }\n" +
                                        "   }\n"
                            }
                            DrawMode.TRIANGLES, DrawMode.TRIANGLE_STRIP -> {
                                "" +
                                        "   Pixel ua = projectPixel(getIndex(primitiveId*3u),instanceId);\n" +
                                        "   Pixel ub = projectPixel(getIndex(primitiveId*3u+1u),instanceId);\n" +
                                        "   Pixel uc = projectPixel(getIndex(primitiveId*3u+2u),instanceId);\n" +
                                        // backside culling
                                        "   if(min(ua.glFragCoord.w,min(ub.glFragCoord.w,uc.glFragCoord.w)) <= 0.0) return;\n" +
                                        // backface culling
                                        "   if(cross(vec3(ub.glFragCoordI-ua.glFragCoordI,0.0), vec3(uc.glFragCoordI-ua.glFragCoordI,0.0)).z <= 0.0) return;\n" +
                                        "   int minX = max(min(ua.glFragCoordI.x,min(ub.glFragCoordI.x,uc.glFragCoordI.x)),0);\n" +
                                        "   int maxX = min(max(ua.glFragCoordI.x,max(ub.glFragCoordI.x,uc.glFragCoordI.x)),viewportSize.x-1);\n" +
                                        "   int minY = max(min(ua.glFragCoordI.y,min(ub.glFragCoordI.y,uc.glFragCoordI.y)),0);\n" +
                                        "   int maxY = min(max(ua.glFragCoordI.y,max(ub.glFragCoordI.y,uc.glFragCoordI.y)),viewportSize.y-1);\n" +
                                        "   if((maxY-minY+1)*(maxX-minX+1) > 1000) return;\n" + // discard too large triangles
                                        "   if(minX==maxX || minY==maxY) return;\n" + // triangle between gaps, can be skipped
                                        "   if(maxX<minX+2 && maxY<minY+2 && minX>0 && minY>0 && maxX<viewportSize.x-1 && maxY<viewportSize.y-1){\n" +
                                        // small triangle on screen, max 3px -> no interpolations needed
                                        // todo even with nothing visible, we only get 30% more fps???
                                        "       drawPixel(ua);\n" +
                                        "       if(ub.glFragCoordI != ua.glFragCoordI){\n" +
                                        "           drawPixel(ub);\n" +
                                        "       }\n" +
                                        "       if(uc.glFragCoordI != ua.glFragCoordI && uc.glFragCoordI != ub.glFragCoordI){\n" +
                                        "           drawPixel(uc);\n" +
                                        "       }\n" +
                                        "   } else {\n" + // medium sized triangle
                                        "      for(int y=minY;y<=maxY;y++){\n" +
                                        // calculate minX and maxX on this line
                                        "           int minX1 = maxX, maxX1 = minX;\n" +
                                        "           union1(ua.glFragCoordI,ub.glFragCoordI,y,minX1,maxX1);\n" +
                                        "           union1(ub.glFragCoordI,uc.glFragCoordI,y,minX1,maxX1);\n" +
                                        "           union1(uc.glFragCoordI,ua.glFragCoordI,y,minX1,maxX1);\n" +
                                        "           minX1 = max(minX1,minX);\n" +
                                        "           maxX1 = min(maxX1,maxX);\n" +
                                        // find left and right pixel
                                        "           Pixel minBary = lerpBarycentrics(ua,ub,uc,ivec2(minX1,y));\n" +
                                        "           Pixel maxBary = lerpBarycentrics(ua,ub,uc,ivec2(maxX1,y));\n" +
                                        "           for(int x=minX1;x<=maxX1;x++){\n" +
                                        "               float f = float(x-minX)/float(maxX-minX+1);\n" +
                                        "               drawPixel(lerpPixels(minBary,maxBary,f,ivec2(x,y)));\n" +
                                        "           }\n" +
                                        "      }\n" +
                                        "   }\n"
                            }
                            else -> {}
                        } +
                        // todo rasterize big triangles as a group
                        //  assumption: there won't be many of them
                        "}\n"
            ).apply {
                setTextureIndices(uniformTextures.map { it.name })
            } to outputs
        }

    val mesh = IcosahedronModel.createIcosphere(7)
    if (false) mesh.createUniqueIndices()

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

        override fun draw(pipeline: Pipeline?, shader: Shader, materialIndex: Int, drawLines: Boolean) {
            if (Input.isShiftDown) {
                mesh.draw(pipeline, shader, materialIndex, drawLines)
            } else {
                drawInstanced0(shader, materialIndex, null, drawLines)
            }
        }

        override fun drawInstanced(
            pipeline: Pipeline, shader: Shader, materialIndex: Int,
            instanceData: Buffer, drawLines: Boolean
        ) {
            if (Input.isShiftDown) {
                mesh.drawInstanced(pipeline, shader, materialIndex, instanceData, drawLines)
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
            if (drawLines) mesh.ensureDebugLines()
            val triBuffer = if (drawLines) mesh.debugLineBuffer else mesh.triBuffer
            val target = GFXState.currentBuffer

            // copy depth to color
            val depthAsColor = getDepthTarget(target)

            val deferredSettings = GFXState.currentRenderer.deferredSettings
            val key = ShaderKey(
                shader, deferredSettings, mesh.buffer!!.attributes,
                instanceData?.attributes ?: emptyList(),
                triBuffer?.elementsType,
                if (drawLines) DrawMode.LINES else mesh.drawMode
            )

            val (rasterizer, outputs) = shaders[key]
            rasterizer.use()

            val numPrimitives = if (drawLines) mesh.debugLineBuffer!!.elementCount / 2
            else mesh.numPrimitives.toInt()

            bindBuffers(rasterizer, instanceData, triBuffer)
            bindUniforms(rasterizer, materialIndex, instanceData, target, numPrimitives)
            bindTargets(rasterizer, depthAsColor, outputs, target)

            rasterizer.runBySize(numPrimitives * (instanceData?.drawLength ?: 1))

            writeDepth(target, depthAsColor)
        }

        private fun getDepthTarget(target: IFramebuffer): Texture2D {
            val depthAsColor = FBStack[
                "depthAsColor", target.width, target.height,
                listOf(TargetType.Float32x1), 1, DepthBufferType.NONE
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
            shader: ComputeShader, materialIndex: Int,
            instanceData: Buffer?, target: IFramebuffer,
            numPrimitives: Int,
        ) {
            val pipeline = RenderView.currentInstance!!.pipeline // meh; todo is there a better way to get the pipeline?
            val material = Pipeline.getMaterial(null, mesh.materials, materialIndex)
            material.bind(shader)
            initShader(shader, false)
            bindRandomness(shader)
            setupLocalTransform(shader, null, 0L)
            setupLights(pipeline, shader, AABBd(), true)
            shader.v1i("numPrimitives", numPrimitives)
            shader.v1i("numInstances", instanceData?.drawLength ?: 1)
            shader.v2i("viewportSize", target.width, target.height)
        }

        private fun bindTargets(
            rasterizer: ComputeShader, depthAsColor: Texture2D,
            outputs: List<Variable>, target: IFramebuffer
        ) {
            rasterizer.bindTexture(0, depthAsColor, ComputeTextureMode.READ_WRITE)
            for (i in outputs.indices) {
                rasterizer.bindTexture(i + 1, target.getTextureI(i) as Texture2D, ComputeTextureMode.WRITE)
            }
        }

        override fun fill(pipeline: Pipeline, entity: Entity, clickId: Int): Int {
            val material = Material.defaultMaterial
            pipeline.findStage(material)
                .add(component, this, entity, material, 0)
            return clickId + 1
        }
    }

    val scene = Entity("Scene")
    val s = 5
    for (z in -s..s) {
        for (x in -s..s) {
            val comp = object : MeshComponentBase() {
                override fun getMeshOrNull() = iMesh
            }
            component = comp // any instance is fine
            scene.add(
                Entity("Compute/$x//$z")
                    .setPosition(x * 2.0, 0.0, z * 2.0)
                    .add(comp)
            )
        }
    }

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
    testSceneWithUI("Compute Rasterizer", scene) {
        EngineBase.enableVSync = false // we want to go fast, so we need to measure performance
    }
}