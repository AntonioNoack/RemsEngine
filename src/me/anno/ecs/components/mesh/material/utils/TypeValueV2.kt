package me.anno.ecs.components.mesh.material.utils

import me.anno.gpu.shader.GLSLType

open class TypeValueV2(type: GLSLType, val getter: () -> Any) : TypeValue(type, Unit) {

    override var value: Any
        get() = getter()
        set(_) {
            // not supported
        }

}