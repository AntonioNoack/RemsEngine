package me.anno.ecs.components.mesh.unique

import me.anno.Build
import me.anno.Time
import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.Transform
import me.anno.ecs.components.mesh.*
import me.anno.ecs.interfaces.Renderable
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.gpu.buffer.Attribute
import me.anno.gpu.buffer.AttributeType
import me.anno.gpu.buffer.DrawMode
import me.anno.gpu.buffer.StaticBuffer
import me.anno.gpu.pipeline.Pipeline
import me.anno.gpu.pipeline.Pipeline.Companion.getMaterial
import me.anno.maths.Maths
import me.anno.mesh.Shapes.flatCube
import me.anno.studio.StudioBase
import me.anno.utils.Color.a
import me.anno.utils.Color.b
import me.anno.utils.Color.black
import me.anno.utils.Color.g
import me.anno.utils.Color.r
import org.joml.Vector3f
import java.nio.ByteBuffer
import java.util.*
import kotlin.math.roundToInt

// todo collect all transforms/meshes that have been idle for X iterations
// todo make this like Physics, because it's kind of a global system...
class StaticMeshManager : Component(), Renderable {

    data class Key(val comp: MeshComponentBase, val mesh: Mesh, val index: Int)

    val managers = HashMap<Material, UniqueMeshRenderer<Key>>()
    val meshes = HashSet<MeshComponent>(1024)

    override fun fill(pipeline: Pipeline, entity: Entity, clickId: Int): Int {
        this.clickId = clickId
        for ((_, manager) in managers) {
            manager.fill(pipeline, entity, clickId)
        }
        return clickId + 1
    }

    var numIdleFrames = 3
    var scanLimit = 5000
    private var fullScan = true
    private val collectStackE = ArrayList<Entity>()
    override fun onUpdate(): Int {
        // this entity must only operate on Root level
        if (entity?.parentEntity != null) return 0
        // todo regularly check whether all transforms are still static
        //  do this more spread out: respect scanLimit
        collect()
        return 1
    }

    fun collect() {
        if (collectStackE.isEmpty()) {
            collectStackE.add(entity ?: return)
        }
        val limit = if (fullScan) Int.MAX_VALUE else scanLimit
        val time = Time.frameIndex + numIdleFrames
        fullScan = false
        for (i in 0 until limit) {
            val idx = collectStackE.size - 1
            if (idx < 0) break
            val entity = collectStackE.removeAt(idx)
            val transform = entity.transform
            if (transform.lastUpdateFrameIndex <= time) {
                for (child in entity.children) {
                    collectStackE.add(child)
                }
                for (comp in entity.components) {
                    if (comp is MeshComponentBase && comp.manager == null) {
                        val mesh = comp.getMesh()
                        if (mesh != null && supportsMesh(mesh)) {
                            register(comp, mesh)
                        }
                    }
                }
            }
        }
    }

    fun supportsMesh(mesh: Mesh): Boolean {
        return !mesh.hasBones && supportsDrawMode(mesh.drawMode) &&
                mesh.proceduralLength <= 0 && mesh.positions != null
    }

    fun supportsDrawMode(drawMode: DrawMode): Boolean {
        return when (drawMode) {
            DrawMode.TRIANGLES -> true
            else -> false
        }
    }

    class UMR(material: Material) : UniqueMeshRenderer<Key>(
        attributes, MeshVertexData.DEFAULT,
        material, DrawMode.TRIANGLES
    ) {

        override val hasVertexColors: Int get() = 1

        fun getData0(key: Key, mesh: Mesh): StaticBuffer? {

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
                    val asInt = Maths.clamp(f * 127f, -127f, +127f).roundToInt()
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

        override fun getData(key: Key, mesh: Mesh): StaticBuffer? {
            val data = getData0(key, mesh) ?: return null
            if (data.vertexCount * data.stride != data.nioBuffer!!.position()) {
                throw IllegalStateException("${data.vertexCount} * ${data.stride} != ${data.nioBuffer!!.position()}")
            }
            data.isUpToDate = false
            return data
        }

        override fun forEachMesh(run: (Mesh, Material?, Transform) -> Unit) {
            // shall we implement this?
        }
    }

    fun register(comp: MeshComponentBase, mesh: Mesh) {
        for (i in 0 until mesh.numMaterials) {
            val material = getMaterial(comp.materials, mesh.materials, i)
            val umr = managers.getOrPut(material) { UMR(material) }
            val key = Key(comp, mesh, i)
            val buffer = umr.getData(key, mesh)
            if (buffer != null) {
                umr.add(key, MeshEntry(mesh, mesh.getBounds(), buffer))
            }
        }
        comp.manager = this
    }

    fun unregister(comp: MeshComponentBase) {
        if (meshes.remove(comp)) {
            comp.manager = null
            val mesh = comp.getMesh()
            if (mesh != null) {
                for (i in 0 until mesh.numMaterials) {
                    val material = getMaterial(comp.materials, mesh.materials, i)
                    managers[material]?.remove(Key(comp, mesh, i))
                }
            }
        }
    }

    override val className: String
        get() = "StaticMeshManager"

    companion object {

        val attributes = listOf(
            // total size: 32 bytes
            Attribute("coords", 3),
            Attribute("normals", AttributeType.SINT8_NORM, 4),
            Attribute("uvs", 2),
            Attribute("tangents", AttributeType.SINT8_NORM, 4),
            Attribute("colors0", AttributeType.UINT8_NORM, 4),
        )

        @JvmStatic
        fun main(args: Array<String>) {

            // todo we need to be able to render a 1M scene at 60 fps (current state: 100k at 15 fps)
            //  - hasRenderable needs to be re-evaluated when we change something, so we don't iterate over it in subFill
            //  - size check on subFill is broken, renders even when viewing from 500Mm distance

            // todo when we use this class, clicking in Editor is broken, because clickId isn't persisted / done on GPU

            // disable glGetError()
            Build.isDebug = false

            val mesh = flatCube.front
            val scene = Entity()
            val random = Random(1234L)
            val materials = listOf(
                Material.diffuse(0xff0000 or black),
                Material.diffuse(0x00ff00 or black),
                Material.diffuse(0x0000ff or black),
            ).map { listOf(it.ref) }

            // todo
            //  - and a world with implicit motion
            // done
            //  - test a world with multiple materials
            //  - test a world with tons of meshes / draw calls: they shall be reduced to 1
            //  - create random hierarchy for more realistic testing
            fun create(entity: Entity, mi: Int) {
                for (i in 0 until mi) {
                    val r = 300.0
                    val x = (random.nextFloat() - 0.5f) * r
                    val y = (random.nextFloat() - 0.5f) * r
                    val z = (random.nextFloat() - 0.5f) * r
                    val child = Entity(entity)
                    child.setPosition(x, y, z)
                    val comp = MeshComponent(mesh)
                    comp.materials = materials.random()
                    comp.isInstanced = false // for testing
                    child.add(comp)
                    create(child, mi - 1)
                }
            }
            create(scene, 8)

            scene.add(StaticMeshManager())
            testSceneWithUI("StaticMeshManager", scene) {
                StudioBase.instance?.enableVSync = false
            }
        }
    }
}