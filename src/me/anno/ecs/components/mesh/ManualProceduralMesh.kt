package me.anno.ecs.components.mesh

class ManualProceduralMesh : ProceduralMesh() {

    override fun generateMesh(mesh: Mesh) {
        // nothing to do...
    }

    override val className get() = "ManualProceduralMesh"

}