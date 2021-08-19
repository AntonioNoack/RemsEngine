package me.anno.gpu.shader.builder

import me.anno.gpu.deferred.DeferredLayerType
import me.anno.gpu.deferred.DeferredSettingsV2
import me.anno.gpu.hidden.HiddenOpenGLContext
import me.anno.gpu.shader.GeoShader
import me.anno.gpu.shader.Shader
import kotlin.math.max

class ShaderBuilder(val name: String) {

    constructor(name: String, settingsV2: DeferredSettingsV2?) : this(name) {
        outputs = settingsV2
    }

    // there exist 3 passes: vertex, fragment, geometry
    // input variables can be defined on multiple ways: uniforms / attributes
    // output colors can be further and further processed

    val vertex = MainStage().apply {
        define(Variable("vec4", "gl_Position", true))
    }

    val fragment = MainStage()

    val geometry: GeoShader? = null

    var outputs: DeferredSettingsV2? = null

    var glslVersion = Shader.DefaultGLSLVersion

    fun addVertex(stage: ShaderStage?) {
        vertex.add(stage ?: return)
    }

    fun addFragment(stage: ShaderStage?) {
        fragment.add(stage ?: return)
    }

    fun create(): Shader {
        // combine the code
        // find imports
        val vi = vertex.findImportsAndDefineValues(null, null)
        fragment.findImportsAndDefineValues(vertex, vi)
        // create the code
        val vertCode = vertex.createCode(false, outputs)
        val fragCode = fragment.createCode(true, outputs)
        val shader = Shader(name, geometry?.code, vertCode, "", fragCode)
        shader.glslVersion = max(330, max(glslVersion, shader.glslVersion))
        return shader
    }


    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val sett = DeferredSettingsV2(listOf(DeferredLayerType.POSITION, DeferredLayerType.COLOR), false)
            val test = ShaderBuilder("test", sett)
            test.vertex.add(
                ShaderStage(
                    "stage0", listOf(
                        Variable("vec4", "attr1"),
                        Variable("vec2", "attr0", false)
                    ), "attr0 = attr1.xy;\n"
                )
            )
            test.vertex.add(
                ShaderStage(
                    "vert", listOf(
                        Variable("vec2", "attr0"),
                        // Variable("vec4", "gl_Position", false) // built-in, must not be redefined
                    ), "gl_Position = vec4(attr0,0,1);\n"
                )
            )
            test.fragment.add(
                ShaderStage(
                    "func0", listOf(
                        Variable("vec3", "tint", true),
                        Variable("vec3", "finalColor", false),
                        Variable("float", "finalAlpha", false)
                    ), "finalColor = tint;\n" +
                            "finalAlpha = 0.5;\n"
                )
            )
            HiddenOpenGLContext.createOpenGL()
            val shader = test.create()
            println("// ---- vertex shader ----")
            println(indent(shader.vertex))
            println("// --- fragment shader ---")
            println(indent(shader.fragment))
            shader.use()
        }


        // a little auto-formatting
        fun indent(text: String): String {
            val lines = text.split("\n")
            val result = StringBuilder(lines.size * 3)
            var depth = 0
            for (i in lines.indices) {
                if (i > 0) result.append('\n')
                val line0 = lines[i]
                val line1 = line0.trim()
                var endIndex = line1.indexOf("//")
                if (endIndex < 0) endIndex = line1.length
                val line2 = line1.substring(0, endIndex)
                val depthDelta =
                    line2.count { it == '(' || it == '{' || it == '[' } - line2.count { it == ')' || it == ']' || it == '}' }
                val firstDepth = when (line2.getOrElse(0) { '-' }) {
                    ')', '}', ']' -> true
                    else -> false
                }
                if (firstDepth) depth += depthDelta
                for (j in 0 until depth) {
                    result.append("  ")
                }
                if (!firstDepth) depth += depthDelta
                result.append(line2)
            }
            return result.toString()
        }
    }

}