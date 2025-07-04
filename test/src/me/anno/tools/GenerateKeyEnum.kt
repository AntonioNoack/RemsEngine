package me.anno.tools

import org.lwjgl.glfw.GLFW
import java.lang.reflect.Modifier

fun main() {
    // generates enum values
    for (field in GLFW::class.java.fields) {
        if (Modifier.isStatic(field.modifiers) &&
            field.name.startsWith("GLFW_KEY_")
        ) {
            println("${field.name.substring(5)}(${field[null]}),")
        }
    }
}