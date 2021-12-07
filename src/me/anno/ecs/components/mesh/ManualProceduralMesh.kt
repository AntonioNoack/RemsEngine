package me.anno.ecs.components.mesh

class ManualProceduralMesh : ProceduralMesh() {

    override fun generateMesh() {
        // nothing to do...
    }

    override fun clone(): ProceduralMesh {
        val clone = ManualProceduralMesh()
        copy(clone)
        return clone
    }

    override val className: String = "ManualProceduralMesh"

}