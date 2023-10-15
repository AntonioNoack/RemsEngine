package me.anno.mesh.gltf

import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.*
import me.anno.ecs.prefab.Prefab
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.GPUFiltering
import me.anno.io.ISaveable
import me.anno.io.Streams.writeLE32
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.io.files.Signature
import me.anno.io.json.JsonWriter
import me.anno.io.zip.InnerFile
import me.anno.maths.Maths.clamp
import me.anno.utils.Color.b
import me.anno.utils.Color.black3
import me.anno.utils.Color.g
import me.anno.utils.Color.r
import me.anno.utils.Color.white4
import me.anno.utils.structures.tuples.IntPair
import org.joml.*
import org.lwjgl.opengl.GL11.*
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import kotlin.test.assertEquals

/**
 * writes a GLTF file from an Entity or Mesh,
 * writes materials and textures, too
 *
 * todo doesn't support skeletons nor animations yet
 * todo doesn't support cameras nor lights yet
 * */
class GLTFWriter(
    val allDepsToFolder: Boolean = false,
    val packedDepsToFolder: Boolean = false,
    val allDepsToBinary: Boolean = false,
    val packedDepsToBinary: Boolean = true,
    val maxNumBackPaths: Int = 0,
) {

    private class BufferView(
        val offset: Int,
        val length: Int,
        val target: Int,
        val byteStride: Int,
    )

    private class Accessor(
        val type: String,
        val componentType: Int,
        val count: Int,
        val normalized: Boolean,
        val min: String? = null,
        val max: String? = null
    )

    private fun JsonWriter.copyRaw(v: String) {
        // raw copy
        next()
        output.write(v.toByteArray())
    }

    private val textures = HashMap<IntPair, Int>() // source, sampler
    private val images = HashMap<FileReference, Int>() // uris
    private val samplers = HashMap<Pair<GPUFiltering, Clamping>, Int>()
    private val materials = HashMap<Material, Int>()
    private val meshes = HashMap<Pair<Mesh, List<FileReference>>, Int>()

    private val json = ByteArrayOutputStream(1024)
    private val binary = ByteArrayOutputStream(4096)
    private val writer = JsonWriter(json)

    private fun countMeshes(entity: Entity): Int {
        return entity.components.count {
            it is MeshComponentBase && it.getMeshOrNull() != null
        }
    }

    private val nodes = ArrayList<ISaveable>()
    private val children = ArrayList<List<Int>>()

    private fun add(entity: Entity): Int {
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
                .filter { it.getMeshOrNull() != null }
                .forEach {
                    val idx2 = nodes.size
                    nodes.add(it)
                    children2.add(idx2)
                }
        } else children.add(emptyList())
        return idx
    }

    private fun <K> write(name: String, map: Map<K, Int>, write: (K) -> Unit) {
        if (map.isNotEmpty()) {
            writer.attr(name)
            writer.open(true)
            for ((k, _) in map.entries.sortedBy { it.value }) {
                write(k)
            }
            writer.close(true)
        }
    }

    private val views = ArrayList<BufferView>()
    private val accessors = ArrayList<Accessor>()

    private fun createView(
        type: String,
        componentType: Int,
        data: Any,
        count: Int,
        length: Int,
        normalized: Boolean,
        byteStride: Int,
        min: String? = null,
        max: String? = null
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
        accessors.add(Accessor(type, componentType, count, normalized, min, max))
        views.add(BufferView(pos, length, 34962, byteStride))
        return accessors.size - 1
    }

    private fun createColorView(data: IntArray): Int {
        val pos = binary.size()
        for (v in data) {
            binary.write(v.r())
            binary.write(v.g())
            binary.write(v.b())
            binary.write(255) // unused
        }
        accessors.add(Accessor("VEC3", GL_UNSIGNED_BYTE, data.size, true, null, null))
        views.add(BufferView(pos, data.size * 4, 34962, 4))
        return accessors.size - 1
    }

    private fun createIndicesView(
        componentType: Int,
        data: IntArray
    ): Int {
        val pos = binary.size()
        for (v in data) binary.writeLE32(v)
        accessors.add(Accessor("SCALAR", componentType, data.size, false))
        views.add(BufferView(pos, data.size * 4, 34963, 0))
        return accessors.size - 1
    }

    private fun createNormalsView(data: FloatArray): Int {
        val pos = binary.size()
        val v = Vector3f()
        val bounds = AABBf()
        val one = 1f.toRawBits()
        for (i in data.indices step 3) {
            v.set(data[i], data[i + 1], data[i + 2])
            v.normalize()
            if (v.isFinite) {
                binary.writeLE32(v.x.toRawBits())
                binary.writeLE32(v.y.toRawBits())
                binary.writeLE32(v.z.toRawBits())
                bounds.union(v.x, v.y, v.z)
            } else {
                binary.writeLE32(0)
                binary.writeLE32(one)
                binary.writeLE32(0)
                bounds.union(0f, 1f, 0f)
            }
        }
        val length = data.size * 4
        accessors.add(Accessor("VEC3", GL_FLOAT, data.size / 3, false, min(bounds), max(bounds)))
        views.add(BufferView(pos, length, 34962, 0))
        return accessors.size - 1
    }

    private fun min(bounds: AABBf) = "[${bounds.minX},${bounds.minY},${bounds.minZ}]"
    private fun max(bounds: AABBf) = "[${bounds.maxX},${bounds.maxY},${bounds.maxZ}]"

    private fun createUVView(data: FloatArray): Int {
        val pos = binary.size()
        val bounds = AABBf()
        for (i in data.indices step 2) {
            var u = data[i]
            var v = 1f - data[i + 1]
            if (u.isNaN()) u = 0f
            if (v.isNaN()) v = 0f
            binary.writeLE32(u.toRawBits())
            binary.writeLE32(v.toRawBits())
            bounds.union(u, v, 0f)
        }
        val acc = Accessor(
            "VEC2", GL_FLOAT, data.size / 2, false,
            "[${bounds.minX},${bounds.minY}]", "[${bounds.maxX},${bounds.maxY}]"
        )
        accessors.add(acc)
        views.add(BufferView(pos, data.size * 4, 34962, 0))
        return accessors.size - 1
    }

    private fun getTextureIndex(source: FileReference, sampler: Int): Int {
        return textures.getOrPut(
            IntPair(images.getOrPut(source) { images.size }, sampler)
        ) { textures.size }
    }

    private fun writeSamplers() {
        write("samplers", samplers) { (filtering, clamping) ->
            writeSampler(filtering, clamping)
        }
    }

    private fun writeSampler(filtering: GPUFiltering, clamping: Clamping) {
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

    private fun writeBufferViews() {
        writer.attr("bufferViews")
        writer.open(true)
        for (i in views.indices) {
            val view = views[i]
            writeBufferView(view)
        }
        writer.close(true)
    }

    private fun writeBufferView(bufferView: BufferView) {
        writer.open(false)
        writer.attr("buffer")
        writer.write(0)
        writer.attr("byteOffset")
        writer.write(bufferView.offset)
        writer.attr("byteLength")
        writer.write(bufferView.length)
        if (bufferView.byteStride != 0) {
            writer.attr("byteStride")
            writer.write(bufferView.byteStride)
        }
        if (bufferView.target != 0) {
            writer.attr("target")
            writer.write(bufferView.target)
        }
        writer.close(false)
    }

    private fun writeEntityAttributes(node: Entity) {
        if (countMeshes(node) == 1) {
            val mesh = node.components
                .filterIsInstance<MeshComponentBase>()
                .firstNotNullOfOrNull {
                    val mesh = it.getMeshOrNull()
                    if (mesh != null) Pair(mesh, it.materials) else null
                }
            if (mesh != null) {
                writer.attr("mesh")
                writer.write(meshes.getOrPut(mesh) { meshes.size })
            }
        }

        val name = node.name
        if (name.isNotEmpty()) {
            writer.attr("name")
            writer.write(name)
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
    }

    private fun writeMaterials() {
        write("materials", materials) { material ->
            writeMaterial(material)
        }
    }

    private fun writeMaterial(material: Material) {
        // https://github.com/KhronosGroup/glTF/blob/main/specification/2.0/schema/material.schema.json
        writer.open(false)
        writer.attr("pbrMetallicRoughness")
        writer.open(false)
        val sampler = if (
            material.emissiveMap.exists ||
            material.diffuseMap.exists ||
            material.normalMap.exists ||
            material.occlusionMap.exists
        ) {
            samplers.getOrPut(
                Pair(
                    if (material.linearFiltering) GPUFiltering.TRULY_LINEAR
                    else GPUFiltering.TRULY_NEAREST,
                    material.clamping
                )
            ) { samplers.size }
        } else -1
        if (material.diffuseMap.exists) {
            writer.attr("baseColorTexture")
            writer.open(false)
            writer.attr("index")
            writer.write(getTextureIndex(material.diffuseMap, sampler))
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
        if (material.emissiveMap.exists) {
            writer.attr("emissiveTexture")
            writer.open(false)
            writer.attr("index")
            writer.write(getTextureIndex(material.emissiveMap, sampler))
            writer.close(false)
        }
        if (material.emissiveBase != black3) {
            writer.attr("emissiveFactor")
            writer.write(material.emissiveBase)
        }
        if (material.normalMap.exists) {
            writer.attr("normalTexture")
            writer.open(false)
            writer.attr("index")
            writer.write(getTextureIndex(material.normalMap, sampler))
            writer.close(false)
        }
        if (material.occlusionMap.exists) {
            writer.attr("occlusionTexture")
            writer.open(false)
            writer.attr("index")
            writer.write(getTextureIndex(material.occlusionMap, sampler))
            writer.close(false)
        }
        writer.close(false)
    }

    private fun writeMeshes() {
        write("meshes", meshes) { (mesh, materialOverrides) ->
            writeMesh(mesh, materialOverrides)
        }
    }

    private fun writeMesh(mesh: Mesh, materialOverrides: List<FileReference>) {
        writer.open(false)

        mesh.getBounds()
        mesh.ensureNorTanUVs()

        val pos = mesh.positions!!
        val posI = createView(
            "VEC3", GL_FLOAT, pos, pos.size / 3, pos.size * 4,
            false, 0,
            min(mesh.aabb), max(mesh.aabb)
        )

        val normal = mesh.normals
        val norI = if (normal != null) createNormalsView(normal) else null

        val uv = mesh.uvs
        val uvI = if (uv != null) createUVView(uv) else null

        val color = mesh.color0
        val colorI = if (color != null) createColorView(color) else null

        writer.attr("primitives")
        writer.open(true)

        fun writeMeshAttributes() {
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
                val material = getMaterial(i)
                writeMeshHelper(helper, material, ::writeMeshAttributes)
            }
            if (ownHelpers) {
                // because they have no buffers
                mesh.helperMeshes = null
            }
        } else {
            writeMesh1(mesh.indices, getMaterial(0), ::writeMeshAttributes)
        }

        writer.close(true) // primitives[]
        writer.close(false) // mesh
    }

    private fun writeMesh1(indices: IntArray?, material: Material?, writeMeshAttributes: () -> Unit) {
        writer.open(false)
        writer.attr("mode")
        writer.write(4) // triangles

        if (material != null) {
            writer.attr("material")
            writer.write(materials.getOrPut(material) { materials.size })
        }

        if (indices != null) {
            writer.attr("indices")
            writer.write(createIndicesView(GL_UNSIGNED_INT, indices))
        }

        writeMeshAttributes()
        writer.close(false) // primitive
    }

    private fun writeMeshHelper(helper: Mesh.HelperMesh, material: Material?, writeMeshAttributes: () -> Unit) {
        writer.open(false)
        writer.attr("mode")
        writer.write(4) // triangles

        if (material != null) {
            writer.attr("material")
            writer.write(materials.getOrPut(material) { materials.size })
        }

        val indices = helper.indices
        writer.attr("indices")
        writer.write(createIndicesView(GL_UNSIGNED_INT, indices))

        writeMeshAttributes()
        writer.close(false) // primitive
    }

    private fun writeTextures() {
        write("textures", textures) { (source, sampler) ->
            writeTexture(source, sampler)
        }
    }

    private fun writeTexture(source: Int, sampler: Int) {
        writer.open(false)
        writer.attr("source")
        writer.write(source)
        writer.attr("sampler")
        writer.write(sampler)
        writer.close(false)
    }

    private fun writeAccessors() {
        writer.attr("accessors")
        writer.open(true)
        for (i in accessors.indices) {
            writeAccessor(accessors[i], i)
        }
        writer.close(true)
    }

    private fun writeAccessor(acc: Accessor, i: Int) {
        writer.open(false)
        writer.attr("bufferView")
        writer.write(i)
        writer.attr("type")
        writer.write(acc.type)
        writer.attr("componentType")
        writer.write(acc.componentType)
        writer.attr("count")
        writer.write(acc.count)
        if (acc.normalized) {
            writer.attr("normalized")
            writer.write(true)
        }
        if (acc.min != null && acc.max != null) {
            writer.attr("min")
            writer.copyRaw(acc.min)
            writer.attr("max")
            writer.copyRaw(acc.max)
        }
        writer.close(false)
    }

    private fun writeImages(dst: FileReference) {
        val dstParent = dst.getParent() ?: InvalidRef
        write("images", images) {
            writeImage(dstParent, it)
        }
    }

    private fun writeImage(dstParent: FileReference, src: FileReference) {
        writer.open(false)
        // if contains inaccessible assets, pack them, or write them to same directory
        val sameFolder = src.getParent() == dstParent
        val packed = src is InnerFile
        if ((packed && packedDepsToFolder) || (!sameFolder && allDepsToFolder)) {
            // copy the file
            val newFile = dstParent.getChild(src.name)
            newFile.writeFile(src) {}
            writeURI(newFile.absolutePath)
        } else {
            val path = src.relativePathTo(dstParent, maxNumBackPaths)
            if ((packed && packedDepsToBinary) || (!sameFolder && allDepsToBinary) || path == null) {
                appendFile(src)
            } else {
                writeURI(path)
            }
        }
        writer.close(false)
    }

    private fun writeURI(uri: String) {
        writer.attr("uri")
        writer.write(uri)
    }

    private fun appendFile(src: FileReference) {
        // "bufferView": 3,
        // "mimeType" : "image/jpeg"
        writer.attr("bufferView")
        writer.write(views.size)
        val pos0 = binary.size()
        src.inputStreamSync().copyTo(binary) // must be sync, or we'd need to unpack this loop
        val pos1 = binary.size()
        views.add(BufferView(pos0, pos1 - pos0, 0, 0))
        val ext = when (Signature.findNameSync(src)) {
            "png" -> "image/png"
            "jpg" -> "image/jpeg"
            else -> null
        }
        if (ext != null) {
            writer.attr("mimeType")
            writer.write(ext)
        }
    }

    private fun writeNodes() {
        writer.attr("nodes")
        writer.open(true)
        for ((i, node) in nodes.withIndex()) {
            writeNode(i, node)
        }
        writer.close(true)
    }

    private fun writeMeshCompAttributes(node: MeshComponent) {
        val name = node.name
        if (name.isNotEmpty()) {
            writer.attr("name")
            writer.write(name)
        }

        val mesh = node.getMeshOrNull()
        if (mesh != null) {
            writer.attr("mesh")
            writer.write(meshes.getOrPut(Pair(mesh, node.materials)) { meshes.size })
        }
    }

    private fun writeNode(i: Int, node: Any) {
        writer.open(false)

        when (node) {
            is Entity -> writeEntityAttributes(node)
            is MeshComponent -> writeMeshCompAttributes(node)
        }

        val childrenI = children.getOrNull(i)
        if (!childrenI.isNullOrEmpty()) {
            writer.attr("children")
            writer.open(true)
            for (child in childrenI) {
                writer.write(child)
            }
            writer.close(true)
        }

        writer.close(false)
    }

    private fun writeScenes() {
        writer.attr("scenes")
        writer.open(true)
        writer.open(false) // scenes[0]
        writer.attr("nodes")
        writer.open(true)
        writer.write(0) // only root nodes
        writer.close(true) // nodes
        writer.close(false) // scenes[0]
        writer.close(true) // scenes
    }

    private fun writeHeader() {
        writer.attr("asset")
        writer.open(false)
        writer.attr("generator")
        writer.write("Rem's Engine")
        writer.attr("version")
        writer.write("2.0")
        writer.close(false)
    }

    private fun writeBuffers() {
        writer.attr("buffers")
        writer.open(true) // buffers
        writer.open(false) // buffers[0]
        writer.attr("byteLength")
        writer.write(binary.size())
        writer.close(false) // buffers[0]
        writer.close(true) // buffers
    }

    private fun writeSceneIndex() {
        writer.attr("scene")
        writer.write(0)
    }

    private fun collectNodes(scene: ISaveable) {
        when (scene) {
            is Prefab -> {
                collectNodes(scene.getSampleInstance())
            }
            is Entity -> {
                scene.validateTransform()
                add(scene)
            }
            is Mesh -> {
                nodes.add(MeshComponent(scene))
                meshes[Pair(scene, emptyList())] = 0
            }
            else -> throw IllegalArgumentException("Cannot write ${scene.className}")
        }
    }

    fun write(scene: ISaveable, dst: FileReference) {

        collectNodes(scene)

        writer.open(false)

        writeHeader()
        writeSceneIndex()
        writeScenes()
        writeNodes()
        writeMeshes()
        writeMaterials()
        writeTextures()
        writeImages(dst)
        writeSamplers()
        writeBufferViews()
        writeAccessors()
        writeBuffers()

        writer.close(false)
        writer.finish()

        writeChunks(dst)
    }

    private fun ensureAlignment(bos: ByteArrayOutputStream, code: Int) {
        while (bos.size() and 3 != 0) {
            bos.write(code)
        }
    }

    private fun writeChunks(dst: FileReference) {

        ensureAlignment(json, ' '.code)
        ensureAlignment(binary, 0)

        val version = 2
        val totalFileSize = 12 + 8 + 8 + json.size() + binary.size()

        val out = dst.outputStream()
        // header
        out.writeChunkType("glTF")
        out.writeLE32(version)
        out.writeLE32(totalFileSize)
        // chunks
        out.writeChunk(json, "JSON")
        out.writeChunk(binary, "BIN${0.toChar()}")
        out.close()
    }

    private fun OutputStream.writeChunk(data: ByteArrayOutputStream, type: String) {
        writeLE32(data.size())
        writeChunkType(type)
        data.writeTo(this)
    }

    private fun OutputStream.writeChunkType(str: String) {
        assertEquals(4, str.length)
        for (char in str) {
            write(char.code)
        }
    }
}