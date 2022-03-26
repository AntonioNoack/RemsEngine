package me.anno.ecs.components.mesh

import me.anno.gpu.shader.GLSLType

open class TypeValueV2(type: GLSLType, val getter: () -> Any) : TypeValue(type, Unit) {

    override val value: Any get() = getter()

}
