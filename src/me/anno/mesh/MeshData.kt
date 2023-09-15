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
                if (comp.meshFile == InvalidRef)
                    "${comp.className} '${comp.name}' is missing path (${comp.meshFile})"
                else
                    "Mesh '${comp.name}'/'${comp.meshFile}' is missing from ${comp.className}"
            } else "Missing mesh $comp, ${comp::class.simpleName} from ${comp.className}"
        } else "Missing positions ${comp.getMesh()}"
        LOGGER.warn(msg)
    }
}