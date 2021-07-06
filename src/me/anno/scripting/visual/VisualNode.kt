package me.anno.scripting.visual

import org.joml.Vector3f

// todo depth could be used xD, idk if it is workable...
// todo you could zoom in and out..., and discover stuff...
// todo you could zoom in on any function and see its content
class VisualNode: NamedVisual() {

    val position: Vector3f = Vector3f()
    var type = Type.FUNCTION_CALL

    enum class Type {
        INPUT,
        OUTPUT,
        FUNCTION_CALL
    }

    override val className get() = "VisualNode"

}