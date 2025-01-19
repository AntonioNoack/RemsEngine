package me.anno.tests.shader.unity

import me.anno.gpu.CullMode
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.builder.Variable
import me.anno.io.files.FileReference
import me.anno.utils.OS.desktop
import me.anno.utils.OS.downloads
import me.anno.utils.assertions.assertEquals
import me.anno.utils.files.Files.formatFileSize
import me.anno.utils.types.Booleans.toInt
import me.anno.utils.types.Strings.indexOf2
import java.io.IOException

/**
 * todo parse Unity's shaders and convert them to Rem's Engine
 *  -> needs preprocessor next
 * */
fun main() {
    if (!unityStdLib.exists) throw IllegalStateException("Missing Unity stdlib")
    val src = desktop.getChild("SyntyStudios_Water.shader")
    val tokens = tokenize(src.getParent(), src.readTextSync())
    var i = 0
    assertEquals("Shader", tokens[i++])
    val shaderName = tokens[i++] as String
    val properties = HashMap<String, Triple<String, String, Any>>()
    assertEquals('{', tokens[i++])
    while (tokens[i] != '}') {
        when (tokens[i++]) {
            "Properties" -> {
                assertEquals('{', tokens[i++])
                while (tokens[i] != '}') {
                    while (tokens[i] == '[') {
                        while (tokens[i] != ']') i++
                        i++ // skip ]
                    }
                    val nameInShader = tokens[i++] as String
                    assertEquals('(', tokens[i++])
                    val nameInEditor = tokens[i++] as String
                    assertEquals('"', nameInEditor[0])
                    assertEquals(',', tokens[i++])
                    val type = tokens[i++] as String // Float,Int,Range(),2D,Color
                    if (tokens[i] == '(') { // skip arguments for now
                        while (tokens[i] != ')') i++
                        i++ // skip )
                    }
                    assertEquals(')', tokens[i++])
                    assertEquals('=', tokens[i++])
                    val value = if (tokens[i] == '(') {
                        i++
                        val list = ArrayList<Double>()
                        while (tokens[i] != ')') {
                            list.add(tokens[i++] as Double)
                            if (tokens[i] == ',') i++
                        }
                        i++ // skip )
                        list
                    } else tokens[i++] // String or Double
                    if (tokens[i] == '{') {
                        while (tokens[i] != '}') i++
                        i++ // skip }
                    }
                    properties[nameInShader] = Triple(nameInEditor, type, value)
                }
                i++ // skip }
                println(properties)
            }
            "SubShader" -> {
                assertEquals('{', tokens[i++])
                var culling = CullMode.FRONT
                val tags = HashMap<String, String>()
                while (true) {
                    when (tokens[i++]) {
                        "Cull" -> {
                            culling = when (tokens[i++]) {
                                "Back" -> CullMode.BACK
                                "Front" -> CullMode.FRONT
                                "Off" -> CullMode.BOTH
                                else -> throw NotImplementedError()
                            }
                        }
                        "Tags" -> {
                            assertEquals('{', tokens[i++])
                            while (tokens[i] != '}') {
                                val name = tokens[i++] as String
                                assertEquals('=', tokens[i++])
                                val value = tokens[i++] as String
                                tags[name] = value
                                if (tokens[i] == ',') i++
                            }
                            i++ // skip }
                        }
                        "CGPROGRAM" -> {

                            data class Member(val type: String, val name: String)
                            class Struct {
                                val members = ArrayList<Member>()
                            }

                            val structs = HashMap<String, Struct>()
                            val uniforms = ArrayList<Variable>()
                            while (tokens[i] != "CGEND") {
                                when (val key = tokens[i++]) {
                                    "struct" -> {
                                        val structName = tokens[i++] as String
                                        assertEquals('{', tokens[i++])
                                        val struct = Struct()
                                        while (tokens[i] != '}') {
                                            val type = tokens[i++] as String
                                            val name = tokens[i++] as String
                                            struct.members.add(Member(type, name))
                                            assertEquals(';', tokens[i++])
                                        }
                                        structs[structName] = struct
                                        i++ // skip }
                                        if (tokens[i] == ';') i++
                                        println("Struct $structName { ${struct.members} }")
                                    }
                                    "uniform" -> {
                                        val type = tokens[i++] as String
                                        val name = tokens[i++] as String
                                        val vType = types[type] ?: throw NotImplementedError("Unknown type $type")
                                        uniforms.add(Variable(vType, name))
                                        assertEquals(';', tokens[i++])
                                    }
                                    in types, "void" -> {
                                        val funcTypeName = tokens[i - 1]
                                        val funcType = types[funcTypeName] ?: structs[funcTypeName]
                                        val funcName = tokens[i++] as String
                                        assertEquals('(', tokens[i++])
                                        // Input i, inout SurfaceOutputStandardSpecular o
                                        val parameters = ArrayList<Triple<Any, String, Int>>()
                                        while (tokens[i] != ')') {
                                            val isInput = tokens[i] == "inout" || tokens[i] == "in"
                                            val isOutput = tokens[i] == "inout" || tokens[i] == "out"
                                            if (isInput || isOutput) i++
                                            val type = tokens[i++] as String
                                            val name = tokens[i++] as String
                                            val vType = types[type] ?: structs[type]
                                            ?: throw NotImplementedError("Unknown type $type")
                                            parameters.add(Triple(vType, name, isInput.toInt() + isOutput.toInt(2)))
                                            if (tokens[i] == ',') i++
                                        }
                                        i++ // skip )

                                        TODO("read function $funcType $funcName($parameters)")
                                    }
                                    else -> throw NotImplementedError("Unknown token $key")
                                }
                            }
                            i++ // skip CGEND
                        }
                        else -> throw NotImplementedError("${tokens[i - 1]}")
                    }
                }
                // Tags{ "RenderType" = "Transparent"  "Queue" = "Transparent+0" "IgnoreProjector" = "True" }
                //		Cull Back
                //		CGPROGRAM .. ENDCG
                TODO("Parse sub-shader")
            }
            "Fallback" -> tokens[i++] as String // shader name, like "Diffuse"
            "CustomEditor" -> tokens[i++] as String // script name, like "MaterialInspector"
        }
    }
}

fun tokenize(folder: FileReference, src0: String): ArrayList<Any> {

    // todo add preprocessor

    val src = StringBuilder(src0)
    val tokens = ArrayList<Any>()
    var i = 0

    fun include(fileName: String) {
        var file = folder.getChild(fileName)
        if (!file.exists) file = unityStdLib.getChild(fileName)
        if (!file.exists) {
            if (fileName != "VS_indirect.cginc")
                throw IOException("Missing $fileName, $folder, $unityStdLib")
        } else {
            src.insert(i, file.readTextSync())
            println("Included $file -> ${src.length.toLong().formatFileSize()}")
        }
    }

    fun skipLine() {
        while (i < src.length && src[i] != '\n') {
            i++
        }
        i++ // skip \n
    }

    fun skipLineAdvanced() {
        var endsWithSlash = false
        while (i < src.length) {
            when (src[i++]) {
                '\\' -> endsWithSlash = true
                ' ', '\r', '\t' -> {}
                '\n' -> {
                    if (endsWithSlash) endsWithSlash = false
                    else break
                }
                else -> endsWithSlash = false
            }
        }
    }

    fun readString(padding: Int): String {
        val j = i - 1
        while (true) {
            val ci = src[i++]
            if (ci == '"') break
            if (ci == '\\') i++ // skip next character
        }
        return src.substring(j + padding, i - padding)
    }
    while (i < src.length) {
        when (val c = src[i++]) {
            ' ', '\t', '\r', '\n' -> {}
            '/' -> {
                if (src[i] == '/') {
                    skipLine()
                } else if (src[i] == '*') {
                    i++
                    while (src[i] != '*' || src[i + 1] != '/') {
                        i++
                    }
                    i += 2 // skip */
                } else {
                    tokens.add(c)
                }
            }
            in 'A'..'Z', in 'a'..'z', '_' -> {
                val j = i - 1
                while (true) {
                    val ci = src[i++]
                    if (!(ci in 'A'..'Z' || ci in 'a'..'z' ||
                                ci in '0'..'9' || ci in "_")
                    ) break
                }
                val tk = src.substring(j, --i)
                tokens.add(tk)
                if (tk == "CGPROGRAM") include("UnityPBSLighting.cginc")
            }
            in '0'..'9' -> {
                val j = i - 1
                if (c == '0' && src[i] in "xX") {
                    i++
                    while (true) {
                        val ci = src[i++]
                        if (!(ci in 'A'..'F' || ci in 'a'..'f' || ci in '0'..'9')) break
                    }
                    val tk = src.substring(j + 2, --i)
                    tokens.add(tk.toLong(16).toDouble())
                } else if (c in "23" && src[i] == 'D') {
                    val tk = src.substring(j, ++i)
                    tokens.add(tk)
                } else {
                    while (src[i] in '0'..'9') {
                        i++
                    }
                    if (src[i] == '.') {
                        i++
                        while (src[i] in '0'..'9') {
                            i++
                        }
                    }
                    if (src[i] in "eE") {
                        i++
                        if (src[i] in "+-") i++
                        while (src[i] in '0'..'9') {
                            i++
                        }
                    }
                    val tk = src.substring(j, i)
                    tokens.add(tk.toDouble())
                }
            }
            '"' -> tokens.add(readString(0))
            '#' -> {
                val k = i - 1
                while (src[i] in " \t") i++ // skip whitespace
                val j = i
                while (src[i] in 'A'..'Z' || src[i] in 'a'..'z') i++
                val primaryName = src.substring(j, i)
                while (src[i] in " \t") i++
                when (primaryName) {
                    "include" -> {
                        assertEquals('"', src[i++])
                        val fileName = readString(1)
                        skipLineAdvanced()
                        include(fileName)
                    }
                    // todo these branches need to be respected for includes
                    "if", "else", "endif", "ifdef", "ifndef", "elif", "define", "undef", "error" -> {
                        skipLineAdvanced()
                        tokens.add(src.substring(k, i - 1))
                    }
                    else -> {
                        println("todo: #${src.substring(j, src.indexOf2('\n', j))}")
                        skipLineAdvanced()
                    }
                }
            }
            in "+-*=.,;()[]{}<>?:!&|^" -> tokens.add(c)
            else -> throw NotImplementedError("Symbol $c")
        }
    }
    return tokens
}

val types = hashMapOf(
    "half" to GLSLType.V1F, "float" to GLSLType.V1F, "fixed" to GLSLType.V1F,
    "half2" to GLSLType.V2F, "float2" to GLSLType.V2F, "fixed2" to GLSLType.V2F,
    "half3" to GLSLType.V3F, "float3" to GLSLType.V3F, "fixed3" to GLSLType.V3F,
    "half4" to GLSLType.V4F, "float4" to GLSLType.V4F, "fixed4" to GLSLType.V4F,
    "int" to GLSLType.V1I,
    "int2" to GLSLType.V2I,
    "int3" to GLSLType.V3I,
    "int4" to GLSLType.V4I,
    "float2x2" to GLSLType.M2x2,
    "float3x3" to GLSLType.M3x3,
    "float4x4" to GLSLType.M4x4,
    "float4x3" to GLSLType.M4x3, // correct?
    "sampler2D" to GLSLType.S2D, "sampler2D_half" to GLSLType.S2D, "sampler2D_float" to GLSLType.S2D,
    "samplerCUBE" to GLSLType.SCube, "samplerCUBE_half" to GLSLType.SCube, "samplerCUBE_float" to GLSLType.SCube,
)

// from https://github.com/TwoTailsGames/Unity-Built-in-Shaders
val unityStdLib = downloads.getChild("Unity-Built-in-Shaders-master.zip/Unity-Built-in-Shaders-master/CGIncludes")
