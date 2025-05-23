package me.anno.mesh.vox

import me.anno.ecs.components.mesh.material.Material
import me.anno.ecs.prefab.Prefab
import me.anno.ecs.prefab.PrefabReadable
import me.anno.ecs.prefab.change.Path
import me.anno.io.SignatureUtils.le32Signature
import me.anno.io.files.FileReference
import me.anno.io.files.inner.InnerFolder
import me.anno.mesh.vox.VoxPalette.defaultPalette
import me.anno.mesh.vox.model.DenseI8VoxelModel
import me.anno.mesh.vox.model.VoxelModel
import me.anno.utils.Color.convertABGR2ARGB
import me.anno.utils.async.Callback
import me.anno.utils.structures.lists.Lists.createArrayList
import me.anno.utils.types.Ints.toIntOrDefault
import me.anno.utils.types.Strings.isBlank2
import org.apache.logging.log4j.LogManager
import org.joml.Vector2f
import org.joml.Vector3i
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.max

class VOXReader {

    private var idCtr = 0
    private fun nextId(): String {
        return "#${idCtr++}"
    }

    fun read(bytes: ByteBuffer, callback: Callback<VOXReader>) {

        //val t0 = Time.nanoTime
        bytes.order(ByteOrder.LITTLE_ENDIAN)
        bytes.position(0)

        if (bytes.capacity() < 8) callback.err(IOException("VOXFile is too small"))
        if (bytes.int != VOX) callback.err(IOException("Incorrect magic"))
        /* val version = */ bytes.int // always 150

        readChunk(bytes)

        applyMapping()

        if (nodes.none { it.containsModel() }) {
            createDefaultNode()
        }

        // 3 ms for the file in question
        // val t1 = Time.nanoTime
        // LOGGER.info("Used ${(t1-t0)*1e-9}s to read vox file")

        callback.ok(this)
    }

    private fun createDefaultNode() {
        // there hasn't been nodes in that version yet ->
        // create default nodes
        val node = getNode(0)
        node.models = IntArray(models.size) { it }
        layerNegative.nodes.add(node)
    }

    private fun applyMapping() {
        val indexMap = indexMap ?: return
        // default index map: 1..255,0
        var isDefault = true
        for (i in 0 until 256) {
            if (indexMap[i] != (i + 1).toByte()) {
                // LOGGER.warn("difference: ${indexMap[i].toInt() and 255} vs ${i+1}")
                isDefault = false
                break
            }
        }
        if (!isDefault) {
            // todo function to apply index mapping
            // val oldMaterials = materials
            LOGGER.warn("index map not applied")
            LOGGER.info(indexMap.joinToString { (it.toInt() and 255).toString() })
        } else this.indexMap = null // done and pseudo-applied
    }

    fun toEntityPrefab(meshPaths: List<FileReference>): Prefab {
        val prefab = Prefab("Entity")
        prefab.setUnsafe(Path.ROOT_PATH, "name", "Root")
        val availableLayers = (listOf(layerNegative) + layers).filter { it.containsModel() }
        when (availableLayers.size) {
            0 -> {// awkward
            }
            1 -> {
                // don't create a layer node, when there only is a single layer
                val layer = availableLayers.first()
                for ((index, node) in layer.nodes.withIndex()) {
                    node.toEntityPrefab(prefab, meshPaths, Path.ROOT_PATH, index)
                }
            }
            else -> {
                for ((index, layer) in availableLayers.withIndex()) {
                    layer.toEntityPrefab(prefab, meshPaths, index)
                }
            }
        }
        return prefab
    }

    val size = Vector3i()

    val models = ArrayList<VoxelModel>()

    var palette = defaultPalette
    val materials = ArrayList<FileReference>()

    // why ever this exists...
    var indexMap: ByteArray? = null

    private val nodes = ArrayList<VOXNode>()
    private fun getNode(id: Int): VOXNode {
        val index = max(id, 0) // make this at least a little crash-safe
        // (still can crash with OOM, if id is really large)
        while (nodes.size <= index) nodes.add(VOXNode())
        return nodes[index]
    }

    private var layerNegative = VOXLayer("Default Layer")
    private val layers = ArrayList<VOXLayer>()
    private fun getLayer(id: Int): VOXLayer {
        if (id < 0) return layerNegative
        while (layers.size <= id) layers.add(VOXLayer("Layer $id"))
        return layers[id]
    }

    private fun readChunk(bytes: ByteBuffer) {
        val id = bytes.int
        val contentSize = bytes.int
        // only the main node has children
        val childrenBytes = bytes.int
        val position = bytes.position()
        when (id) {
            MAIN -> {
                // just contains all other chunks
            }
            SIZE -> {
                size.x = bytes.int
                size.z = bytes.int
                size.y = bytes.int
            }
            PACK -> {
                val numModels = bytes.int
                models.ensureCapacity(numModels)
            }
            BLOCK_DATA -> {
                val numVoxels = bytes.int
                val fullVolume = size.x * size.y * size.z
                // val density = numVoxels.toFloat() / fullVolume
                // val useSparse = density < 0.1f && numVoxels < 32
                // we could use an oct-tree
                val full = ByteArray(fullVolume)
                val model = DenseI8VoxelModel(size.x, size.y, size.z, full)
                val dz = size.z - 1 // mirror z
                for (i in 0 until numVoxels) {
                    val x = bytes.get().toInt() and 255
                    val z = dz - (bytes.get().toInt() and 255)
                    val y = bytes.get().toInt() and 255
                    val index = bytes.get()
                    full[model.getIndex(x, y, z)] = index
                }
                models.add(model)
                LOGGER.debug("Loaded model of size ${size.x},${size.y},${size.z} with $numVoxels voxels")
            }
            RGBA -> {
                palette = IntArray(256)
                // color 0 is always transparent black
                for (i in 1 until 256) {// data seems to be ABGR
                    palette[i] = convertABGR2ARGB(bytes.int)
                }
            }
            MATv1 -> {
                val matIndex = bytes.int
                // 0 = diffuse, 1 = metal, 2 = glass, 3 = emissive
                val type = bytes.int
                // diffuse: no meaning, metal: how metal, glass: how glass, emissive: how emissive, (0,1]
                val weight = bytes.float
                val properties = bytes.int
                val hasPlastic = (properties and 1) != 0
                val plastic = if (hasPlastic) bytes.float else 0f
                val hasRoughness = (properties and 2) != 0
                val roughness = if (hasRoughness) bytes.float else 0.5f
                val hasSpecular = (properties and 4) != 0
                val specular = if (hasSpecular) bytes.float else 0.5f
                val hasIOR = (properties and 8) != 0
                val ior = if (hasIOR) bytes.float else 1.333f // water
                val hasAttenuation = (properties and 16) != 0
                val attenuation = if (hasAttenuation) bytes.float else 0f //??
                val hasPower = (properties and 32) != 0
                val power = if (hasPower) bytes.float else 0f // ?
                val hasGlow = (properties and 64) != 0
                val glow = if (hasGlow) bytes.float else 0f // ?
                val isTotalPower = (properties and 128) != 0
                while (matIndex > materials.size) {
                    materials.add(Material().ref)
                }
                val material = (materials[matIndex] as PrefabReadable).readPrefab()
                material[Path.ROOT_PATH, "roughnessMinMax"] = Vector2f(0f, roughness)
                if (type == 1) material[Path.ROOT_PATH, "metallicMinMax"] = Vector2f(0f, weight)
                if (hasIOR) material["indexOfRefraction"] = ior
                // todo set all relevant properties
                // todo we should set the correct shader here as well :)
            }
            nTRN -> {
                // transform
                // _t = space separated coordinates
                // _r = rotation enum (an integer)

                val nodeId = bytes.int
                val node = getNode(nodeId)

                val properties = readDict(bytes) ?: emptyMap() // can have _name
                var name = properties["_name"] ?: node.name
                if (name.isBlank2()) name = nextId()
                node.name = name

                val childNodeId = bytes.int
                node.child = getNode(childNodeId)

                /*val reserved = */bytes.int
                val layerId = bytes.int
                getLayer(layerId).nodes.add(node)

                when (val numFrames = bytes.int) {
                    0 -> {
                    }
                    1 -> {
                        val frame = readDict(bytes)
                        decodeFrame(node, frame)
                        // LOGGER.debug("nTRN $nodeId $properties, child $childNodeId on layer $layerId with frame $frame")
                    }
                    else -> {
                        // todo these frames define an animation -> would should load it somehow...
                        // todo maybe an animation data attribute/component? :)
                        val frames = createArrayList(numFrames) { readDict(bytes) }
                        decodeFrame(node, frames[0])
                        LOGGER.warn("Todo multiple frames")
                    }
                }
            }
            nGRP -> {
                // group, parent-children relation of nodes
                val nodeId = bytes.int
                val node = getNode(nodeId)
                // haven't had interesting properties
                /*val properties =*/ skipDict(bytes)
                val childrenCount = bytes.int
                val children = IntArray(childrenCount) { bytes.int }
                node.children = children.map { getNode(it) }
                // LOGGER.debug("nGRP $nodeId $properties with children ${children.joinToString()}")
            }
            nSHP -> {
                // a model is assigned to a node
                val nodeId = bytes.int
                val node = getNode(nodeId)
                /*val properties = */ skipDict(bytes)
                val numModels = bytes.int
                if (numModels > 0) {
                    val models = IntArray(numModels) {
                        val modelId = bytes.int
                        // haven't had interesting properties
                        skipDict(bytes)
                        modelId
                    }
                    node.models = models
                }
                // LOGGER.debug("nSHP $nodeId $properties with models ${node.models?.joinToString()}")
            }
            LAYER -> {
                val layerId = bytes.int
                // _name = the name of the layer
                val properties = readDict(bytes)
                if (properties != null) {
                    val layer = getLayer(layerId)
                    properties["_name"]?.run { layer.name = this }
                    // LOGGER.debug("Layer $layerId $properties")
                }
                // val unknown = bytes.int
            }
            MATv2 -> {
                // val matId = bytes.int
                // val properties = readDict(bytes)
                // seems to have very similar properties to v1:
                // _plastic, _ior, _flux, _weight, _ldr, _type, _spec, _rough, _att
                // todo parse the material and apply it
                // LOGGER.info("Material $matId $properties")
            }
            rOBJ -> {
                // scene information, _type, e.g. background & ground color, camera fov, bloom, edge thickness, grid settings
                // val properties = skipDict(bytes)
                // LOGGER.debug("Scene Information: $properties")
            }
            rCAM -> {} // camera infos
            NOTE -> {} // notes?
            IMAP -> {
                val indexMap = ByteArray(256)
                bytes.get(indexMap)
                this.indexMap = indexMap
            }
            else -> {
                val idName = (0..3).joinToString("") { ((id shr it * 8) and 255).toChar().toString() }
                LOGGER.warn("Unknown id $idName with $contentSize bytes content and $childrenBytes bytes of children")
            }
        }
        bytes.position(position + contentSize)
        val endOfChunk = position + contentSize + childrenBytes
        val minChunkSize = 12
        while (bytes.position() + minChunkSize <= endOfChunk) {
            readChunk(bytes)
        }
        bytes.position(position + contentSize + childrenBytes)
    }

    private fun decodeFrame(node: VOXNode, frame: HashMap<String, String>?) {
        frame ?: return
        val translation = frame["_t"]?.split(' ')?.map { it.toDouble() } // typically just integers
        if (translation != null) {
            node.px = +translation[0]
            node.pz = -translation[1]
            node.py = +translation[2]
        }
        node.rotation = frame["_r"].toIntOrDefault(node.rotation)
    }

    /**
     * skips over a dictionary
     * this skips a lot of string allocations
     * can be used, when there is no interesting data anyway
     * */
    private fun skipDict(bytes: ByteBuffer) {
        val entries = bytes.int
        for (i in 0 until entries * 2) {
            skipDictString(bytes)
        }
    }

    private fun readDict(bytes: ByteBuffer): HashMap<String, String>? {
        val entries = bytes.int
        if (entries <= 0) return null
        val map = HashMap<String, String>(entries)
        for (i in 0 until entries) {
            val key = readDictString(bytes)
            val value = readDictString(bytes)
            map[key] = value
        }
        return map
    }

    private fun skipDictString(bytes: ByteBuffer) {
        val size = bytes.int
        bytes.position(bytes.position() + size)
    }

    private fun readDictString(bytes: ByteBuffer): String {
        val size = bytes.int
        val tmp = ByteArray(size)
        bytes.get(tmp)
        return tmp.decodeToString()
    }

    companion object {

        fun readAsFolder(src: FileReference, callback: Callback<InnerFolder>) {
            src.readByteBuffer(false) { bytes, exc ->
                if (bytes != null) {
                    VOXReader().read(bytes) { reader, err ->
                        if (err != null) callback.err(err)
                        else callback.ok(readAsFolder(reader!!, src))
                    }
                } else callback.err(exc)
            }
        }

        private fun readAsFolder(reader: VOXReader, src: FileReference): InnerFolder {
            val folder = InnerFolder(src)
            val meshes = InnerFolder(folder, "meshes")
            val meshReferences = reader.models.mapIndexed { index, mesh ->
                val prefab = mesh.createMeshPrefab(reader.palette)
                val meshFile = meshes.createPrefabChild("$index.json", prefab)
                prefab.sourceFile = meshFile
                meshFile
            }
            val prefab = reader.toEntityPrefab(meshReferences)
            val layersRoot = folder.createPrefabChild("Scene.json", prefab)
            prefab.sourceFile = layersRoot
            folder.sealPrefabs()
            return folder
        }

        private val LOGGER = LogManager.getLogger(VOXReader::class)

        // old stuff
        private val VOX = le32Signature("VOX ")
        private val MAIN = le32Signature("MAIN")
        private val PACK = le32Signature("PACK")
        private val SIZE = le32Signature("SIZE")
        private val BLOCK_DATA = le32Signature("XYZI")
        private val RGBA = le32Signature("RGBA")
        private val MATv1 = le32Signature("MATT")

        // new stuff
        // https://github.com/ephtracy/voxel-model/issues/19
        private val MATv2 = le32Signature("MATL")
        private val nSHP = le32Signature("nSHP")
        private val nTRN = le32Signature("nTRN")
        private val nGRP = le32Signature("nGRP")
        private val LAYER = le32Signature("LAYR")
        private val rOBJ = le32Signature("rOBJ")
        private val rCAM = le32Signature("rCAM")
        private val NOTE = le32Signature("NOTE")
        private val IMAP = le32Signature("IMAP")
    }
}