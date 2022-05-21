package me.anno.gpu.rtrt

import me.anno.Engine
import me.anno.engine.ECSRegistry
import me.anno.gpu.GFX
import me.anno.gpu.GFX.discoverOpenGLNames
import org.lwjgl.opengl.GL11C.glGetIntegerv
import org.lwjgl.opengl.NVMeshShader

fun main() {

    // GL_NV_ray_tracing
    // https://github.com/KhronosGroup/GLSL/blob/master/extensions/nv/GLSL_NV_ray_tracing.txt

    // [l060] new stages:
    // [l060] ray generation, intersection, any hit, closest hit, miss stages
    // [l187] any hit: is intersection accepted? | maybe to skip through alpha...
    // order of intersections undefined -> makes sense
    // [l196] flags can be set to skip any-hit on specific instances
    // [l207] closest-hit is executed once iff ray hit sth
    // [l216] miss stage is executed else
    // [l231] callable processor??? common library/buffer??

    // GLSL specific:
    // new keywords:
    //  accelerationStructureNV: opaque handle, used exclusively in traceNV calls
    // [l287] storage qualifiers:
    //  rayPayloadNV
    //  rayPayloadInNV, hitAttributeNV, callableDataNV, callableDataInNV

    // shaderRecordNV? l424

    // l500 has a list of predefined uniforms for all stages
    // todo how do I call this? vkCmdTraceRaysNV()
    // any-hit shader calls reportIntersectionNV()

    /*
    l680, constants for all shading stages, ray-flags argument for traceNV()
    *  const uint gl_RayFlagsNoneNV = 0U;
    const uint gl_RayFlagsOpaqueNV = 1U;
    const uint gl_RayFlagsNoOpaqueNV = 2U;
    const uint gl_RayFlagsTerminateOnFirstHitNV = 4U;
    const uint gl_RayFlagsSkipClosestHitShaderNV = 8U;
    const uint gl_RayFlagsCullBackFacingTrianglesNV = 16U;
    const uint gl_RayFlagsCullFrontFacingTrianglesNV = 32U;
    const uint gl_RayFlagsCullOpaqueNV = 64U;
    const uint gl_RayFlagsCullNoOpaqueNV = 128U;
    * */
    /*
    * available in ray-gen, closest-hit, miss-shaders
    * void traceNV(accelerationStructureNV topLevel,
                   uint rayFlags,
                   uint cullMask,
                   uint sbtRecordOffset,
                   uint sbtRecordStride,
                   uint missIndex,
                   vec3 origin,
                   float Tmin,
                   vec3 direction,
                   float Tmax,
                   int payload);
    * */

    // todo continue at line 713 or give up, because it seems unavailable for OpenGL
    // todo can we combine OpenGL with Vulkan, so we can progressively move from OpenGL to Vulkan?

    // init OpenGL
    ECSRegistry.initWithGFX()

    // todo get raytracing on opengl working Nvidia
    // todo is there an extension for AMD?

    // task shader
    // mesh shader

    // glDrawMeshTasksNV(first, count)
    // glDrawMeshTasksIndirectNV(pointer)

    val properties = listOf<Int>(

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

    /*open class SimpleShader(name: String, val source: String, val shaderType: Int) : OpenGLShader(name) {
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
    class TaskShader(name: String, source: String) : SimpleShader(name, source, GL_TASK_SHADER_NV)*/

    // shut down
    Engine.requestShutdown()

}