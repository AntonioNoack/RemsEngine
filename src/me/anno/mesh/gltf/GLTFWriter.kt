package me.anno.mesh.gltf

import me.anno.Engine
import me.anno.ecs.Entity
import me.anno.ecs.components.cache.MaterialCache
import me.anno.ecs.components.mesh.Material
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.MeshComponentBase
import me.anno.ecs.prefab.Prefab
import me.anno.ecs.prefab.PrefabCache
import me.anno.engine.ECSRegistry
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.GPUFiltering
import me.anno.io.Streams.writeLE32
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.io.files.Signature
import me.anno.io.json.JsonWriter
import me.anno.io.zip.InnerFile
import me.anno.maths.Maths.clamp
import me.anno.utils.Color.black3
import me.anno.utils.Color.white4
import me.anno.utils.OS.desktop
import me.anno.utils.OS.downloads
import org.joml.*
import org.lwjgl.opengl.GL11.*
import java.io.ByteArrayOutputStream

object GLTFWriter {

    @JvmStatic
    fun main(args: Array<String>) {
        ECSRegistry.init()
        val main = downloads.getChild("gradientdomain-scenes.zip/gradientdomain-scenes")
        val name = "sponza"
        val sceneMain = main.getChild("$name/$name-gpt.xml/Scene.json")
        write(PrefabCache[sceneMain]!!, desktop.getChild("$name.glb"))
        Engine.requestShutdown()
    }

    fun JsonWriter.write(v: Vector4f) {
        open(true)
        write(v.x)
        write(v.y)
        write(v.z)
        write(v.w)
        close(true)
    }

    fun JsonWriter.write(v: Vector3f) {
        open(true)
        write(v.x)
        write(v.y)
        write(v.z)
        close(true)
    }

    fun JsonWriter.write(v: Vector3d) {
        open(true)
        write(v.x)
        write(v.y)
        write(v.z)
        close(true)
    }

    fun JsonWriter.write(q: Quaterniond) {
        open(true)
        write(q.x)
        write(q.y)
        write(q.z)
        write(q.w)
        close(true)
    }

    fun JsonWriter.write(v: Any) {
        when (v) {
            is Int -> write(v)
            is Vector2f -> write(v)
            is Vector3f -> write(v)
            is String -> {
                // raw copy
                next()
                output.write(v.toByteArray())
            }
            else -> throw NotImplementedError()
        }
    }

    // todo cameras, lights

    fun write(
        scene: Any, dst: FileReference,
        allDepsToFolder: Boolean = false,
        packedDepsToFolder: Boolean = false,
        allDepsToBinary: Boolean = false,
        packedDepsToBinary: Boolean = true,
    ) {

        if (scene is Prefab) {
            write(
                scene.getSampleInstance(), dst,
                allDepsToFolder, packedDepsToFolder,
                allDepsToBinary, packedDepsToBinary
            )
            return
        }

        if (scene !is Mesh && scene !is Entity)
            throw IllegalArgumentException("Cannot export ${scene.javaClass} as GLTF")

        val textures = HashMap<Pair<Int, Int>, Int>() // source, sampler
        val images = HashMap<FileReference, Int>() // uris
        val samplers = HashMap<Pair<GPUFiltering, Clamping>, Int>()
        val materials = HashMap<Material, Int>()
        val meshes = HashMap<Pair<Mesh, List<FileReference>>, Int>()

        val json = ByteArrayOutputStream(1024)
        val binary = ByteArrayOutputStream(4096)
        val writer = JsonWriter(json)
        writer.open(false)

        fun <K> write(name: String, map: Map<K, Int>, write: (K) -> Unit) {
            if (map.isNotEmpty()) {
                writer.attr(name)
                writer.open(true)
                for ((k, _) in map.entries.sortedBy { it.value }) {
                    write(k)
                }
                writer.close(true)
            }
        }

        writer.attr("asset")
        writer.open(false)
        writer.attr("generator")
        writer.write("Rem's Engine")
        writer.attr("version")
        writer.write("2.0")
        writer.close(false)

        writer.attr("scene")
        writer.write(0)

        fun countMeshes(entity: Entity): Int {
            return entity.components.count {
                it is MeshComponentBase && it.getMesh() != null
            }
        }

        val nodes = ArrayList<Any>()
        val children = ArrayList<List<Int>>()
        if (scene is Entity) {
            fun add(entity: Entity): Int {
                val idx = nodes.size
                nodes.add(entity)
                val cm = countMeshes(entity)
                if (entity.children.isNotEmpty() || cm > 1) {
                    val children2 = ArrayList<Int>(entity.children.size + cm)
                    children.add(children2)
                    for (child in entity.children) {
                        children2.add(add(child))
                    }
                    entity.components.filterIsInstance<MeshComponentBase>()
                        .filter { it.getMesh() != null }
                        .forEach {
                            val idx2 = nodes.size
                            nodes.add(it)
                            children2.add(idx2)
                        }
                } else {
                    children.add(emptyList())
                }
                return idx
            }
            add(scene)
        } else {
            scene as Mesh
            nodes.add(MeshComponent(scene.ref))
            meshes[Pair(scene, emptyList())] = 0
        }

        writer.attr("scenes")
        writer.open(true)
        writer.open(false)
        writer.attr("nodes")
        writer.open(true)
        /* for (i in 0 until nodes.size) {
             writer.write(i)
        } */
        writer.write(0) // only root nodes
        writer.close(true)
        writer.close(false)
        writer.close(true)

        writer.attr("nodes")
        writer.open(true)

        if (scene is Entity) scene.validateTransform()
        for ((i, node) in nodes.withIndex()) {

            writer.open(false)

            if (node is Entity) {

                if (countMeshes(node) == 1) {
                    val mesh = node.components
                        .filterIsInstance<MeshComponentBase>()
                        .firstNotNullOfOrNull {
                            val mesh = it.getMesh()
                            if (mesh != null) Pair(mesh, it.materials) else null
                        }
                    if (mesh != null) {
                        writer.attr("mesh")
                        writer.write(meshes.getOrPut(mesh) { meshes.size })
                    }
                }

                val translation = node.transform.localPosition
                if (translation != Vector3d()) {
                    writer.attr("translation")
                    writer.write(translation)
                }

                val rotation = node.transform.localRotation
                if (rotation != Quaterniond()) {
                    writer.attr("rotation")
                    writer.write(rotation)
                }

                val scale = node.transform.localScale
                if (scale.x != 1.0 || scale.y != 1.0 || scale.z != 1.0) {
                    writer.attr("scale")
                    writer.write(scale)
                }

            } else {

                node as MeshComponent
                val mesh = node.getMesh()
                if (mesh != null) {
                    writer.attr("mesh")
                    writer.write(meshes.getOrPut(Pair(mesh, node.materials)) { meshes.size })
                }

            }

            val childrenI = children.getOrNull(i)
            if (childrenI != null && childrenI.isNotEmpty()) {
                writer.attr("children")
                writer.open(true)
                for (child in childrenI) {
                    writer.write(child)
                }
                writer.close(true)
            }

            writer.close(false)
        }
        writer.close(true) // nodes

        class BufferView(
            val offset: Int,
            val length: Int,
            val target: Int
        )

        class Accessor(
            val type: String,
            val componentType: Int,
            val count: Int,
            val min: Any? = null,
            val max: Any? = null
        )

        val views = ArrayList<BufferView>()
        val accessors = ArrayList<Accessor>()

        fun createView(
            type: String,
            componentType: Int,
            data: Any,
            count: Int,
            length: Int,
            min: Any? = null,
            max: Any? = null
        ): Int {
            val pos = binary.size()
            when (data) {
                is IntArray -> {
                    for (v in data) {
                        binary.writeLE32(v)
                    }
                }
                is FloatArray -> {
                    for (v in data) {
                        if (v.isNaN()) {
                            binary.writeLE32(0)
                        } else {
                            binary.writeLE32(v.toRawBits())
                        }
                    }
                }
                else -> throw NotImplementedError(data.javaClass.toString())
            }
            accessors.add(Accessor(type, componentType, count, min, max))
            views.add(BufferView(pos, length, 34962))
            return accessors.size - 1
        }

        fun createIndicesView(
            componentType: Int,
            data: IntArray
        ): Int {
            val pos = binary.size()
            for (v in data) binary.writeLE32(v)
            accessors.add(Accessor("SCALAR", componentType, data.size))
            views.add(BufferView(pos, data.size * 4, 34963))
            return accessors.size - 1
        }

        fun createNormalsView(data: FloatArray): Int {
            val pos = binary.size()
            val v = Vector3f()
            val one = 1f.toRawBits()
            for (i in data.indices step 3) {
                v.set(data[i], data[i + 1], data[i + 2])
                v.normalize()
                if (v.isFinite) {
                    binary.writeLE32(v.x.toRawBits())
                    binary.writeLE32(v.y.toRawBits())
                    binary.writeLE32(v.z.toRawBits())
                } else {
                    binary.writeLE32(0)
                    binary.writeLE32(one)
                    binary.writeLE32(0)
                }
            }
            val length = data.size * 4
            accessors.add(Accessor("VEC3", GL_FLOAT, data.size / 3))
            views.add(BufferView(pos, length, 34962))
            return accessors.size - 1
        }

        fun createUVView(data: FloatArray): Int {
            val pos = binary.size()
            for (i in data.indices step 2) {
                val u = data[i]
                val v = data[i + 1]
                if (u.isNaN() || v.isNaN()) {
                    binary.writeLE32(0)
                    binary.writeLE32(0)
                } else {
                    binary.writeLE32(u.toRawBits())
                    binary.writeLE32((1f - v).toRawBits())
                }
            }
            accessors.add(Accessor("VEC2", GL_FLOAT, data.size / 2))
            views.add(BufferView(pos, data.size * 4, 34962))
            return accessors.size - 1
        }


        write("meshes", meshes) { (mesh, materialOverrides) ->

            writer.open(false)

            mesh.ensureBounds()
            mesh.ensureNorTanUVs()

            val pos = mesh.positions!!
            val posI = createView(
                "VEC3", GL_FLOAT, pos,
                pos.size / 3, pos.size * 4,
                "[${mesh.aabb.minX},${mesh.aabb.minY},${mesh.aabb.minZ}]",
                "[${mesh.aabb.maxX},${mesh.aabb.maxY},${mesh.aabb.maxZ}]",
            )

            val normal = mesh.normals
            val norI = if (normal != null)
                createNormalsView(normal)
            else null

            val uv = mesh.uvs
            val uvI = if (uv != null) createUVView(uv) else null

            val color = mesh.color0
            val colorI = if (color != null)
                createView(
                    "VEC4", GL_UNSIGNED_BYTE,
                    color, color.size, color.size * 4
                )
            else null

            writer.attr("primitives")
            writer.open(true)

            fun attributes() {
                writer.attr("attributes")
                writer.open(false)

                writer.attr("POSITION")
                writer.write(posI)

                if (norI != null) {
                    writer.attr("NORMAL")
                    writer.write(norI)
                }

                if (uvI != null) {
                    writer.attr("TEXCOORD_0")
                    writer.write(uvI)
                }

                // todo test vertex colors
                if (colorI != null) {
                    writer.attr("COLOR_0")
                    writer.write(colorI)
                }

                // todo skinning and animation support

                writer.close(false) // attr
            }

            // for each material add a primitive
            val matIds = mesh.materialIds
            val ownHelpers = mesh.helperMeshes == null
            val helpers = if (matIds != null) {
                if (ownHelpers) mesh.createHelperMeshes(matIds, false)
                mesh.helperMeshes
            } else null

            fun getMaterial(i: Int): Material? {
                val materialRef = materialOverrides.getOrNull(0)?.nullIfUndefined() ?: mesh.materials.getOrNull(i)
                return MaterialCache[materialRef]
            }

            if (helpers != null) {

                for ((i, helper) in helpers.withIndex()) {
                    helper ?: continue

                    writer.open(false)
                    writer.attr("mode")
                    writer.write(4) // triangles

                    val material = getMaterial(i)
                    if (material != null) {
                        writer.attr("material")
                        writer.write(materials.getOrPut(material) { materials.size })
                    }

                    val indices = helper.indices
                    writer.attr("indices")
                    writer.write(createIndicesView(GL_UNSIGNED_INT, indices))

                    attributes()
                    writer.close(false) // primitive

                }

                if (ownHelpers) mesh.helperMeshes = null // because they have no buffers

            } else {

                writer.open(false)
                writer.attr("mode")
                writer.write(4) // triangles

                val material = getMaterial(0)
                if (material != null) {
                    writer.attr("material")
                    writer.write(materials.getOrPut(material) { materials.size })
                }

                val indices = mesh.indices
                if (indices != null) {
                    writer.attr("indices")
                    writer.write(createIndicesView(GL_UNSIGNED_INT, indices))
                }

                attributes()

                writer.close(false) // primitive

            }

            writer.close(true) // primitives[]
            writer.close(false) // mesh
        }

        write("materials", materials) { material ->
            // https://github.com/KhronosGroup/glTF/blob/main/specification/2.0/schema/material.schema.json
            writer.open(false)
            writer.attr("pbrMetallicRoughness")
            writer.open(false)
            val sampler = if (material.emissiveMap != InvalidRef || material.diffuseMap != InvalidRef) {
                samplers.getOrPut(
                    Pair(
                        if (material.linearFiltering) GPUFiltering.TRULY_LINEAR
                        else GPUFiltering.TRULY_NEAREST,
                        material.clamping
                    )
                ) { samplers.size }
            } else -1
            if (material.diffuseMap != InvalidRef) {
                writer.attr("baseColorTexture")
                writer.open(false)
                writer.attr("index")
                writer.write(
                    textures.getOrPut(
                        Pair(images.getOrPut(material.diffuseMap) { images.size }, sampler)
                    ) { textures.size }
                )
                writer.close(false)
            }
            val color = material.diffuseBase
            if (color != white4) {
                writer.attr("baseColorFactor")
                if (color.x in 0f..1f && color.y in 0f..1f && color.z in 0f..1f && color.w in 0f..1f) {
                    writer.write(material.diffuseBase)
                } else {
                    writer.write(
                        Vector4f(
                            clamp(color.x),
                            clamp(color.y),
                            clamp(color.z),
                            clamp(color.w)
                        )
                    )
                }
            }
            writer.attr("metallicFactor")
            writer.write(material.metallicMinMax.y)
            writer.attr("roughnessFactor")
            writer.write(material.roughnessMinMax.y)
            writer.close(false)
            if (material.isDoubleSided) {
                writer.attr("doubleSided")
                writer.write(true)
            }
            if (material.emissiveMap != InvalidRef) {
                writer.attr("emissiveTexture")
                writer.open(false)
                writer.attr("index")
                writer.write(
                    textures.getOrPut(
                        Pair(images.getOrPut(material.emissiveMap) { images.size }, sampler)
                    ) { textures.size }
                )
                writer.close(false)
            }
            if (material.emissiveBase != black3) {
                writer.attr("emissiveFactor")
                writer.write(material.emissiveBase)
            }
            writer.close(false)
        }

        write("textures", textures) { (source, sampler) ->
            writer.open(false)
            writer.attr("source")
            writer.write(source)
            writer.attr("sampler")
            writer.write(sampler)
            writer.close(false)
        }

        val dstParent = dst.getParent() ?: InvalidRef
        write("images", images) {
            writer.open(false)
            // if contains inaccessible assets, pack them, or write them to same directory
            val sameFolder = it.getParent() == dstParent
            val packed = it is InnerFile
            when {
                (packed && packedDepsToFolder) || (!sameFolder && allDepsToFolder) -> {
                    // copy the file
                    val newFile = dstParent.getChild(it.name)
                    newFile.writeFile(it) {}
                    writer.attr("uri")
                    writer.write(newFile.absolutePath) // todo relative path to file
                }
                (packed && packedDepsToBinary) || (!sameFolder && allDepsToBinary) -> {
                    // "bufferView": 3,
                    // "mimeType" : "image/jpeg"
                    writer.attr("bufferView")
                    writer.write(views.size)
                    val pos0 = binary.size()
                    it.inputStreamSync().copyTo(binary)
                    val pos1 = binary.size()
                    views.add(BufferView(pos0, pos1 - pos0, 0))
                    val ext = when (Signature.findNameSync(it)) {
                        "png" -> "image/png"
                        "jpg" -> "image/jpeg"
                        else -> null
                    }
                    if (ext != null) {
                        writer.attr("mimeType")
                        writer.write(ext)
                    }
                }
                else -> {
                    writer.attr("uri")
                    writer.write(it.absolutePath) // todo relative path to file
                }
            }
            writer.close(false)
        }

        write("samplers", samplers) { (filtering, clamping) ->
            writer.open(false)
            writer.attr("magFilter")
            writer.write(filtering.mag)
            writer.attr("minFilter")
            writer.write(filtering.min)
            writer.attr("wrapS")
            writer.write(clamping.mode)
            writer.attr("wrapT")
            writer.write(clamping.mode)
            writer.close(false)
        }

        writer.attr("bufferViews")
        writer.open(true)
        for (i in views.indices) {
            val view = views[i]
            writer.open(false)
            writer.attr("buffer")
            writer.write(0)
            writer.attr("byteOffset")
            writer.write(view.offset)
            writer.attr("byteLength")
            writer.write(view.length)
            // writer.attr("byteStride")
            // writer.write(view.stride)
            if (view.target != 0) {
                writer.attr("target")
                writer.write(view.target)
            }
            writer.close(false)
        }
        writer.close(true)

        writer.attr("accessors")
        writer.open(true)
        for (i in accessors.indices) {
            val acc = accessors[i]
            writer.open(false)
            writer.attr("bufferView")
            writer.write(i)
            writer.attr("type")
            writer.write(acc.type)
            writer.attr("componentType")
            writer.write(acc.componentType)
            writer.attr("count")
            writer.write(acc.count)
            if (acc.min != null && acc.max != null) {
                writer.attr("min")
                writer.write(acc.min)
                writer.attr("max")
                writer.write(acc.max)
            }
            writer.close(false)
        }
        writer.close(true)

        writer.attr("buffers")
        writer.open(true)
        writer.open(false)
        writer.attr("byteLength")
        writer.write(binary.size())
        writer.close(false)
        writer.close(true)

        writer.close(false)
        writer.finish()

        while (json.size() and 3 != 0) {
            json.write(' '.code)
        }

        while (binary.size() and 3 != 0) {
            binary.write(0)
        }

        val out = dst.outputStream()
        out.write('g'.code)
        out.write('l'.code)
        out.write('T'.code)
        out.write('F'.code)

        out.writeLE32(2) // version
        out.writeLE32(12 + 8 + 8 + json.size() + binary.size()) // total file size

        // json chunk
        out.writeLE32(json.size())
        out.write('J'.code)
        out.write('S'.code)
        out.write('O'.code)
        out.write('N'.code)
        out.write(json.toByteArray())

        // binary chunk
        out.writeLE32(binary.size())
        out.write('B'.code)
        out.write('I'.code)
        out.write('N'.code)
        out.write(0)
        out.write(binary.toByteArray())

        out.close()

    }

}