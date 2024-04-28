package me.anno.mesh.gltf

import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.MeshComponentBase
import me.anno.ecs.components.mesh.material.Material
import me.anno.ecs.prefab.Prefab
import me.anno.gpu.pipeline.Pipeline
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.Filtering
import me.anno.io.Saveable
import me.anno.io.Streams.writeLE32
import me.anno.io.files.FileReference
import me.anno.io.files.Signature
import me.anno.io.files.inner.InnerFile
import me.anno.io.json.generic.JsonWriter
import me.anno.maths.Maths.clamp
import me.anno.utils.Color.b
import me.anno.utils.Color.black3
import me.anno.utils.Color.g
import me.anno.utils.Color.r
import me.anno.utils.Color.white4
import me.anno.utils.structures.tuples.IntPair
import org.apache.logging.log4j.LogManager
import org.joml.AABBf
import org.joml.Quaterniond
import org.joml.Vector3d
import org.joml.Vector3f
import org.joml.Vector4f
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import kotlin.test.assertEquals

/**
 * writes a GLTF file from an Entity or Mesh,
 * writes materials and textures, too
 *
 * todo support skeletons and animations
 * todo support cameras
 * todo support lights
 * */
class GLTFWriter(
    val allDepsToFolder: Boolean = false,
    val packedDepsToFolder: Boolean = false,
    val allDepsToBinary: Boolean = false,
    val packedDepsToBinary: Boolean = true,
    val maxNumBackPaths: Int = 0,
    val json: ByteArrayOutputStream = ByteArrayOutputStream(1024)
) : JsonWriter(json) {

    companion object {
        private val LOGGER = LogManager.getLogger(GLTFWriter::class)
        private const val GL_FLOAT = 0x1406
        private const val GL_UNSIGNED_BYTE = 0x1401
        private const val GL_UNSIGNED_INT = 0x1405
    }

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

    private fun copyRaw(v: String) {
        // raw copy
        next()
        output.write(v.toByteArray())
    }

    private val textures = HashMap<IntPair, Int>() // source, sampler
    private val images = HashMap<FileReference, Int>() // uris
    private val samplers = HashMap<Pair<Filtering, Clamping>, Int>()
    private val materials = HashMap<Material, Int>()
    private val meshes = HashMap<Pair<Mesh, List<FileReference>>, Int>()

    private val binary = ByteArrayOutputStream(4096)

    private fun countMeshes(entity: Entity): Int {
        return entity.components.count {
            it is MeshComponentBase && it.getMesh() != null
        }
    }

    private val nodes = ArrayList<Saveable>()
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
                .filter { it.getMesh() != null }
                .forEach {
                    val idx2 = nodes.size
                    nodes.add(it)
                    children2.add(idx2)
                }
        } else children.add(emptyList())
        return idx
    }

    private fun <K> writeArray(name: String, map: Map<K, Int>, write: (K) -> Unit) {
        if (map.isNotEmpty()) {
            attr(name)
            writeArray {
                for ((k, _) in map.entries.sortedBy { it.value }) {
                    write(k)
                }
            }
        }
    }

    private val views = ArrayList<BufferView>()
    private val accessors = ArrayList<Accessor>()

    private fun createPositionsView(positions: FloatArray, bounds: AABBf): Int {
        val start = binary.size()
        for (v in positions) {
            // NaN is not supported
            binary.writeLE32(if (v.isNaN()) 0 else v.toRawBits())
        }
        accessors.add(Accessor("VEC3", GL_FLOAT, positions.size / 3, false, min(bounds), max(bounds)))
        views.add(BufferView(start, positions.size * 4, 34962, 0))
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

    private fun createIndicesView(data: IntArray): Int {
        val pos = binary.size()
        for (i in data.indices) {
            binary.writeLE32(data[i])
        }
        accessors.add(Accessor("SCALAR", GL_UNSIGNED_INT, data.size, false))
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
        writeArray("samplers", samplers) { (filtering, clamping) ->
            writeSampler(filtering, clamping)
        }
    }

    private fun writeSampler(filtering: Filtering, clamping: Clamping) {
        writeObject {
            attr("magFilter")
            write(filtering.mag)
            attr("minFilter")
            write(filtering.min)
            attr("wrapS")
            write(clamping.mode)
            attr("wrapT")
            write(clamping.mode)
        }
    }

    private fun writeBufferViews() {
        attr("bufferViews")
        writeArray(views, ::writeBufferView)
    }

    private fun writeBufferView(bufferView: BufferView) {
        writeObject {
            attr("buffer")
            write(0)
            attr("byteOffset")
            write(bufferView.offset)
            attr("byteLength")
            write(bufferView.length)
            if (bufferView.byteStride != 0) {
                attr("byteStride")
                write(bufferView.byteStride)
            }
            if (bufferView.target != 0) {
                attr("target")
                write(bufferView.target)
            }
        }
    }

    private fun writeEntityAttributes(node: Entity) {
        if (countMeshes(node) == 1) {
            val mesh = node.components
                .filterIsInstance<MeshComponentBase>()
                .firstNotNullOfOrNull {
                    val mesh = it.getMesh() as? Mesh
                    if (mesh != null) Pair(mesh, it.materials) else null
                }
            if (mesh != null) {
                attr("mesh")
                write(meshes.getOrPut(mesh) { meshes.size })
            }
        }

        val name = node.name
        if (name.isNotEmpty()) {
            attr("name")
            write(name)
        }

        val translation = node.transform.localPosition
        if (translation != Vector3d()) {
            attr("translation")
            write(translation)
        }

        val rotation = node.transform.localRotation
        if (rotation != Quaterniond()) {
            attr("rotation")
            write(rotation)
        }

        val scale = node.transform.localScale
        if (scale.x != 1.0 || scale.y != 1.0 || scale.z != 1.0) {
            attr("scale")
            write(scale)
        }
    }

    private fun writeMaterials() {
        writeArray("materials", materials) { material ->
            writeMaterial(material)
        }
    }

    private fun writePbrMetallicRoughness(material: Material, sampler: Int) {
        writeObject {
            if (material.diffuseMap.exists) {
                attr("baseColorTexture")
                writeObject {
                    attr("index")
                    write(getTextureIndex(material.diffuseMap, sampler))
                }
            }
            val color = material.diffuseBase
            if (color != white4) {
                attr("baseColorFactor")
                val color1 = if (color.x in 0f..1f && color.y in 0f..1f && color.z in 0f..1f && color.w in 0f..1f) {
                    material.diffuseBase
                } else {
                    Vector4f(clamp(color.x), clamp(color.y), clamp(color.z), clamp(color.w))
                }
                write(color1)
            }
            attr("metallicFactor")
            write(material.metallicMinMax.y)
            attr("roughnessFactor")
            write(material.roughnessMinMax.y)
        }
    }

    private fun findSampler(material: Material): Int {
        return if (
            material.emissiveMap.exists ||
            material.diffuseMap.exists ||
            material.normalMap.exists ||
            material.occlusionMap.exists
        ) {
            samplers.getOrPut(
                Pair(
                    if (material.linearFiltering) Filtering.TRULY_LINEAR
                    else Filtering.TRULY_NEAREST,
                    material.clamping
                )
            ) { samplers.size }
        } else -1
    }

    private fun writeMaterial(material: Material) {
        // https://github.com/KhronosGroup/glTF/blob/main/specification/2.0/schema/material.schema.json
        writeObject {
            val sampler = findSampler(material)
            attr("pbrMetallicRoughness")
            writePbrMetallicRoughness(material, sampler)
            if (material.isDoubleSided) {
                attr("doubleSided")
                write(true)
            }
            writeTextureProperty(material.emissiveMap, "emissiveTexture", sampler)
            if (material.emissiveBase != black3) {
                attr("emissiveFactor")
                write(material.emissiveBase)
            }
            writeTextureProperty(material.normalMap, "normalTexture", sampler)
            writeTextureProperty(material.occlusionMap, "occlusionTexture", sampler)
        }
    }

    private fun writeTextureProperty(texture: FileReference, attrName: String, sampler: Int) {
        if (texture.exists) {
            attr(attrName)
            writeObject {
                attr("index")
                write(getTextureIndex(texture, sampler))
            }
        }
    }

    private fun writeMeshes() {
        writeArray("meshes", meshes) { (mesh, materialOverrides) ->
            writeMesh(mesh, materialOverrides)
        }
    }

    private fun writeMesh(mesh: Mesh, materialOverrides: List<FileReference>) {
        writeObject {

            mesh.ensureNorTanUVs()

            val pos = mesh.positions!!
            val bounds = mesh.getBounds()
            val posI = createPositionsView(pos, bounds)

            val normal = mesh.normals
            val norI = if (normal != null) createNormalsView(normal) else null

            val uv = mesh.uvs
            val uvI = if (uv != null) createUVView(uv) else null

            val color = mesh.color0
            val colorI = if (color != null) createColorView(color) else null

            attr("primitives")
            writeArray {
                fun writeMeshAttributes() {
                    attr("attributes")
                    writeObject {

                        attr("POSITION")
                        write(posI)

                        if (norI != null) {
                            attr("NORMAL")
                            write(norI)
                        }

                        if (uvI != null) {
                            attr("TEXCOORD_0")
                            write(uvI)
                        }

                        if (colorI != null) {
                            attr("COLOR_0")
                            write(colorI)
                        }

                        // todo skinning and animation support
                    }
                }

                // for each material add a primitive
                val matIds = mesh.materialIds
                val ownHelpers = mesh.helperMeshes == null
                val helpers = if (matIds != null) {
                    if (ownHelpers) mesh.createHelperMeshes(matIds, false)
                    mesh.helperMeshes
                } else null

                fun getMaterial(i: Int): Material {
                    return Pipeline.getMaterial(materialOverrides, mesh.materials, i)
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
            }
        }
    }

    private fun writeMesh1(indices: IntArray?, material: Material, writeMeshAttributes: () -> Unit) {
        writeObject {
            attr("mode")
            write(4) // triangles

            attr("material")
            write(materials.getOrPut(material) { materials.size })

            if (indices != null) {
                attr("indices")
                write(createIndicesView(indices))
            }

            writeMeshAttributes()
        }
    }

    private fun writeMeshHelper(
        helper: Mesh.HelperMesh,
        material: Material?,
        writeMeshAttributes: () -> Unit
    ) {
        writeObject {
            attr("mode")
            write(4) // triangles

            if (material != null) {
                attr("material")
                write(materials.getOrPut(material) { materials.size })
            }

            val indices = helper.indices
            attr("indices")
            write(createIndicesView(indices))

            writeMeshAttributes()
        }
    }

    private fun writeTextures() {
        writeArray("textures", textures) { (source, sampler) ->
            writeTexture(source, sampler)
        }
    }

    private fun writeTexture(source: Int, sampler: Int) {
        writeObject {
            attr("source")
            write(source)
            attr("sampler")
            write(sampler)
        }
    }

    private fun writeAccessors() {
        attr("accessors")
        writeArrayIndexed(accessors, ::writeAccessor)
    }

    private fun writeAccessor(i: Int, acc: Accessor) {
        writeObject {
            attr("bufferView")
            write(i)
            attr("type")
            write(acc.type)
            attr("componentType")
            write(acc.componentType)
            attr("count")
            write(acc.count)
            if (acc.normalized) {
                attr("normalized")
                write(true)
            }
            if (acc.min != null && acc.max != null) {
                attr("min")
                copyRaw(acc.min)
                attr("max")
                copyRaw(acc.max)
            }
        }
    }

    private fun writeImages(dst: FileReference) {
        val dstParent = dst.getParent()
        writeArray("images", images) {
            writeImage(dstParent, it)
        }
    }

    private fun writeImage(dstParent: FileReference, src: FileReference) {
        writeObject {
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
        }
    }

    private fun writeURI(uri: String) {
        attr("uri")
        write(uri)
    }

    private fun appendFile(src: FileReference) {
        // "bufferView": 3,
        // "mimeType" : "image/jpeg"
        attr("bufferView")
        write(views.size)
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
            attr("mimeType")
            write(ext)
        }
    }

    private fun writeMeshCompAttributes(node: MeshComponent) {
        val name = node.name
        if (name.isNotEmpty()) {
            attr("name")
            write(name)
        }
        val mesh = node.getMesh()
        if (mesh != null) {
            attr("mesh")
            write(meshes.getOrPut(Pair(mesh, node.materials)) { meshes.size })
        }
    }

    private fun writeNodes() {
        attr("nodes")
        writeArrayIndexed(nodes, ::writeNode)
    }

    private fun writeNode(i: Int, node: Any) {
        writeObject {
            when (node) {
                is Entity -> writeEntityAttributes(node)
                is MeshComponent -> writeMeshCompAttributes(node)
                else -> LOGGER.warn("Unknown node type $node")
            }

            val childrenI = children.getOrNull(i)
            if (!childrenI.isNullOrEmpty()) {
                attr("children")
                writeArray(childrenI, ::write)
            }
        }
    }

    private fun writeScenes() {
        attr("scenes")
        writeArray {
            writeObject {  // scenes[0]
                attr("nodes")
                writeArray {
                    write(0) // only root nodes
                }
            }
        }
    }

    private fun writeHeader() {
        attr("asset")
        writeObject {
            attr("generator")
            write("Rem's Engine")
            attr("version")
            write("2.0")
        }
    }

    private fun writeBuffers() {
        attr("buffers")
        writeArray {
            writeObject {
                attr("byteLength")
                write(binary.size())
            }
        }
    }

    private fun writeSceneIndex() {
        attr("scene")
        write(0)
    }

    private fun collectNodes(scene: Saveable) {
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

    fun write(scene: Saveable, dst: FileReference) {
        collectNodes(scene)
        writeObject {
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
        }
        finish()
        writeChunks(dst)
    }

    private fun ensureAlignment(bos: ByteArrayOutputStream, paddingByte: Int) {
        while (bos.size().and(3) != 0) {
            bos.write(paddingByte)
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
        for (i in 0 until 4) {
            write(str[i].code)
        }
    }
}