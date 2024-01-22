package me.anno.gpu

import me.anno.Build
import me.anno.Time
import org.apache.logging.log4j.LogManager
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
        /*val t0 = Time.nanoTime
        val properties = clazz.staticProperties // this call takes 1000 ms
        val t1 = Time.nanoTime
        println("took ${(t1 - t0) * 1e-9f}s for loading ${glConstants.size} OpenGL names")
        for (property in properties) {
            val name = property.name
            if (name.startsWith("GL_")) {
                val value = property.get()
                if (value is Int) {
                    glConstants[value] = name.substring(3)
                }
            }
        }*/
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