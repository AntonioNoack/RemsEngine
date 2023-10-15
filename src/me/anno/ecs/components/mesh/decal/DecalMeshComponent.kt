package me.anno.ecs.components.mesh.decal

import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshComponentBase
import me.anno.mesh.Shapes
import me.anno.utils.types.Arrays.resize

class DecalMeshComponent : MeshComponentBase() {

    companion object {
        val decalMesh = (Shapes.smoothCube.front.clone() as Mesh).apply {
            ensureNorTanUVs()
            val pos = positions!!
            val nor = normals!!
            val uvs = uvs.resize(nor.size / 3 * 2)
            val tan = tangents.resize(nor.size)
            nor.fill(0f)
            tan.fill(0f)
            var j = 0
            for (i in nor.indices step 3) {
                nor[i + 2] = -1f
                tan[i] = 1f
                uvs[j++] = pos[i] * .5f + .5f
                uvs[j++] = pos[i + 1] * .5f + .5f
            }
            this.uvs = uvs
            this.tangents = tan
        }
    }

    val material = DecalMaterial()

    init {
        materials = listOf(material.ref)
    }

    override fun getMeshOrNull() = decalMesh

}