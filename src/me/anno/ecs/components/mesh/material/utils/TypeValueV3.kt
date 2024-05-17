package me.anno.ecs.components.mesh.material.utils

import me.anno.gpu.shader.GLSLType

open class TypeValueV3<V: Any>(type: GLSLType, val value0: V, val update: (V) -> Unit) : TypeValue(type, Unit) {

    override var value: Any
        get() {
            update(value0)
            return value0
        }
        set(_) {
            // not supported
        }

}