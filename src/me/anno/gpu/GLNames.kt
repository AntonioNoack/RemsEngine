package me.anno.gpu

import me.anno.Build
import me.anno.Time
import org.apache.logging.log4j.LogManager
import org.lwjgl.opengl.ARBImaging
import org.lwjgl.opengl.GL46C
import java.lang.reflect.Modifier
import kotlin.reflect.KClass

object GLNames {

    private val LOGGER = LogManager.getLogger(GLNames::class)

    @JvmStatic
    fun getName(i: Int): String {
        val constants = glConstants ?: return "$i"
        if (constants.isEmpty()) {
            discoverOpenGLNames()
        }
        return constants[i] ?: "$i"
    }

    @JvmStatic
    fun getErrorTypeName(error: Int): String {
        return when (error) {
            GL46C.GL_INVALID_ENUM -> "invalid enum"
            GL46C.GL_INVALID_VALUE -> "invalid value"
            GL46C.GL_INVALID_OPERATION -> "invalid operation"
            GL46C.GL_STACK_OVERFLOW -> "stack overflow"
            GL46C.GL_STACK_UNDERFLOW -> "stack underflow"
            GL46C.GL_OUT_OF_MEMORY -> "out of memory"
            GL46C.GL_INVALID_FRAMEBUFFER_OPERATION -> "invalid framebuffer operation"
            GL46C.GL_CONTEXT_LOST -> "context lost"
            GL46C.GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT -> "incomplete attachment"
            GL46C.GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT -> "missing attachment"
            GL46C.GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER -> "incomplete draw buffer"
            GL46C.GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER -> "incomplete read buffer"
            GL46C.GL_FRAMEBUFFER_UNSUPPORTED -> "framebuffer unsupported"
            GL46C.GL_FRAMEBUFFER_INCOMPLETE_MULTISAMPLE -> "incomplete multisample"
            GL46C.GL_FRAMEBUFFER_UNDEFINED -> "framebuffer undefined"
            ARBImaging.GL_TABLE_TOO_LARGE -> "table too large (arb imaging)"
            else -> GFX.getName(error)
        }
    }

    // 1696 values in my testing
    @JvmStatic
    private val glConstants = if (Build.isShipped) null else HashMap<Int, String>(2048)

    @JvmStatic
    private fun discoverOpenGLNames() {
        discoverOpenGLNames(GL46C::class)
    }

    @JvmStatic
    private fun discoverOpenGLNames(clazz: KClass<*>) {
        val glConstants = glConstants ?: return
        // literally 300 times faster than the Kotlin code... what is Kotlin doing???
        // 3.5 ms instead of 1000 ms
        val t2 = Time.nanoTime
        discoverOpenGLNames(clazz.java)
        val t3 = Time.nanoTime
        LOGGER.debug("Took ${(t3 - t2) * 1e-9f}s for loading ${glConstants.size} OpenGL names")
    }

    @JvmStatic
    private fun discoverOpenGLNames(clazz: Class<*>) {
        val glConstants = glConstants ?: return
        val properties2 = clazz.declaredFields
        for (property in properties2) {
            if (Modifier.isPublic(property.modifiers) &&
                Modifier.isStatic(property.modifiers)
            ) {
                val name = property.name
                if (name.startsWith("GL_")) {
                    val value = property.get(null)
                    if (value is Int) {
                        glConstants[value] = name.substring(3)
                    }
                }
            }
        }
        discoverOpenGLNames(clazz.superclass ?: return)
    }
}