package me.anno.gpu.deferred

class DeferredSettings(
    val layers: List<DeferredLayer>,
    val fpLights: Boolean
){
    val colorLayer = layers.first()
    val samplerUniforms = "uniform sampler2D ${layers.joinToString { it.name }};\n"
    val layerNames = layers.map { it.name }
}