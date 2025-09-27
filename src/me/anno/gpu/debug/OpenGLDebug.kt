package me.anno.gpu.debug

import org.lwjgl.opengl.KHRDebug.GL_DEBUG_SEVERITY_HIGH
import org.lwjgl.opengl.KHRDebug.GL_DEBUG_SEVERITY_LOW
import org.lwjgl.opengl.KHRDebug.GL_DEBUG_SEVERITY_MEDIUM
import org.lwjgl.opengl.KHRDebug.GL_DEBUG_SEVERITY_NOTIFICATION
import org.lwjgl.opengl.KHRDebug.GL_DEBUG_SOURCE_API
import org.lwjgl.opengl.KHRDebug.GL_DEBUG_SOURCE_APPLICATION
import org.lwjgl.opengl.KHRDebug.GL_DEBUG_SOURCE_OTHER
import org.lwjgl.opengl.KHRDebug.GL_DEBUG_SOURCE_SHADER_COMPILER
import org.lwjgl.opengl.KHRDebug.GL_DEBUG_SOURCE_THIRD_PARTY
import org.lwjgl.opengl.KHRDebug.GL_DEBUG_SOURCE_WINDOW_SYSTEM
import org.lwjgl.opengl.KHRDebug.GL_DEBUG_TYPE_DEPRECATED_BEHAVIOR
import org.lwjgl.opengl.KHRDebug.GL_DEBUG_TYPE_ERROR
import org.lwjgl.opengl.KHRDebug.GL_DEBUG_TYPE_MARKER
import org.lwjgl.opengl.KHRDebug.GL_DEBUG_TYPE_OTHER
import org.lwjgl.opengl.KHRDebug.GL_DEBUG_TYPE_PERFORMANCE
import org.lwjgl.opengl.KHRDebug.GL_DEBUG_TYPE_POP_GROUP
import org.lwjgl.opengl.KHRDebug.GL_DEBUG_TYPE_PORTABILITY
import org.lwjgl.opengl.KHRDebug.GL_DEBUG_TYPE_PUSH_GROUP
import org.lwjgl.opengl.KHRDebug.GL_DEBUG_TYPE_UNDEFINED_BEHAVIOR

@Suppress("unused")
object OpenGLDebug {

    @JvmStatic
    fun getDebugSourceName(source: Int): String {
        return when (source) {
            GL_DEBUG_SOURCE_API -> "API"
            GL_DEBUG_SOURCE_WINDOW_SYSTEM -> "WINDOW_SYSTEM"
            GL_DEBUG_SOURCE_SHADER_COMPILER -> "SHADER_COMPILER"
            GL_DEBUG_SOURCE_THIRD_PARTY -> "THIRD_PARTY"
            GL_DEBUG_SOURCE_APPLICATION -> "APPLICATION"
            GL_DEBUG_SOURCE_OTHER -> "OTHER"
            else -> source.toString()
        }
    }

    @JvmStatic
    fun getDebugTypeName(type: Int): String {
        return when (type) {
            GL_DEBUG_TYPE_ERROR -> "ERROR"
            GL_DEBUG_TYPE_DEPRECATED_BEHAVIOR -> "DEPRECATED_BEHAVIOR"
            GL_DEBUG_TYPE_UNDEFINED_BEHAVIOR -> "UNDEFINED_BEHAVIOR"
            GL_DEBUG_TYPE_PORTABILITY -> "PORTABILITY"
            GL_DEBUG_TYPE_PERFORMANCE -> "PERFORMANCE"
            GL_DEBUG_TYPE_MARKER -> "MARKER"
            GL_DEBUG_TYPE_PUSH_GROUP -> "PUSH_GROUP"
            GL_DEBUG_TYPE_POP_GROUP -> "POP_GROUP"
            GL_DEBUG_TYPE_OTHER -> "OTHER"
            else -> type.toString()
        }
    }

    @JvmStatic
    fun getDebugSeverityName(type: Int): String {
        return when (type) {
            GL_DEBUG_SEVERITY_HIGH -> "HIGH"
            GL_DEBUG_SEVERITY_MEDIUM -> "MEDIUM"
            GL_DEBUG_SEVERITY_LOW -> "LOW"
            GL_DEBUG_SEVERITY_NOTIFICATION -> "NOTIFICATION"
            else -> type.toString()
        }
    }
}