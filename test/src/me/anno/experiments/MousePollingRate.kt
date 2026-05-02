package me.anno.experiments

import me.anno.utils.types.Floats.f2
import org.joml.Vector2d
import org.lwjgl.glfw.GLFW.glfwCreateWindow
import org.lwjgl.glfw.GLFW.glfwDestroyWindow
import org.lwjgl.glfw.GLFW.glfwInit
import org.lwjgl.glfw.GLFW.glfwMakeContextCurrent
import org.lwjgl.glfw.GLFW.glfwPollEvents
import org.lwjgl.glfw.GLFW.glfwSetCursorPosCallback
import org.lwjgl.glfw.GLFW.glfwShowWindow
import org.lwjgl.glfw.GLFW.glfwSwapBuffers
import org.lwjgl.glfw.GLFW.glfwTerminate
import org.lwjgl.glfw.GLFW.glfwWindowShouldClose
import org.lwjgl.system.MemoryUtil.NULL
import kotlin.system.exitProcess

/**
 * Chatchy generated test to find mouse update rate
 * for my Logitech M705, it prints ~128 events/s
 * */
fun main() {

    check(glfwInit()) { "Failed to initialize GLFW" }

    val window = glfwCreateWindow(640, 480, "Mouse Polling Test", NULL, NULL)
    if (window == NULL) {
        glfwTerminate()
        error("Failed to create window")
    }

    glfwMakeContextCurrent(window)
    glfwShowWindow(window)

    val mousePosition = Vector2d()
    var movementStartTime = 0L
    var lastMovementTime = 0L
    var eventCount = 0

    glfwSetCursorPosCallback(window, { _, x, y ->
        val now = System.nanoTime()
        if (x != mousePosition.x || y != mousePosition.y) {
            if (eventCount == 0) {
                movementStartTime = now
            }
            eventCount++
            lastMovementTime = now
        }

        mousePosition.set(x, y)
    })

    while (!glfwWindowShouldClose(window)) {
        glfwPollEvents()

        if (eventCount > 0) {
            val durationSec = (lastMovementTime - movementStartTime) / 1e9
            if (durationSec > 0.3) {
                val frequency = eventCount / durationSec

                println("Mouse update rate: ${frequency.f2()} events/sec")

                // reset
                eventCount = 0
                movementStartTime = 0L
                lastMovementTime = 0L
            }
        }

        glfwSwapBuffers(window)
    }

    glfwDestroyWindow(window)
    glfwTerminate()
    exitProcess(0)
}