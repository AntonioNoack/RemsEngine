package me.anno.gpu.deferred

class DeferredSettingsV1(
    val layers: List<DeferredLayer>,
    val fpLights: Boolean
){
    val colorLayer = layers.first()
    val samplerUniforms = "uniform sampler2D ${layers.joinToString { it.name }};\n"
    val layerNames = layers.map { it.name }
    val f3D = layers.withIndex().joinToString("") { (index, name) -> "layout (location = $index) out ${name.glslType} ${name.name};\n" }
}