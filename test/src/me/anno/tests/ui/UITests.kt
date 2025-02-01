package me.anno.tests.ui

import me.anno.Time
import me.anno.Time.frameTimeNanos
import me.anno.engine.EngineBase
import me.anno.engine.RemsEngine
import me.anno.gpu.GFX
import me.anno.input.Input
import me.anno.input.Input.isKeyDown
import me.anno.input.Key
import me.anno.maths.Maths.SECONDS_TO_NANOS
import me.anno.ui.Panel
import me.anno.ui.Style
import me.anno.ui.Window
import me.anno.utils.types.Booleans.toInt
import org.lwjgl.glfw.GLFW

open class UITests {
    companion object {
        val style = Style(null, null)
        val osWindow = GFX.someWindow
        val engine = RemsEngine()
        val windowStack = osWindow.windowStack
    }

    fun prepareUI(panel: Panel): Window {
        GFX.windows.clear()
        GFX.windows.add(osWindow)
        osWindow.isInFocus = true
        EngineBase.instance = engine
        windowStack.clear()
        return windowStack.push(panel)
    }

    fun hold(key: Key) {
        if (key.isButton()) Input.onMousePress(osWindow, key)
        else Input.onKeyPressed(osWindow, key, Time.nanoTime)
        updateKeyModState()
    }

    fun release(key: Key, cp: Char? = null) {
        if (key.isButton()) Input.onMouseRelease(osWindow, key)
        else Input.onKeyReleased(osWindow, key)
        updateKeyModState()
        if (cp != null) {
            Input.onCharTyped(osWindow, cp.code, 0)
        }
    }

    fun type(key: Key, cp: Char? = null) {
        hold(key)
        release(key, cp)
    }

    fun updateKeyModState() {
        Input.keyModState =
            (isKeyDown(Key.KEY_LEFT_CONTROL) || isKeyDown(Key.KEY_RIGHT_CONTROL)).toInt(GLFW.GLFW_MOD_CONTROL) or
                    (isKeyDown(Key.KEY_LEFT_SHIFT) or isKeyDown(Key.KEY_RIGHT_SHIFT)).toInt(GLFW.GLFW_MOD_SHIFT) or
                    (isKeyDown(Key.KEY_LEFT_ALT) or isKeyDown(Key.KEY_RIGHT_ALT)).toInt(GLFW.GLFW_MOD_ALT) or
                    (isKeyDown(Key.KEY_LEFT_SUPER) or isKeyDown(Key.KEY_RIGHT_SUPER)).toInt(GLFW.GLFW_MOD_SUPER)
    }

    fun click(button: Key) {
        Input.onMousePress(osWindow, button)
        Input.onMouseRelease(osWindow, button)
    }

    fun moveMouseTo(panel: Panel) {
        moveMouseTo(panel.x + panel.width * 0.5f, panel.y + panel.height * 0.5f)
    }

    fun moveMouseTo(x: Float, y: Float) {
        Input.onMouseMove(osWindow, x, y)
    }

    var width = 800
    var height = 600

    fun updateUI() {
        for (window in GFX.windows) {
            window.windowStack.updateTransform(window, 0, 0, width, height)
            engine.updateHoveredAndCursor(window)
            engine.processMouseMovement(window)
            for (windowI in window.windowStack) {
                windowI.update(0, 0, width, height)
            }
        }
    }

    fun callMouseMove() {
        Input.onMouseMove(osWindow, osWindow.mouseX, osWindow.mouseY)
    }

    fun skipTime(dt: Double) {
        val deltaNanos = (dt * SECONDS_TO_NANOS).toLong()
        val timeNanos = frameTimeNanos + deltaNanos
        Time.updateTime(dt, timeNanos)
        // cheat all keys
        for (entry in Input.keysDown) {
            entry.setValue(entry.value - deltaNanos)
        }
    }
}