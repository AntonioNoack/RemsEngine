package me.anno.mesh.gltf.writer

import me.anno.ecs.components.mesh.material.Material
import me.anno.gpu.CullMode

data class MaterialData(
    val material: Material,
    val isDoubleSided: Boolean
) {
    constructor(material: Material, cullMode: CullMode) :
            this(material, cullMode == CullMode.BOTH)
}