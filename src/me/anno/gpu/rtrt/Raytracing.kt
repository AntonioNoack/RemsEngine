package me.anno.gpu.rtrt

import me.anno.Engine
import me.anno.engine.ECSRegistry
import me.anno.gpu.GFX
import me.anno.gpu.GFX.discoverOpenGLNames
import me.anno.gpu.shader.OpenGLShader
import org.lwjgl.opengl.GL11C.glGetIntegerv
import org.lwjgl.opengl.GL20
import org.lwjgl.opengl.NVMeshShader
import org.lwjgl.opengl.NVMeshShader.*

fun main() {

    // https://www.khronos.org/registry/OpenGL/extensions/NV/NV_mesh_shader.txt

    // init OpenGL
    ECSRegistry.initWithGFX()

    // todo get raytracing on opengl working Nvidia: GL_NV_mesh_shader
    // todo is there an extension for AMD?

    // task shader
    // mesh shader

    // glDrawMeshTasksNV(first, count)
    // glDrawMeshTasksIndirectNV(pointer)

    val properties = listOf(
        GL_MAX_MESH_UNIFORM_BLOCKS_NV,
        GL_MAX_MESH_TEXTURE_IMAGE_UNITS_NV,
        GL_MAX_MESH_IMAGE_UNIFORMS_NV,
        GL_MAX_MESH_UNIFORM_COMPONENTS_NV,
        GL_MAX_MESH_ATOMIC_COUNTER_BUFFERS_NV,
        GL_MAX_MESH_ATOMIC_COUNTERS_NV,
        GL_MAX_MESH_SHADER_STORAGE_BLOCKS_NV,
        GL_MAX_COMBINED_MESH_UNIFORM_COMPONENTS_NV,

        GL_MAX_TASK_UNIFORM_BLOCKS_NV,
        GL_MAX_TASK_TEXTURE_IMAGE_UNITS_NV,
        GL_MAX_TASK_IMAGE_UNIFORMS_NV,
        GL_MAX_TASK_UNIFORM_COMPONENTS_NV,
        GL_MAX_TASK_ATOMIC_COUNTER_BUFFERS_NV,
        GL_MAX_TASK_ATOMIC_COUNTERS_NV,
        GL_MAX_TASK_SHADER_STORAGE_BLOCKS_NV,
        GL_MAX_COMBINED_TASK_UNIFORM_COMPONENTS_NV,

        GL_MAX_MESH_WORK_GROUP_INVOCATIONS_NV,
        GL_MAX_TASK_WORK_GROUP_INVOCATIONS_NV,

        GL_MAX_MESH_TOTAL_MEMORY_SIZE_NV,
        GL_MAX_TASK_TOTAL_MEMORY_SIZE_NV,

        GL_MAX_MESH_OUTPUT_VERTICES_NV,
        GL_MAX_MESH_OUTPUT_PRIMITIVES_NV,

        GL_MAX_TASK_OUTPUT_COUNT_NV,

        GL_MAX_DRAW_MESH_TASKS_COUNT_NV,

        GL_MAX_MESH_VIEWS_NV,

        GL_MESH_OUTPUT_PER_VERTEX_GRANULARITY_NV,
        GL_MESH_OUTPUT_PER_PRIMITIVE_GRANULARITY_NV,

        GL_MAX_MESH_WORK_GROUP_SIZE_NV,
        GL_MAX_TASK_WORK_GROUP_SIZE_NV,

    )

    // find property names
    discoverOpenGLNames(NVMeshShader::class)

    val tmp = IntArray(1)
    for (property in properties) {
        glGetIntegerv(property, tmp)
        println("${GFX.getName(property)}: ${tmp[0]}")
    }

    /*
        MAX_MESH_UNIFORM_BLOCKS_NV: 14
        MAX_MESH_TEXTURE_IMAGE_UNITS_NV: 32
        MAX_MESH_IMAGE_UNIFORMS_NV: 8
        MAX_MESH_UNIFORM_COMPONENTS_NV: 2048
        MAX_MESH_ATOMIC_COUNTER_BUFFERS_NV: 8
        MAX_MESH_ATOMIC_COUNTERS_NV: 16384
        MAX_MESH_SHADER_STORAGE_BLOCKS_NV: 16
        MAX_COMBINED_MESH_UNIFORM_COMPONENTS_NV: 231424
        MAX_TASK_UNIFORM_BLOCKS_NV: 14
        MAX_TASK_TEXTURE_IMAGE_UNITS_NV: 32
        MAX_TASK_IMAGE_UNIFORMS_NV: 8
        MAX_TASK_UNIFORM_COMPONENTS_NV: 2048
        MAX_TASK_ATOMIC_COUNTER_BUFFERS_NV: 8
        MAX_TASK_ATOMIC_COUNTERS_NV: 16384
        MAX_TASK_SHADER_STORAGE_BLOCKS_NV: 16
        MAX_COMBINED_TASK_UNIFORM_COMPONENTS_NV: 231424
        MAX_MESH_WORK_GROUP_INVOCATIONS_NV: 32
        MAX_TASK_WORK_GROUP_INVOCATIONS_NV: 32
        MAX_MESH_TOTAL_MEMORY_SIZE_NV: 16384
        MAX_TASK_TOTAL_MEMORY_SIZE_NV: 16384
        MAX_MESH_OUTPUT_VERTICES_NV: 256
        MAX_MESH_OUTPUT_PRIMITIVES_NV: 512
        MAX_TASK_OUTPUT_COUNT_NV: 65535
        MAX_DRAW_MESH_TASKS_COUNT_NV: 65535
        MAX_MESH_VIEWS_NV: 4
        MESH_OUTPUT_PER_VERTEX_GRANULARITY_NV: 32
        MESH_OUTPUT_PER_PRIMITIVE_GRANULARITY_NV: 32
        MAX_MESH_WORK_GROUP_SIZE_NV: 32
        MAX_TASK_WORK_GROUP_SIZE_NV: 32
    * */

    open class SimpleShader(name: String, val source: String, val shaderType: Int) : OpenGLShader(name) {
        override fun compile() {

            val program = GL20.glCreateProgram()
            GFX.check()
            updateSession()
            GFX.check()

            val vertexShader = compile(name, program, shaderType, source)

            GFX.check()

            GL20.glLinkProgram(program)
            // these could be reused...
            GL20.glDeleteShader(vertexShader)

            postPossibleError(name, program, false, source, "")

            GFX.check()

            this.program = program

        }

        override fun sourceContainsWord(word: String): Boolean = false
    }

    class MeshShader(name: String, source: String) : SimpleShader(name, source, GL_MESH_SHADER_NV)
    class TaskShader(name: String, source: String) : SimpleShader(name, source, GL_TASK_SHADER_NV)


    // shut down
    Engine.requestShutdown()

}