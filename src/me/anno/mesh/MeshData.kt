package me.anno.mesh

import me.anno.cache.data.ICacheData
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.MeshComponentBase
import me.anno.io.files.InvalidRef
import me.anno.mesh.assimp.AnimGameItem
import org.apache.logging.log4j.LogManager

open class MeshData : ICacheData {

    var lastWarning: String? = null
    var assimpModel: AnimGameItem? = null

    override fun destroy() {
        // destroy assimp data? no, it uses caches and is cleaned automatically
    }

    companion object {
        private val LOGGER = LogManager.getLogger(MeshData::class)
        fun warnMissingMesh(comp: MeshComponentBase, mesh: Mesh?) {
            if (mesh == null) {
                if (comp is MeshComponent) {
                    if (comp.mesh == InvalidRef) {
                        LOGGER.warn("MeshComponent '${comp.name}' is missing path (${comp.mesh})")
                    } else {
                        LOGGER.warn("Mesh '${comp.name}'/'${comp.mesh}' is missing from MeshComponent")
                    }
                } else {
                    LOGGER.warn("Missing mesh $comp, ${comp::class.simpleName}")
                }
            } else {
                LOGGER.warn("Missing positions ${comp.getMesh()}")
            }
        }
    }

}