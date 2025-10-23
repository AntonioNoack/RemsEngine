package me.anno.input

import me.anno.Time.nanoTime
import me.anno.engine.Events.addEvent
import me.anno.gpu.OSWindow
import me.anno.input.Input.onCharTyped
import me.anno.input.Input.onKeyDown
import me.anno.input.Input.onKeyUp
import me.anno.input.Input.onKeyTyped
import me.anno.input.Input.onMouseMove
import me.anno.input.Input.onMouseDown
import me.anno.input.Input.onMouseUp
import me.anno.input.Input.onMouseWheel
import me.anno.input.Touch.Companion.onTouchDown
import me.anno.input.Touch.Companion.onTouchMove
import me.anno.input.Touch.Companion.onTouchUp
import me.anno.io.files.Reference.getReference
import org.lwjgl.glfw.GLFW
import org.lwjgl.glfw.GLFWDropCallback

object GLFWListeners {

    fun handleDropCallback(window: OSWindow, count: Int, names: Long) {
        if (count <= 0) return
        // it's important to be executed here, because the strings may be GCed otherwise
        val files = (0 until count).mapNotNull { nameIndex ->
            try {
                getReference(GLFWDropCallback.getName(names, nameIndex))
            } catch (_: Exception) {
                null
            }
        }
        addEvent {
            val ws = window.windowStack
            val mouseX = window.mouseX
            val mouseY = window.mouseY
            ws.requestFocus(ws.getPanelAt(mouseX, mouseY), true)
            ws.inFocus0?.onPasteFiles(mouseX, mouseY, files)
        }
    }

    fun handleCharMods(window: OSWindow, codepoint: Int) {
        addEvent { onCharTyped(window, codepoint) }
    }

    fun handleCursorPos(window: OSWindow, x: Double, y: Double) {
        if (nanoTime > window.lastMouseTeleport) {
            addEvent { onMouseMove(window, x.toFloat(), y.toFloat()) }
        }
    }

    fun handleMouseButton(window: OSWindow, button: Int, action: Int) {
        val nanoTime = nanoTime
        addEvent {
            val button1 = Key.byId(button)
            when (action) {
                GLFW.GLFW_PRESS -> onMouseDown(window, button1, nanoTime)
                GLFW.GLFW_RELEASE -> onMouseUp(window, button1, nanoTime)
            }
        }
    }

    fun handleScroll(window: OSWindow, xOffset: Double, yOffset: Double) {
        addEvent { onMouseWheel(window, xOffset.toFloat(), yOffset.toFloat(), true) }
    }

    fun handleKeyCallback(window: OSWindow, window1: Long, key: Int, scancode: Int, action: Int, mods: Int) {
        val time = nanoTime
        if (window1 != window.pointer) {
            // touch events are hacked into GLFW for Windows 7+
            // val pressure = max(1, mods)
            val x = scancode * 0.01f
            val y = action * 0.01f
            addEvent {
                when (mods) {
                    -1 -> onTouchDown(key, x, y)
                    -2 -> onTouchUp(key, x, y)
                    else -> onTouchMove(key, x, y)
                }
            }
        } else addEvent {
            val key1 = Key.byId(key)
            when (action) {
                GLFW.GLFW_PRESS -> onKeyDown(window, key1, time)
                GLFW.GLFW_RELEASE -> onKeyUp(window, key1)
                GLFW.GLFW_REPEAT -> onKeyTyped(window, key1)
            }
            // LOGGER.info("event $key $scancode $action $mods")
        }
    }

    fun registerCallbacks(window: OSWindow) {
        GLFW.glfwSetDropCallback(window.pointer) { _: Long, count: Int, names: Long ->
            handleDropCallback(window, count, names)
        }
        GLFW.glfwSetCharModsCallback(window.pointer) { _, codepoint, _ ->
            handleCharMods(window, codepoint)
        }
        GLFW.glfwSetCursorPosCallback(window.pointer) { _, xPosition, yPosition ->
            handleCursorPos(window, xPosition, yPosition)
        }
        GLFW.glfwSetMouseButtonCallback(window.pointer) { _, button, action, _ ->
            handleMouseButton(window, button, action)
        }
        GLFW.glfwSetScrollCallback(window.pointer) { _, xOffset, yOffset ->
            handleScroll(window, xOffset, yOffset)
        }
        GLFW.glfwSetKeyCallback(window.pointer) { window1, key, scancode, action, mods ->
            handleKeyCallback(window, window1, key, scancode, action, mods)
        }
    }
}