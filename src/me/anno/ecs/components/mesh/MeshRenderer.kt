package me.anno.ecs.components.mesh

// todo animated mesh renderer as well...
// todo which then uses bones :)

// todo this component is pretty useless:
// todo join this with mesh component

class MeshRenderer : RendererComponent() {

    override fun clone(): MeshRenderer {
        val clone = MeshRenderer()
        copy(clone)
        return clone
    }

    override val className get() = "MeshRenderer"

}