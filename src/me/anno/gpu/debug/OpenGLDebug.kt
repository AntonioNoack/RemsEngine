package me.anno.gpu.debug

import org.lwjgl.opengl.KHRDebug

object OpenGLDebug {

    @JvmStatic
    fun getDebugSourceName(source: Int): String {
        return when (source) {
            KHRDebug.GL_DEBUG_SOURCE_API -> "API"
            KHRDebug.GL_DEBUG_SOURCE_WINDOW_SYSTEM -> "WINDOW_SYSTEM"
            KHRDebug.GL_DEBUG_SOURCE_SHADER_COMPILER -> "SHADER_COMPILER"
            KHRDebug.GL_DEBUG_SOURCE_THIRD_PARTY -> "THIRD_PARTY"
            KHRDebug.GL_DEBUG_SOURCE_APPLICATION -> "APPLICATION"
            KHRDebug.GL_DEBUG_SOURCE_OTHER -> "OTHER"
            else -> source.toString()
        }
    }

    @JvmStatic
    fun getDebugTypeName(type: Int): String {
        return when (type) {
            KHRDebug.GL_DEBUG_TYPE_ERROR -> "ERROR"
            KHRDebug.GL_DEBUG_TYPE_DEPRECATED_BEHAVIOR -> "DEPRECATED_BEHAVIOR"
            KHRDebug.GL_DEBUG_TYPE_UNDEFINED_BEHAVIOR -> "UNDEFINED_BEHAVIOR"
            KHRDebug.GL_DEBUG_TYPE_PORTABILITY -> "PORTABILITY"
            KHRDebug.GL_DEBUG_TYPE_PERFORMANCE -> "PERFORMANCE"
            KHRDebug.GL_DEBUG_TYPE_MARKER -> "MARKER"
            KHRDebug.GL_DEBUG_TYPE_PUSH_GROUP -> "PUSH_GROUP"
            KHRDebug.GL_DEBUG_TYPE_POP_GROUP -> "POP_GROUP"
            KHRDebug.GL_DEBUG_TYPE_OTHER -> "OTHER"
            else -> type.toString()
        }
    }

    @JvmStatic
    fun getDebugSeverityName(type: Int): String {
        return when (type) {
            KHRDebug.GL_DEBUG_SEVERITY_HIGH -> "HIGH"
            KHRDebug.GL_DEBUG_SEVERITY_MEDIUM -> "MEDIUM"
            KHRDebug.GL_DEBUG_SEVERITY_LOW -> "LOW"
            KHRDebug.GL_DEBUG_SEVERITY_NOTIFICATION -> "NOTIFICATION"
            else -> type.toString()
        }
    }
}