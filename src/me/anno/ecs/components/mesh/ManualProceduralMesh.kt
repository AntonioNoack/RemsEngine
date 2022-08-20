package me.anno.ecs.components.mesh

class ManualProceduralMesh : ProceduralMesh() {

    override fun generateMesh(mesh: Mesh) {
        // nothing to do...
    }

    override fun clone(): ProceduralMesh {
        val clone = ManualProceduralMesh()
        copy(clone)
        return clone
    }

    override val className = "ManualProceduralMesh"

}