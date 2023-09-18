package me.anno.tests.ui

import me.anno.gpu.GFX
import me.anno.gpu.OSWindow
import me.anno.input.Input
import me.anno.input.Key
import me.anno.ui.Panel
import me.anno.ui.Window
import me.anno.ui.Style

open class UITests {

    val style = Style(null, null)
    val osWindow = OSWindow("Sth")
    val windowStack = osWindow.windowStack

    fun prepareUI(panel: Panel): Window {
        GFX.windows.add(osWindow)
        val window = Window(panel, false, windowStack)
        windowStack.add(window)
        return window
    }

    fun press(key: Key, cp: Char? = null) {
        Input.onKeyPressed(osWindow, key)
        Input.onKeyReleased(osWindow, key)
        if (cp != null) Input.onCharTyped(osWindow, cp.code, 0)
    }
}