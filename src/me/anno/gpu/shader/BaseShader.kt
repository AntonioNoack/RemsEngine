package me.anno.gpu.shader

import me.anno.gpu.GFX
import me.anno.gpu.RenderState
import me.anno.gpu.deferred.DeferredLayerType
import me.anno.gpu.deferred.DeferredSettingsV2
import me.anno.gpu.shader.builder.ShaderStage
import me.anno.gpu.shader.builder.Variable
import me.anno.utils.structures.maps.KeyTripleMap
import me.anno.utils.types.Booleans.toInt
import me.anno.utils.types.Strings.isBlank2
import java.lang.RuntimeException
import kotlin.math.max

/**
 * converts a shader with color, normal, tint and such into
 *  a) a clickable / depth-able shader
 *  b) a flat color shader
 *  c) a deferred shader
 * */
open class BaseShader(
    val name: String,
    val vertexSource: String,
    val varyingSource: List<Variable>,
    val fragmentSource: String
) {

    constructor() : this("", "", emptyList(), "")

    var glslVersion = Shader.DefaultGLSLVersion
    var textures: List<String>? = null
    var ignoredUniforms = HashSet<String>()

    private val flatShader = KeyTripleMap<Renderer, Boolean, GeoShader?, Shader>()
    private val deferredShaders = KeyTripleMap<DeferredSettingsV2, Boolean, GeoShader?, Shader>()
    private val depthShader = lazy { Array(2) { createDepthShader(it > 0) } }

    open fun createDepthShader(instanced: Boolean): Shader {
        println("creating depth shader $name from '$vertexSource'")
        if(vertexSource.isBlank2()) throw RuntimeException()
        val vertex = if (instanced) "#define INSTANCED\n$vertexSource" else vertexSource
        return Shader(name, null, vertex, emptyList(), "void main(){}")
    }

    open fun createFlatShader(postProcessing: ShaderStage?, instanced: Boolean, geoShader: GeoShader?): Shader {

        val varying = varyingSource
        val vertex = if (instanced) "#define INSTANCED\n$vertexSource" else vertexSource

        val postProcessing = postProcessing?.functions?.firstOrNull { it.name == "main" }?.body ?: ""

        // if it does not have tint, then add it?
        // what do we do if it writes glFragColor?
        // option to use flat shading independent of rendering mode (?)
        val fragment = StringBuilder()
        if (instanced) fragment.append("#define INSTANCED\n")
        val postMainIndex = postProcessing.indexOf("void main")
        if (postMainIndex > 0) {
            // add the code before main
            fragment.append(postProcessing.substring(0, postMainIndex))
        }
        if ("gl_FragColor" !in fragmentSource) {
            fragment.append(fragmentSource.substring(0, fragmentSource.lastIndexOf('}')))
            // finalColor, finalAlpha were properly defined
        } else {
            fragment.append(fragmentSource.substring(0, fragmentSource.lastIndexOf('}')))
            // finalColor, finalAlpha are missing
            fragment.append(
                "" +
                        "vec3 finalColor = gl_FragColor.rgb;\n" +
                        "float finalAlpha = gl_FragColor.a;\n"
            )
        }
        if (postMainIndex >= 0 || !postProcessing.isBlank2()) {
            val pmi2 = if (postMainIndex < 0) -1 else postProcessing.indexOf('{', postMainIndex + 9)
            // define all variables with prefix "final", which are missing
            for (type in DeferredLayerType.values()) {
                if (type.glslName in postProcessing && type.glslName !in fragment) {
                    type.appendDefinition(fragment)
                    fragment.append(" = ")
                    type.appendDefaultValue(fragment)
                    fragment.append(";\n")
                }
            }
            fragment.append(postProcessing.substring(pmi2 + 1))
        } else {
            fragment.append("gl_FragColor = vec4(finalColor, finalAlpha);\n")
        }
        if (postMainIndex < 0) {
            fragment.append('}')
        }
        GFX.check()
        val shader = ShaderPlus.create(name, geoShader?.code, vertex, varying, fragment.toString())
        shader.glslVersion = glslVersion
        shader.setTextureIndices(textures)
        shader.ignoreUniformWarnings(ignoredUniforms)
        shader.v1("drawMode", ShaderPlus.DrawMode.COLOR.id)
        shader.v4("tint", 1f, 1f, 1f, 1f)
        GFX.check()
        return shader
    }

    val value: Shader
        get() {
            val renderer = RenderState.currentRenderer
            val instanced = RenderState.instanced.currentValue
            return if (renderer == Renderer.depthOnlyRenderer) {
                depthShader.value[instanced.toInt()]
            } else when (val deferred = renderer.deferredSettings) {
                null -> {
                    val geoMode = RenderState.geometryShader.currentValue
                    val shader = flatShader.getOrPut(renderer, instanced, geoMode) { r, i, g ->
                        val shader = createFlatShader(r.getPostProcessing(), i, g)
                        r.uploadDefaultUniforms(shader)
                        // LOGGER.info(shader.fragmentSource)
                        shader
                    }
                    shader.use()
                    shader.v1("drawMode", renderer.drawMode.id)
                    shader
                }
                else -> get(deferred, instanced, RenderState.geometryShader.currentValue)
            }
        }

    fun ignoreUniformWarnings(warnings: Collection<String>) {
        ignoredUniforms += warnings
    }

    fun setTextureIndices(textures: List<String>?) {
        this.textures = textures
    }

    operator fun get(settings: DeferredSettingsV2, instanced: Boolean, geoShader: GeoShader?): Shader {
        return deferredShaders.getOrPut(settings, instanced, geoShader) { s, i, g ->
            val shader = s.createShader(
                name,
                g?.code, i,
                vertexSource,
                varyingSource,
                fragmentSource,
                textures
            )
            shader.glslVersion = max(shader.glslVersion, glslVersion)
            shader.use()
            shader.setTextureIndices(textures)
            shader.ignoreUniformWarnings(ignoredUniforms)
            shader.v1("drawMode", ShaderPlus.DrawMode.COLOR.id)
            shader.v4("tint", 1f, 1f, 1f, 1f)
            shader
        }
    }

    fun destroy() {
        for (list in flatShader) {
            for ((_, _, shader) in list) {
                shader.destroy()
            }
        }
        for (list in deferredShaders) {
            for ((_, _, shader) in list) {
                shader.destroy()
            }
        }
    }

    companion object {

        // is used to draw indexed geometry optionally as lines (for debugging purposes)
        val lineGeometry = GeoShader(
            "layout(triangles) in;\n" +
                    "layout(line_strip, max_vertices = 6) out;\n" +
                    "#inOutVarying\n" +
                    "void main(){\n" +
                    "   #copy[0]\n" +
                    "   gl_Position = gl_in[0].gl_Position;\n" +
                    "   EmitVertex();\n" + // a-b
                    "   #copy[1]\n" +
                    "   gl_Position = gl_in[1].gl_Position;\n" +
                    "   EmitVertex();\n" + // b-c
                    "   #copy[2]\n" +
                    "   gl_Position = gl_in[2].gl_Position;\n" +
                    "   EmitVertex();\n" + // c-a
                    "   #copy[0]\n" +
                    "   gl_Position = gl_in[0].gl_Position;\n" +
                    "   EmitVertex();\n" +
                    "   EndPrimitive();\n" +
                    "}"
        )

        // does not work for scaled things, somehow...
        /*val normalGeometry0 = GeoShader(
            "layout(triangles) in;\n" +
                    "layout(triangle_strip, max_vertices = 12) out;\n" +
                    "\n" +
                    "#inOutVarying\n" +
                    "\n" +
                    // v_normal[i]
                    // x*w,y*w,z*w,w
                    "uniform mat4 transform;\n" + // the camera + projection transform
                    "void main(){\n" +
                    // the normal lines for visualization
                    "   for(int i=0;i<3;i++){\n" +
                    "       #copy[i]\n" +
                    "       gl_Position = gl_in[i].gl_Position;\n" +
                    "       vec4 normal = transform * vec4(v_normal[i]*gl_Position.w*0.05, 0.0);\n" +
                    "       vec4 smallOffset = vec4(normalize(vec2(normal.y,-normal.x))*0.004,0.0,0.0)*gl_in[i].gl_Position.w;\n" +
                    "       gl_Position = gl_in[i].gl_Position - smallOffset;\n" +
                    "       EmitVertex();\n" +
                    "       gl_Position = gl_in[i].gl_Position + smallOffset;\n" +
                    "       EmitVertex();\n" +
                    "       gl_Position = gl_in[i].gl_Position + normal;\n" +
                    "       EmitVertex();\n" +
                    "       EndPrimitive();\n" +
                    "   }\n" +
                    // the original triangle
                    "   for(int i=0;i<3;i++){\n" +
                    "       #copy[i]\n" +
                    "       gl_Position = gl_in[i].gl_Position;\n" +
                    "       EmitVertex();\n" +
                    "   }\n" +
                    "   EndPrimitive();\n" +
                    "}\n"
        )*/

        val cullFaceColoringGeometry = GeoShader(
            "layout(triangles) in;\n" +
                    "layout(triangle_strip, max_vertices = 3) out;\n" +
                    "#inOutVarying\n" +
                    "void main(){\n" +
                    // check if front facing or back facing, and change the color
                    "   vec3 a = gl_in[0].gl_Position.xyz / gl_in[0].gl_Position.w;\n" +
                    "   vec3 b = gl_in[1].gl_Position.xyz / gl_in[1].gl_Position.w;\n" +
                    "   vec3 c = gl_in[2].gl_Position.xyz / gl_in[2].gl_Position.w;\n" +
                    "   vec4 color = vec4(cross(b-a,c-a).z < 0.0 ? vec3(1,0,0) : vec3(0,0,1), 1.0);\n" +
                    "   for(int i=0;i<3;i++){\n" +
                    "       #copy[i]\n" +
                    "       f_vertexColor = color;\n" +
                    "       gl_Position = gl_in[i].gl_Position;\n" +
                    "       EmitVertex();\n" +
                    "   }\n" +
                    "   EndPrimitive();\n" +
                    "}\n"
        )

    }

}