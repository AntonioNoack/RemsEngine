package me.anno.graph.visual.render.scene

/**
 * bakes the skybox (for reflections and ambient light)
 * */
class BakeSkyboxNode : RenderViewNode("Render Scene", listOf("Int", "Resolution"), emptyList()) {

    init {
        setInput(1, 256) // default resolution
    }

    override fun executeAction() {
        val resolution = getIntInput(1)
        pipeline.bakeSkybox(resolution)
    }
}