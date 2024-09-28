package me.anno.ecs.components.mesh.unique

import me.anno.ecs.Transform
import me.anno.ecs.components.mesh.IMesh
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.material.Material
import me.anno.ecs.components.mesh.unique.StaticMeshManager.Companion.attributes
import me.anno.ecs.components.mesh.utils.MeshVertexData
import me.anno.gpu.buffer.DrawMode
import me.anno.gpu.buffer.StaticBuffer
import me.anno.io.files.FileReference
import me.anno.maths.Maths
import me.anno.utils.Color.a
import me.anno.utils.Color.b
import me.anno.utils.Color.g
import me.anno.utils.Color.r
import me.anno.utils.assertions.assertEquals
import me.anno.utils.types.Floats.roundToIntOr
import org.joml.Vector3f
import java.nio.ByteBuffer

class SMMMeshRenderer(material: Material) :
    UniqueMeshRenderer<Mesh, SMMKey>(attributes, MeshVertexData.DEFAULT, DrawMode.TRIANGLES) {

    override val hasVertexColors: Int get() = 1
    override val materials: List<FileReference> = listOf(material.ref)
    override val numMaterials: Int get() = 1

    fun getData0(key: SMMKey, mesh: Mesh): StaticBuffer? {

        // todo call ensureBuffer(), and then use a compute shader instead of this things...

        mesh.ensureNorTanUVs()
        val pos = mesh.positions ?: return null
        val nor = mesh.normals
        val uvs = mesh.uvs
        val tan = mesh.tangents
        val col = mesh.color0
        val matIds = mesh.materialIds
        val transform = key.comp.transform!!
            .apply { validate() }
            .globalTransform
        val v = Vector3f()
        fun putVertex(buffer: ByteBuffer, i: Int) {

            // put vertex
            val i3 = i * 3
            if (i3 + 3 <= pos.size) {
                transform.transformPosition(v.set(pos, i3))
                buffer.putFloat(v.x)
                buffer.putFloat(v.y)
                buffer.putFloat(v.z)
            } else {
                buffer.putFloat(transform.m30.toFloat())
                buffer.putFloat(transform.m31.toFloat())
                buffer.putFloat(transform.m32.toFloat())
            }

            fun putByte(f: Float) {
                val asInt = Maths.clamp(f * 127f, -127f, +127f).roundToIntOr()
                buffer.put(asInt.toByte())
            }

            // put normal
            fun putNor() {
                if (nor != null && i3 + 3 <= nor.size) {
                    transform.transformDirection(v.set(nor, i3)).safeNormalize()
                    putByte(v.x)
                    putByte(v.y)
                    putByte(v.z)
                    buffer.put(127) // ^^
                } else buffer.putInt(0)
            }
            putNor()

            // put uvs
            val i2 = i * 2
            if (uvs != null && i2 + 2 <= uvs.size) {
                buffer.putFloat(uvs[i2])
                buffer.putFloat(uvs[i2 + 1])
            } else {
                buffer.putFloat(0f)
                buffer.putFloat(0f)
            }

            // put tan
            val i4 = i * 4
            if (tan != null && i4 + 4 <= tan.size) {
                // transform tangent
                transform.transformDirection(v.set(tan, i4)).safeNormalize()
                putByte(v.x)
                putByte(v.y)
                putByte(v.z)
                putByte(tan[i4 + 3]) // sign, doesn't really need to be transformed
            } else putNor()

            // put color
            if (col != null && i < col.size) {
                val color = col[i]
                buffer.put(color.r().toByte())
                buffer.put(color.g().toByte())
                buffer.put(color.b().toByte())
                buffer.put(color.a().toByte())
            } else buffer.putInt(-1)
        }

        val indices = mesh.indices
        if (matIds == null && key.index == 0) {
            if (indices != null) {
                when (mesh.drawMode) {
                    DrawMode.TRIANGLES -> {
                        val buffer = StaticBuffer(mesh.name, attributes, indices.size)
                        for (i in indices.indices) putVertex(buffer.nioBuffer!!, indices[i])
                        return buffer
                    }
                    else -> throw NotImplementedError()
                }
            } else {
                when (mesh.drawMode) {
                    DrawMode.TRIANGLES -> {
                        val buffer = StaticBuffer(mesh.name, attributes, pos.size / 9 * 3)
                        for (i in 0 until buffer.vertexCount) {
                            putVertex(buffer.nioBuffer!!, i)
                        }
                        return buffer
                    }
                    else -> throw NotImplementedError()
                }
            }
        } else if (matIds != null) {
            // count how many we need
            val numInstances = matIds.count { it == key.index }
            when (mesh.drawMode) {
                DrawMode.TRIANGLES -> {
                    val buffer = StaticBuffer(mesh.name, attributes, numInstances * 3)
                    val nioBuffer = buffer.nioBuffer!!
                    if (indices != null) {
                        for (mi in matIds.indices) {
                            if (matIds[mi] == key.index) {
                                val mi3 = mi * 3
                                putVertex(nioBuffer, indices[mi3 + 0])
                                putVertex(nioBuffer, indices[mi3 + 1])
                                putVertex(nioBuffer, indices[mi3 + 2])
                            }
                        }
                    } else {
                        for (mi in matIds.indices) {
                            if (matIds[mi] == key.index) {
                                val mi3 = mi * 3
                                putVertex(nioBuffer, mi3 + 0)
                                putVertex(nioBuffer, mi3 + 1)
                                putVertex(nioBuffer, mi3 + 2)
                            }
                        }
                    }
                    return buffer
                }
                else -> throw NotImplementedError()
            }
        } else return null
    }

    override fun getTransformAndMaterial(key: SMMKey, transform: Transform): Material? {
        transform.setGlobal(key.comp.transform!!.globalTransform)
        transform.teleportUpdate()
        return null
    }

    override fun getData(key: SMMKey, mesh: Mesh): StaticBuffer? {
        val data = getData0(key, mesh) ?: return null
        assertEquals(data.vertexCount * data.stride, data.nioBuffer!!.position()) {
            "${data.vertexCount} * ${data.stride} != ${data.nioBuffer!!.position()}"
        }
        data.isUpToDate = false
        return data
    }

    override fun forEachMesh(run: (IMesh, Material?, Transform) -> Boolean) {
        // shall we implement this?
    }
}