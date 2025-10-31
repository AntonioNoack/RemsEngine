package org.lwjgl.debug

import me.anno.gpu.GFXState.PUSH_DEBUG_GROUP_MAGIC
import me.anno.utils.structures.lists.Lists.any2
import org.apache.logging.log4j.LogManager
import org.lwjgl.opengl.AMDDebugOutput
import org.lwjgl.opengl.ARBDebugOutput
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL46C
import org.lwjgl.opengl.GLDebugMessageAMDCallback
import org.lwjgl.opengl.GLDebugMessageARBCallback
import org.lwjgl.opengl.GLDebugMessageCallback
import org.lwjgl.opengl.KHRDebug
import org.lwjgl.system.APIUtil
import org.lwjgl.system.Callback
import org.lwjgl.system.MemoryUtil
import java.nio.ByteBuffer

/**
 * OpenGL utility for debugging - memory allocation optimized.
 * The original implementation in GLUtil allocated a 300-long StringBuffer for every message.
 */
object LWJGLDebugCallback {

    // ignored message, because it spams my logs
    private val ignoredSequences = listOf(
        "will use VIDEO memory as the source for buffer object operations",
        "Pixel-path performance warning: Pixel transfer is synchronized with 3D rendering.",
        "is being recompiled based on GL state",
        "Buffer detailed info: Based on the usage hint and actual usage, buffer object"
    ).map { it.encodeToByteArray() }

    private val LOGGER = LogManager.getLogger(LWJGLDebugCallback::class)

    private fun ByteBuffer.contains(str: ByteArray): Boolean {
        return (0 until remaining() - str.size).any { offset ->
            startsWith(str, offset)
        }
    }

    private fun ByteBuffer.startsWith(str: ByteArray, i0: Int): Boolean {
        if (i0 < 0 || i0 + str.size > remaining()) return false
        return str.indices.all { i -> this[i + i0] == str[i] }
    }

    private fun handleMessage(
        id: Int, source: GLSource, issueType: GLIssueType, severity: GLSeverity,
        length: Int, message: Long
    ) {
        if (id == PUSH_DEBUG_GROUP_MAGIC) return

        val messageBytes = MemoryUtil.memByteBuffer(message, length)
        if (ignoredSequences.any2 { sequence -> messageBytes.contains(sequence) }) return

        val message = MemoryUtil.memUTF8(messageBytes)
        val printedMessage = "#${id.toString(16)} $source $issueType $severity: $message"
        if (severity == GLSeverity.HIGH) LOGGER.error(printedMessage)
        else LOGGER.info(printedMessage)
    }

    /**
     * Detects the best debug output functionality to use and creates a callback that prints information to the specified [java.io.PrintStream]. The callback
     * function is returned as a [org.lwjgl.system.Callback], that should be [freed][org.lwjgl.system.Callback.free] when no longer needed.
     */
    fun setupDebugMessageCallback(): Callback? {
        val caps = GL.getCapabilities()

        if (caps.OpenGL43) {
            APIUtil.apiLog("[GL] Using OpenGL 4.3 for error logging.")
            val proc = GLDebugMessageCallback.create { source: Int, type: Int, id: Int, severity: Int,
                                                       length: Int, message: Long, userParam: Long ->
                handleMessage(
                    id, getDebugSource(source), getDebugType(type),
                    getDebugSeverity(severity), length, message
                )
            }
            GL46C.glDebugMessageCallback(proc, MemoryUtil.NULL)
            if ((GL46C.glGetInteger(GL46C.GL_CONTEXT_FLAGS) and GL46C.GL_CONTEXT_FLAG_DEBUG_BIT) == 0) {
                APIUtil.apiLog("[GL] Warning: A non-debug context may not produce any debug output.")
                GL46C.glEnable(GL46C.GL_DEBUG_OUTPUT)
            }
            return proc
        }

        if (caps.GL_KHR_debug) {
            APIUtil.apiLog("[GL] Using KHR_debug for error logging.")
            val proc = GLDebugMessageCallback.create { source: Int, type: Int, id: Int, severity: Int,
                                                       length: Int, message: Long, userParam: Long ->
                handleMessage(
                    id, getDebugSource(source), getDebugType(type),
                    getDebugSeverity(severity), length, message
                )
            }
            KHRDebug.glDebugMessageCallback(proc, MemoryUtil.NULL)
            if (caps.OpenGL30 && (GL46C.glGetInteger(GL46C.GL_CONTEXT_FLAGS) and GL46C.GL_CONTEXT_FLAG_DEBUG_BIT) == 0) {
                APIUtil.apiLog("[GL] Warning: A non-debug context may not produce any debug output.")
                GL46C.glEnable(GL46C.GL_DEBUG_OUTPUT)
            }
            return proc
        }

        if (caps.GL_ARB_debug_output) {
            APIUtil.apiLog("[GL] Using ARB_debug_output for error logging.")
            val proc = GLDebugMessageARBCallback.create { source: Int, type: Int, id: Int, severity: Int,
                                                          length: Int, message: Long, userParam: Long ->
                handleMessage(
                    id, getSourceARB(source), getTypeARB(type),
                    getSeverityARB(severity), length, message
                )
            }
            ARBDebugOutput.glDebugMessageCallbackARB(proc, MemoryUtil.NULL)
            return proc
        }

        if (caps.GL_AMD_debug_output) {
            APIUtil.apiLog("[GL] Using AMD_debug_output for error logging.")
            val proc = GLDebugMessageAMDCallback.create { id: Int, category: Int, severity: Int,
                                                          length: Int, message: Long, userParam: Long ->
                handleMessage(
                    id, getCategoryAMD(category), GLIssueType.OTHER,
                    getSeverityAMD(severity), length, message
                )
            }
            AMDDebugOutput.glDebugMessageCallbackAMD(proc, MemoryUtil.NULL)
            return proc
        }

        APIUtil.apiLog("[GL] No debug output implementation is available.")
        return null
    }

    private fun getDebugSource(source: Int): GLSource {
        return when (source) {
            GL46C.GL_DEBUG_SOURCE_API -> GLSource.API
            GL46C.GL_DEBUG_SOURCE_WINDOW_SYSTEM -> GLSource.WINDOW_SYSTEM
            GL46C.GL_DEBUG_SOURCE_SHADER_COMPILER -> GLSource.SHADER_COMPILER
            GL46C.GL_DEBUG_SOURCE_THIRD_PARTY -> GLSource.THIRD_PARTY
            GL46C.GL_DEBUG_SOURCE_APPLICATION -> GLSource.APPLICATION
            // GL46C.GL_DEBUG_SOURCE_OTHER -> Category.OTHER
            else -> GLSource.OTHER
        }
    }

    private fun getDebugType(type: Int): GLIssueType {
        return when (type) {
            GL46C.GL_DEBUG_TYPE_ERROR -> GLIssueType.ERROR
            GL46C.GL_DEBUG_TYPE_DEPRECATED_BEHAVIOR -> GLIssueType.DEPRECATED_BEHAVIOR
            GL46C.GL_DEBUG_TYPE_UNDEFINED_BEHAVIOR -> GLIssueType.UNDEFINED_BEHAVIOUR
            GL46C.GL_DEBUG_TYPE_PORTABILITY -> GLIssueType.PORTABILITY
            GL46C.GL_DEBUG_TYPE_PERFORMANCE -> GLIssueType.PERFORMANCE
            // GL46C.GL_DEBUG_TYPE_OTHER -> GLIssueType.OTHER
            GL46C.GL_DEBUG_TYPE_MARKER -> GLIssueType.MARKER
            else -> GLIssueType.OTHER
        }
    }

    private fun getDebugSeverity(severity: Int): GLSeverity {
        return when (severity) {
            GL46C.GL_DEBUG_SEVERITY_HIGH -> GLSeverity.HIGH
            GL46C.GL_DEBUG_SEVERITY_MEDIUM -> GLSeverity.MEDIUM
            GL46C.GL_DEBUG_SEVERITY_LOW -> GLSeverity.LOW
            GL46C.GL_DEBUG_SEVERITY_NOTIFICATION -> GLSeverity.NOTIFICATION
            else -> GLSeverity.OTHER
        }
    }

    private fun getSourceARB(source: Int): GLSource {
        return when (source) {
            ARBDebugOutput.GL_DEBUG_SOURCE_API_ARB -> GLSource.API
            ARBDebugOutput.GL_DEBUG_SOURCE_WINDOW_SYSTEM_ARB -> GLSource.WINDOW_SYSTEM
            ARBDebugOutput.GL_DEBUG_SOURCE_SHADER_COMPILER_ARB -> GLSource.SHADER_COMPILER
            ARBDebugOutput.GL_DEBUG_SOURCE_THIRD_PARTY_ARB -> GLSource.THIRD_PARTY
            ARBDebugOutput.GL_DEBUG_SOURCE_APPLICATION_ARB -> GLSource.APPLICATION
            // ARBDebugOutput.GL_DEBUG_SOURCE_OTHER_ARB -> Category.OTHER
            else -> GLSource.OTHER
        }
    }

    private fun getTypeARB(type: Int): GLIssueType {
        return when (type) {
            ARBDebugOutput.GL_DEBUG_TYPE_ERROR_ARB -> GLIssueType.ERROR
            ARBDebugOutput.GL_DEBUG_TYPE_DEPRECATED_BEHAVIOR_ARB -> GLIssueType.DEPRECATED_BEHAVIOR
            ARBDebugOutput.GL_DEBUG_TYPE_UNDEFINED_BEHAVIOR_ARB -> GLIssueType.UNDEFINED_BEHAVIOUR
            ARBDebugOutput.GL_DEBUG_TYPE_PORTABILITY_ARB -> GLIssueType.PORTABILITY
            ARBDebugOutput.GL_DEBUG_TYPE_PERFORMANCE_ARB -> GLIssueType.PERFORMANCE
            // ARBDebugOutput.GL_DEBUG_TYPE_OTHER_ARB -> GLIssueType.OTHER
            else -> GLIssueType.OTHER
        }
    }

    private fun getSeverityARB(severity: Int): GLSeverity {
        return when (severity) {
            ARBDebugOutput.GL_DEBUG_SEVERITY_HIGH_ARB -> GLSeverity.HIGH
            ARBDebugOutput.GL_DEBUG_SEVERITY_MEDIUM_ARB -> GLSeverity.MEDIUM
            ARBDebugOutput.GL_DEBUG_SEVERITY_LOW_ARB -> GLSeverity.LOW
            else -> GLSeverity.OTHER
        }
    }

    private fun getCategoryAMD(category: Int): GLSource {
        return when (category) {
            AMDDebugOutput.GL_DEBUG_CATEGORY_API_ERROR_AMD -> GLSource.API
            AMDDebugOutput.GL_DEBUG_CATEGORY_WINDOW_SYSTEM_AMD -> GLSource.WINDOW_SYSTEM
            AMDDebugOutput.GL_DEBUG_CATEGORY_DEPRECATION_AMD -> GLSource.DEPRECATION
            AMDDebugOutput.GL_DEBUG_CATEGORY_UNDEFINED_BEHAVIOR_AMD -> GLSource.UNDEFINED_BEHAVIOUR
            AMDDebugOutput.GL_DEBUG_CATEGORY_PERFORMANCE_AMD -> GLSource.PERFORMANCE
            AMDDebugOutput.GL_DEBUG_CATEGORY_SHADER_COMPILER_AMD -> GLSource.SHADER_COMPILER
            AMDDebugOutput.GL_DEBUG_CATEGORY_APPLICATION_AMD -> GLSource.APPLICATION
            // AMDDebugOutput.GL_DEBUG_CATEGORY_OTHER_AMD -> Category.OTHER
            else -> GLSource.OTHER
        }
    }

    private fun getSeverityAMD(severity: Int): GLSeverity {
        return when (severity) {
            AMDDebugOutput.GL_DEBUG_SEVERITY_HIGH_AMD -> GLSeverity.HIGH
            AMDDebugOutput.GL_DEBUG_SEVERITY_MEDIUM_AMD -> GLSeverity.MEDIUM
            AMDDebugOutput.GL_DEBUG_SEVERITY_LOW_AMD -> GLSeverity.LOW
            else -> GLSeverity.OTHER
        }
    }
}