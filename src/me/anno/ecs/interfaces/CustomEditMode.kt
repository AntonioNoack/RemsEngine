package me.anno.ecs.interfaces

import me.anno.engine.ui.render.RenderView
import me.anno.input.MouseButton

interface CustomEditMode {

    // todo selecting this component,
    // todo (or this entity, when there is only one CustomEditMode)
    // todo enables the mode automatically. It can be left with escape


    // todo right click: on, catchable
    // todo middle click: ok, awkward
    // todo left click: only if in special mode, escape with esc?
    // todo     - show border, that in special edit mode
    // todo left drag: move stuff -> overrideable (?)

    // todo functions return whether they consumed the event (?)

    fun getEditModeBorderColor(): Int = 0x00ff00

    fun onEditClick(button: MouseButton): Boolean = false
    fun onEditClick(button: Int) = false

    fun onEditMove(view: RenderView, x: Float, y: Float, dx: Float, dy: Float) {}

    fun onEditDown(button: MouseButton) = false
    fun onEditDown(button: Int) = false

    fun onEditUp(button: MouseButton) = false
    fun onEditUp(button: Int) = false

}