package me.anno.tests.ui

import me.anno.Time
import me.anno.Time.frameTimeNanos
import me.anno.engine.EngineBase
import me.anno.engine.RemsEngine
import me.anno.gpu.GFX
import me.anno.input.Input
import me.anno.input.Key
import me.anno.maths.Maths.SECONDS_TO_NANOS
import me.anno.ui.Panel
import me.anno.ui.Style
import me.anno.ui.Window

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
        if (key.isButton()) Input.onMouseDown(osWindow, key, Time.nanoTime)
        else Input.onKeyDown(osWindow, key, Time.nanoTime)
    }

    fun release(key: Key, cp: Char? = null) {
        if (key.isButton()) Input.onMouseUp(osWindow, key, Time.nanoTime)
        else Input.onKeyUp(osWindow, key)
        if (cp != null) {
            Input.onCharTyped(osWindow, cp.code)
        }
    }

    fun type(key: Key, cp: Char? = null) {
        hold(key)
        release(key, cp)
    }

    fun click(button: Key) {
        type(button)
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