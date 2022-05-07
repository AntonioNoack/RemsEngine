package me.anno.gpu.shader

import me.anno.gpu.GFX
import me.anno.gpu.OpenGL
import me.anno.gpu.deferred.DeferredLayerType
import me.anno.gpu.deferred.DeferredSettingsV2
import me.anno.gpu.shader.builder.ShaderStage
import me.anno.gpu.shader.builder.Variable
import me.anno.utils.structures.maps.KeyTripleMap
import me.anno.utils.types.Booleans.toInt
import me.anno.utils.types.Strings.isBlank2

/**
 * converts a shader with color, normal, tint and such into
 *  a) a clickable / depth-able shader
 *  b) a flat color shader
 *  c) a deferred shader
 * */
open class BaseShader(
    val name: String,
    val vertexVariables: List<Variable>,
    val vertexShader: String,
    val varyings: List<Variable>,
    val fragmentVariables: List<Variable>,
    val fragmentShader: String
) { // Saveable or PrefabSaveable?

    constructor(name: String, vertexSource: String, varyingSource: List<Variable>, fragmentSource: String) : this(
        name,
        emptyList(),
        vertexSource,
        varyingSource,
        emptyList(),
        fragmentSource
    )

    constructor() : this("", "", emptyList(), "")

    var glslVersion = OpenGLShader.DefaultGLSLVersion
    var textures: List<String>? = null
    var ignoredUniforms = HashSet<String>()

    private val flatShader = KeyTripleMap<Renderer, Int, GeoShader?, Shader>()
    private val deferredShaders = KeyTripleMap<DeferredSettingsV2, Int, GeoShader?, Shader>()
    private val depthShader = Array(2) { lazy { createDepthShader(it.and(1) != 0, it.and(2) != 0) } }

    /** shader for rendering the depth, e.g. for pre-depth */
    open fun createDepthShader(isInstanced: Boolean, isAnimated: Boolean): Shader {
        if (vertexShader.isBlank2()) throw RuntimeException()
        var vertexShader = vertexShader
        if (isInstanced) vertexShader = "#define INSTANCED\n$vertexShader"
        if (isAnimated) vertexShader = "#define ANIMATED\n$vertexShader"
        return Shader(name, vertexVariables, vertexShader, varyings, fragmentVariables, "void main(){}")
    }

    /** shader for forward rendering */
    open fun createForwardShader(
        postProcessing: ShaderStage?, isInstanced: Boolean, isAnimated: Boolean, geoShader: GeoShader?
    ): Shader {

        val varying = varyings
        var vertexShader = vertexShader
        if (isInstanced) vertexShader = "#define INSTANCED\n$vertexShader"
        if (isAnimated) vertexShader = "#define ANIMATED\n$vertexShader"
        val vertex = vertexShader

        val postProcessing1 = postProcessing?.functions?.firstOrNull { it.name == "main" }?.body ?: ""

        // if it does not have tint, then add it?
        // what do we do if it writes glFragColor?
        // option to use flat shading independent of rendering mode (?)
        val fragment = StringBuilder()
        if (isInstanced) fragment.append("#define INSTANCED\n")
        val postMainIndex = postProcessing1.indexOf("void main")
        var fragmentShader = fragmentShader
        val hasLegacyFragColor = "gl_FragColor" in fragmentShader
        if (hasLegacyFragColor) {
            fragment.append("out vec4 glFragColor;\n")
            fragmentShader = fragmentShader.replace("gl_FragColor", "glFragColor")
        }
        if (postMainIndex > 0) {
            // add the code before main
            fragment.append(postProcessing1.substring(0, postMainIndex))
        }
        if (hasLegacyFragColor) {
            fragment.append(fragmentShader.substring(0, fragmentShader.lastIndexOf('}')))
            // finalColor, finalAlpha are missing
            fragment.append(
                "" + "vec3 finalColor = glFragColor.rgb;\n" + "float finalAlpha = glFragColor.a;\n"
            )
        } else {
            fragment.append(fragmentShader.substring(0, fragmentShader.lastIndexOf('}')))
            // finalColor, finalAlpha were properly defined
        }
        if (postMainIndex >= 0 || !postProcessing1.isBlank2()) {
            val pmi2 = if (postMainIndex < 0) -1 else postProcessing1.indexOf('{', postMainIndex + 9)
            // define all variables with prefix "final", which are missing
            for (type in DeferredLayerType.values) {
                if (type.glslName in postProcessing1 && type.glslName !in fragment) {
                    type.appendDefinition(fragment)
                    fragment.append(" = ")
                    type.appendDefaultValue(fragment)
                    fragment.append(";\n")
                }
            }
            fragment.append(postProcessing1.substring(pmi2 + 1))
        } else {
            if (!fragment.contains("out vec4 glFragColor"))
                fragment.insert(0, "out vec4 glFragColor;\n")
            fragment.append("glFragColor = vec4(finalColor, finalAlpha);\n")
        }
        if (postMainIndex < 0) {
            fragment.append('}')
        }
        GFX.check()
        val shader = ShaderPlus.create(
            name, geoShader?.code, vertexVariables, vertex, varying, fragmentVariables, fragment.toString()
        )
        shader.glslVersion = glslVersion
        shader.setTextureIndices(textures)
        shader.ignoreNameWarnings(ignoredUniforms)
        shader.v1i("drawMode", ShaderPlus.DrawMode.COLOR.id)
        shader.v4f("tint", 1f, 1f, 1f, 1f)
        GFX.check()
        return shader
    }

    val value: Shader
        get() {
            GFX.check()
            val renderer = OpenGL.currentRenderer
            val instanced = OpenGL.instanced.currentValue
            val animated = OpenGL.animated.currentValue
            val stateId = instanced.toInt() + animated.toInt(2)
            val shader = if (renderer == Renderer.depthRenderer) {
                depthShader[stateId].value
            } else when (val deferred = renderer.deferredSettings) {
                null -> {
                    val geoMode = OpenGL.geometryShader.currentValue
                    flatShader.getOrPut(renderer, stateId, geoMode) { r, i, g ->
                        val shader = createForwardShader(r.getPostProcessing(), i.and(1) != 0, i.and(2) != 0, g)
                        r.uploadDefaultUniforms(shader)
                        // LOGGER.info(shader.fragmentSource)
                        shader
                    }
                }
                else -> get(deferred, stateId, OpenGL.geometryShader.currentValue)
            }
            GFX.check()
            shader.use()
            bind(shader, renderer, instanced)
            GFX.check()
            return shader
        }

    open fun bind(shader: Shader, renderer: Renderer, instanced: Boolean) {
        shader.v1i("drawMode", renderer.drawMode.id)
    }

    fun ignoreUniformWarnings(names: Collection<String>) {
        ignoredUniforms += names
    }

    fun ignoreUniformWarnings(vararg names: String) {
        ignoredUniforms += names
    }

    fun ignoreUniformWarning(name: String) {
        ignoredUniforms += name
    }

    fun setTextureIndices(textures: List<String>?) {
        this.textures = textures
    }

    /** shader for deferred rendering */
    open fun createDeferredShader(
        deferred: DeferredSettingsV2, isInstanced: Boolean, isAnimated: Boolean, geoShader: GeoShader?
    ): Shader {
        val shader = deferred.createShader(
            name,
            geoShader?.code,
            isInstanced,
            vertexVariables,
            vertexShader,
            varyings,
            fragmentVariables,
            fragmentShader,
            textures
        )
        finish(shader)
        return shader
    }

    fun finish(shader: Shader) {
        shader.glslVersion = glslVersion
        shader.use()
        shader.setTextureIndices(textures)
        shader.ignoreNameWarnings(ignoredUniforms)
        shader.v1i("drawMode", ShaderPlus.DrawMode.COLOR.id)
        shader.v4f("tint", 1f, 1f, 1f, 1f)
        GFX.check()
    }

    operator fun get(settings: DeferredSettingsV2, stateId: Int, geoShader: GeoShader?): Shader {
        return deferredShaders.getOrPut(settings, stateId, geoShader) { settings2, stateId2, geoShader2 ->
            createDeferredShader(settings2, stateId2.and(1) != 0, stateId2.and(2) != 0, geoShader2)
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
            "layout(triangles) in;\n" + "layout(line_strip, max_vertices = 6) out;\n" + "#inOutVarying\n" + "void main(){\n" + "   #copy[0]\n" + "   gl_Position = gl_in[0].gl_Position;\n" + "   EmitVertex();\n" + // a-b
                    "   #copy[1]\n" + "   gl_Position = gl_in[1].gl_Position;\n" + "   EmitVertex();\n" + // b-c
                    "   #copy[2]\n" + "   gl_Position = gl_in[2].gl_Position;\n" + "   EmitVertex();\n" + // c-a
                    "   #copy[0]\n" + "   gl_Position = gl_in[0].gl_Position;\n" + "   EmitVertex();\n" + "   EndPrimitive();\n" + "}"
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
            "layout(triangles) in;\n" + "layout(triangle_strip, max_vertices = 3) out;\n" + "#inOutVarying\n" + "void main(){\n" +
                    // check if front facing or back facing, and change the color
                    "   vec3 a = gl_in[0].gl_Position.xyz / gl_in[0].gl_Position.w;\n" + "   vec3 b = gl_in[1].gl_Position.xyz / gl_in[1].gl_Position.w;\n" + "   vec3 c = gl_in[2].gl_Position.xyz / gl_in[2].gl_Position.w;\n" + "   vec4 color = vec4(cross(b-a,c-a).z < 0.0 ? vec3(1.0, 0.0, 0.0) : vec3(0.0, 0.0, 1.0), 1.0);\n" + "   for(int i=0;i<3;i++){\n" + "       #copy[i]\n" + "       f_vertexColor = color;\n" + "       gl_Position = gl_in[i].gl_Position;\n" + "       EmitVertex();\n" + "   }\n" + "   EndPrimitive();\n" + "}\n"
        )

    }

}