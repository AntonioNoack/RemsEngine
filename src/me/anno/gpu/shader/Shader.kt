package me.anno.gpu.shader

import me.anno.gpu.GFX
import me.anno.objects.cache.CacheData
import org.apache.logging.log4j.LogManager
import org.joml.Vector2f
import org.joml.Vector3f
import org.joml.Vector4f
import org.lwjgl.opengl.GL20.*
import java.lang.RuntimeException

open class Shader(val shaderName: String,
             val vertex: String,
             val varying: String,
             val fragment: String,
             private val disableShorts: Boolean = false): CacheData {

    companion object {
        private val LOGGER = LogManager.getLogger(Shader::class)
        const val attributeName = "in"
        var lastProgram = -1
    }

    private var program = -1

    val pointer get() = program

    // shader compile time doesn't really matter... -> move it to the start to preserve ram use?
    // isn't that much either...
    fun init(){
        program = glCreateProgram()
        // the shaders are like a C compilation process, .o-files: after linking, they can be removed
        val vertexShader = compile(GL_VERTEX_SHADER, ("" +
                "#version 150\n " +
                "${varying.replace("varying", "out")} $vertex").replaceShortCuts())
        val fragmentShader = compile(GL_FRAGMENT_SHADER, ("" +
                "#version 150\n" +
                "precision mediump float; ${varying.replace("varying", "in")} $fragment").replaceShortCuts())
        glLinkProgram(program)
        glDeleteShader(vertexShader)
        glDeleteShader(fragmentShader)
    }

    fun String.replaceShortCuts() = if(disableShorts) this else this
        .replace("\n", " \n ")
        .replace(";", " ; ")
        .replace(" u1 ", " uniform float ")
        .replace(" u2 ", " uniform vec2 ")
        .replace(" u3 ", " uniform vec3 ")
        .replace(" u4 ", " uniform vec4 ")
        .replace(" u2x2 ", " uniform mat2 ")
        .replace(" u3x3 ", " uniform mat3 ")
        .replace(" u4x4 ", " uniform mat4 ")
        .replace(" a1 ", " $attributeName float ")
        .replace(" a2 ", " $attributeName vec2 ")
        .replace(" a3 ", " $attributeName vec3 ")
        .replace(" a4 ", " $attributeName vec4 ")
        .replace(" v1 ", " float ")
        .replace(" v2 ", " vec2 ")
        .replace(" v3 ", " vec3 ")
        .replace(" v4 ", " vec4 ")
        .replace(" m2 ", " mat2 ")
        .replace(" m3 ", " mat3 ")
        .replace(" m4 ", " mat4 ")

    private fun compile(type: Int, source: String): Int {
        // ("$shaderName/$type: $source")
        val shader = glCreateShader(type)
        glShaderSource(shader, source)
        glCompileShader(shader)
        glAttachShader(program, shader)
        postPossibleError(shader, source)
        return shader
    }

    private fun postPossibleError(shader: Int, source: String){
        val log = glGetShaderInfoLog(shader)
        if(log.isNotBlank()){
            LOGGER.warn("$log by\n\n${
            source
                .split('\n')
                .mapIndexed { index, line -> 
                    "${"%1\$3s".format(index+1)}: $line"
                }.joinToString("\n")}")
            throw RuntimeException()
        }
    }

    private val uniformCache = HashMap<String, Int>()
    private val attributeCache = HashMap<String, Int>()

    fun getUniformLocation(name: String): Int {
        val old = uniformCache[name]
        if(old != null) return old
        check()
        val loc = glGetUniformLocation(program, name)
        check()
        uniformCache[name] = loc
        if(loc < 0) LOGGER.warn("Uniform location \"$name\" not found in shader $shaderName")
        return loc
    }

    fun getAttributeLocation(name: String): Int {
        val old = attributeCache[name]
        if(old != null) return old
        val loc = glGetAttribLocation(program, name)
        attributeCache[name] = loc
        if(loc < 0) LOGGER.warn("Attribute location \"$name\" not found in shader $shaderName")
        return loc
    }

    fun use(){
        if(program == -1) init()
        if(program != lastProgram){
            glUseProgram(program)
            lastProgram = program
        }
    }

    fun v3(name: String, color: Int){
        val loc = getUniformLocation(name)
        if(loc > -1) glUniform3f(loc,
            (color.shr(16) and 255)/255f,
            (color.shr(8) and 255)/255f,
            color.and(255)/255f)
    }

    fun v4(name: String, color: Int){
        val loc = getUniformLocation(name)
        if(loc > -1) glUniform4f(loc,
            (color.shr(16) and 255)/255f,
            (color.shr(8) and 255)/255f,
            color.and(255)/255f,
            (color.shr(24) and 255)/255f)
    }


    fun v1(name: String, x: Float){
        val loc = getUniformLocation(name)
        if(loc > -1) glUniform1f(loc, x)
    }

    fun v1(name: String, x: Int){
        val loc = getUniformLocation(name)
        if(loc > -1) glUniform1i(loc, x)
    }

    fun v2(name: String, x: Float, y: Float){
        val loc = getUniformLocation(name)
        if(loc > -1) glUniform2f(loc, x, y)
    }

    fun v3(name: String, x: Float, y: Float, z: Float){
        val loc = getUniformLocation(name)
        if(loc > -1) glUniform3f(loc, x, y, z)
    }

    fun v3X(name: String, v: Vector4f){
        val loc = getUniformLocation(name)
        if(loc > -1) glUniform3f(loc, v.x/v.w, v.y/v.w, v.z/v.w)
    }

    fun v4(name: String, x: Float, y: Float, z: Float, w: Float){
        val loc = getUniformLocation(name)
        if(loc > -1) glUniform4f(loc, x, y, z, w)
    }

    fun v2(name: String, v: Vector2f) = v2(name, v.x, v.y)
    fun v3(name: String, v: Vector3f) = v3(name, v.x, v.y, v.z)
    fun v4(name: String, v: Vector4f) = v4(name, v.x, v.y, v.z, v.w)

    fun check() = GFX.check()

    operator fun get(name: String) = getUniformLocation(name)

    override fun destroy() {
        if(program > -1) glDeleteProgram(program)
    }

}