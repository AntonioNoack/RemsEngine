package me.anno.mesh

import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.MeshComponentBase
import me.anno.io.files.InvalidRef
import org.apache.logging.log4j.LogManager

object MeshData {
    private val LOGGER = LogManager.getLogger(MeshData::class)
    fun warnMissingMesh(comp: MeshComponentBase, mesh: Mesh?) {
        val msg = if (mesh == null) {
            if (comp is MeshComponent) {
                if (comp.mesh == InvalidRef)
                    "MeshComponent '${comp.name}' is missing path (${comp.mesh})"
                else
                    "Mesh '${comp.name}'/'${comp.mesh}' is missing from MeshComponent"
            } else "Missing mesh $comp, ${comp::class.simpleName}"
        } else "Missing positions ${comp.getMesh()}"
        LOGGER.warn(msg)
    }
}