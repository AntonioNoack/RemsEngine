package me.anno.tests.gfx.nanite

import me.anno.gpu.buffer.Attribute
import me.anno.gpu.buffer.AttributeType
import me.anno.gpu.buffer.DrawMode
import me.anno.gpu.deferred.DeferredSettings
import me.anno.gpu.shader.BufferCompute.createAccessors
import me.anno.gpu.shader.ComputeShader
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.utils.structures.maps.LazyMap
import me.anno.utils.types.Strings.titlecase
import org.joml.Vector3i

object ComputeShaders {

    private fun getAttributes(shader: Shader): List<Variable> {
        return shader.vertexVariables
            .filter { it.isAttribute }
    }

    private fun getUniforms(shader: Shader): List<Variable> {
        return (shader.vertexVariables +
                shader.fragmentVariables)
            .filter { !it.isAttribute && it.isInput }
            .distinctBy { it.name }
            .sortedBy { it.size }
    }

    private fun getVaryings(shader: Shader): List<Variable> {
        return listOf(
            Variable(GLSLType.V4F, "glFragCoord"),
            Variable(GLSLType.V2I, "glFragCoordI"),
        ) + shader.varyings
    }

    private fun getOutputs(shader: Shader, target: DeferredSettings?): List<Variable> {
        return (target?.storageLayers?.map { Variable(GLSLType.V4F, it.name) }
            ?: shader.fragmentVariables.filter { it.inOutMode == VariableMode.OUT })
    }

    private fun createMeshAccessors(
        meshAttr: List<Attribute>,
        attributes: List<Variable>,
        instNames: Set<String>
    ): String {
        return createAccessors(
            meshAttr, attributes
                .filter { it.name !in instNames }
                .map { Attribute(it.name, it.type.components) },
            "Mesh", 0, false
        )
    }

    private fun createInstanceAccessors(
        instAttr: List<Attribute>,
        attributes: List<Variable>,
        instNames: Set<String>
    ): String {
        return createAccessors(instAttr, attributes
            .filter { it.name in instNames }
            .map { Attribute(it.name, it.type.components) },
            "Inst", 1, false
        )
    }

    private fun isIntVariable(type: GLSLType): Boolean {
        return when (type) {
            GLSLType.V1I, GLSLType.V2I, GLSLType.V3I, GLSLType.V4I -> true
            else -> false
        }
    }

    private fun union1(): String {
        return "void union1(ivec2 a, ivec2 b, int y, inout int minX, inout int maxX){\n" +
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
                "}\n"
    }

    private fun findBarycentrics(): String {
        return "vec3 findBarycentrics(ivec2 p0, ivec2 p1, ivec2 p2, ivec2 px){\n" +
                "   ivec2 v0 = p1-p0, v1 = p2-p0, v2 = px-p0;\n" +
                "   float d00=dot(v0,v0),d01=dot(v0,v1),d11=dot(v1,v1),d20=dot(v2,v0),d21=dot(v2,v1);\n" +
                "   float div = d00*d11-d01*d01;\n" +
                "   float d = 1.0/div;\n" +
                "   float v = clamp(float(d11 * d20 - d01 * d21) * d,0.0,1.0);\n" +
                "   float w = clamp(float(d00 * d21 - d01 * d20) * d,0.0,1.0);\n" +
                "   float u = 1.0 - v - w;\n" +
                "   return u+v+w < 1.0 ? vec3(u,v,w) : vec3(1.0,0.0,0.0);\n" +
                "}\n"
    }

    private fun lerpBarycentrics(): String {
        return "Pixel lerpBarycentrics(Pixel a, Pixel b, Pixel c, ivec2 glFragCoordI){\n" +
                "   vec3 bary = findBarycentrics(a.glFragCoordI,b.glFragCoordI,c.glFragCoordI,glFragCoordI);\n" +
                "   Pixel ab = mulAddPixels(a,bary.x,b,bary.y,glFragCoordI);\n" +
                "   return mulAddPixels(ab,1.0,c,bary.z,glFragCoordI);\n" +
                // "   Pixel ab = lerpPixels(a,b,bary.y/(bary.x+bary.y),glFragCoordI);\n" +
                // "   return lerpPixels(ab,c,bary.z,glFragCoordI);\n" +
                "}\n"
    }

    private val interpolateIntVariables = false

    private fun lerpPixels(varyings: List<Variable>): String {
        // todo theoretically needs perspective-corrected lerping...
        //  -> disable that when the triangle is far enough
        val lerpedVariables = varyings
            .filter { it.name != "glFragCoordI" }
        return "Pixel lerpPixels(Pixel a, Pixel b, float f, ivec2 glFragCoordI){\n" +
                "   if(!(f > 0.0)) f = 0.0;\n" + // avoid NaN/Infinity
                "   if(!(f < 1.0)) f = 1.0;\n" +
                "   Pixel lerped;\n" +
                "   lerped.glFragCoordI = glFragCoordI;\n" +
                lerpedVariables.joinToString("") {
                    if (isIntVariable(it.type)) {
                        if (interpolateIntVariables) {
                            val ioType = it.type
                            val mixType = when (ioType) {
                                GLSLType.V1I -> GLSLType.V1F
                                GLSLType.V2I -> GLSLType.V2F
                                GLSLType.V3I -> GLSLType.V3F
                                GLSLType.V4I -> GLSLType.V4F
                                else -> throw NotImplementedError()
                            }.glslName
                            "lerped.${it.name} = ${ioType.glslName}(mix(" +
                                    "$mixType(a.${it.name})," +
                                    "$mixType(b.${it.name}),f));\n"
                        } else {
                            "lerped.${it.name} = a.${it.name};\n"
                        }
                    } else {
                        "lerped.${it.name} = mix(a.${it.name},b.${it.name},f);\n"
                    }
                } +
                "   return lerped;\n" +
                "}\n"
    }

    private fun mulAddPixels(varyings: List<Variable>): String {
        // todo theoretically needs perspective-corrected lerping...
        //  -> disable that when the triangle is far enough
        val addedVariables = varyings
            .filter { it.name != "glFragCoordI" }
        return "Pixel mulAddPixels(Pixel a, float fa, Pixel b, float fb, ivec2 glFragCoordI){\n" +
                "   if(!(fa > 0.0)) fa = 0.0;\n" + // avoid NaN/Infinity
                "   if(!(fa < 1.0)) fa = 1.0;\n" +
                "   if(!(fb > 0.0)) fb = 0.0;\n" +
                "   if(!(fb < 1.0)) fb = 1.0;\n" +
                "   Pixel added;\n" +
                "   added.glFragCoordI = glFragCoordI;\n" +
                addedVariables.joinToString("") {
                    if (isIntVariable(it.type)) {
                        if (interpolateIntVariables) {
                            val ioType = it.type
                            val mixType = when (ioType) {
                                GLSLType.V1I -> GLSLType.V1F
                                GLSLType.V2I -> GLSLType.V2F
                                GLSLType.V3I -> GLSLType.V3F
                                GLSLType.V4I -> GLSLType.V4F
                                else -> throw NotImplementedError()
                            }.glslName
                            "added.${it.name} = ${ioType.glslName}(" +
                                    "$mixType(a.${it.name})*fa + " +
                                    "$mixType(b.${it.name})*fb);\n"
                        } else {
                            "added.${it.name} = a.${it.name};\n"
                        }
                    } else {
                        "added.${it.name} = a.${it.name}*fa + b.${it.name}*fb;\n"
                    }
                } +
                "   return added;\n" +
                "}\n"
    }

    private fun projectPixel(
        attributes: List<Variable>,
        instNames: Set<String>,
        varyings: List<Variable>,
        vertexMain: String
    ): String {
        return "Pixel projectPixel(uint glVertexID, uint glInstanceID){\n" +
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
                "}\n"
    }

    private fun rasterizePixel(): String {
        return "drawPixel(projectPixel(getIndex(primitiveId),instanceId));\n"
    }

    private fun rasterizeLine(): String {
        return "" +
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

    private fun rasterizeTriangle(): String {
        return "" +
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

    private fun outputVariables(outputs: List<Variable>): String {
        return outputs.joinToString("") { layer ->
            "vec4 ${layer.name} = vec4(0.0);\n"
        }
    }

    private fun loadVaryingsFromPixel(varyings: List<Variable>): String {
        return varyings
            .filter { it.name != "glFragCoordI" }
            .joinToString("") {
                "${it.type.glslName} ${it.name} = pixel.${it.name};\n"
            }
    }

    val shaders = LazyMap<ComputeShaderKey, Pair<ComputeShader, List<Variable>>> { key ->
        val (shader, target, meshAttr, instAttr, indexed, drawMode) = key
        val varyings = getVaryings(shader)
        val attributes = getAttributes(shader)
        val instNames = instAttr.map { it.name }.toSet()
        val (vertexFunctions, vertexMain) = extractMain(shader.vertexShader)
        val (pixelFunctions, pixelMain) = extractMain(shader.fragmentShader)
        val uniforms = getUniforms(shader)
        val outputs = getOutputs(shader, target)
        ComputeShader(
            "rasterizer", Vector3i(512, 1, 1), listOf(
                Variable(GLSLType.V1I, "numPrimitives"),
                Variable(GLSLType.V1I, "numInstances"),
                Variable(GLSLType.V2I, "viewportSize"),
            ), "" +
                    createMeshAccessors(meshAttr, attributes, instNames) +
                    createInstanceAccessors(instAttr, attributes, instNames) +
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
                    lerpPixels(varyings) +
                    mulAddPixels(varyings) +
                    projectPixel(attributes, instNames, varyings, vertexMain) +
                    "void drawPixel(Pixel pixel){\n" +
                    "   ivec2 glFragCoordI = pixel.glFragCoordI;\n" +
                    "   float depth = pixel.glFragCoord.z;\n" +
                    "   if(glFragCoordI.x >= 0 && glFragCoordI.y >= 0 && glFragCoordI.x < viewportSize.x && glFragCoordI.y < viewportSize.y){\n" +
                    "       if(imageLoad(depthTex,glFragCoordI).x < depth){\n" +
                    outputVariables(outputs) +
                    loadVaryingsFromPixel(varyings) +
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
                    union1() +
                    findBarycentrics() +
                    lerpBarycentrics() +
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
                        DrawMode.POINTS -> rasterizePixel()
                        DrawMode.LINES, DrawMode.LINE_STRIP -> rasterizeLine()
                        DrawMode.TRIANGLES, DrawMode.TRIANGLE_STRIP -> rasterizeTriangle()
                        else -> throw NotImplementedError()
                    } +
                    // todo rasterize big triangles as a group
                    //  assumption: there won't be many of them
                    "}\n"
        ).apply {
            val uniformTextures = uniforms.filter { it.type.isSampler }
            setTextureIndices(uniformTextures.map { it.name })
        } to outputs
    }
}