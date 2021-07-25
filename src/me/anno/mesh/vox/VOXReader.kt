package me.anno.mesh.vox

import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.Material
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.prefab.Change
import me.anno.ecs.prefab.ChangeSetEntityAttribute
import me.anno.ecs.prefab.EntityPrefab
import me.anno.ecs.prefab.Path
import me.anno.io.files.FileReference
import me.anno.mesh.vox.format.Layer
import me.anno.mesh.vox.format.Node
import me.anno.mesh.vox.model.DenseVoxelModel
import me.anno.mesh.vox.model.VoxelModel
import org.apache.logging.log4j.LogManager
import org.joml.Vector3i
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class VOXReader {

    fun read(file: FileReference): VOXReader {
        return file.inputStream().use { read(it) }
    }

    fun read(input: InputStream): VOXReader {

        val bytes = ByteBuffer.wrap(input.readBytes()).order(ByteOrder.LITTLE_ENDIAN)

        if (bytes.int != VOX) throw IOException("Incorrect magic")
        /* val version = */ bytes.int // always 150

        readChunk(bytes)

        applyMapping()

        createMeshes()

        if (nodes.none { it.containsModel() }) {
            createDefaultNode()
        }

        return this

    }

    private fun createMeshes() {
        meshes.ensureCapacity(models.size)
        meshes.addAll(models.map { it.createMesh(palette) })
    }

    private fun createDefaultNode() {
        // there hasn't been nodes in that version yet ->
        // create default nodes
        val node = getNode(0)
        node.models = IntArray(meshes.size) { it }
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
            LOGGER.warn("index map not applied")
            println(indexMap.joinToString { (it.toInt() and 255).toString() })
        } else this.indexMap = null // done and pseudo-applied
    }

    fun toEntityPrefab(): EntityPrefab {
        val prefab = EntityPrefab()
        val changes = ArrayList<Change>()
        prefab.changes = changes
        changes.add(ChangeSetEntityAttribute(Path("name"), "Root"))
        val availableLayers = (listOf(layerNegative) + layers).filter { it.containsModel() }
        when (availableLayers.size) {
            0 -> {// awkward
            }
            1 -> {
                // don't create a layer node, when there only is a single layer
                val layer = availableLayers.first()
                for ((childIndex, node) in layer.nodes.withIndex()) {
                    node.toEntityPrefab(changes, meshes, intArrayOf(), intArrayOf(childIndex))
                }
            }
            else -> {
                for ((index, layer) in availableLayers.withIndex()) {
                    layer.toEntityPrefab(changes, meshes, index)
                }
            }
        }
        return prefab
    }

    fun toEntity(): Entity {
        val entity = Entity("Root")
        entity.add(layerNegative.toEntity(meshes, -1))
        for ((index, layer) in layers.withIndex()) {
            entity.add(layer.toEntity(meshes, index))
        }
        entity.updateTransform()
        return entity
    }


    val size = Vector3i()

    val models = ArrayList<VoxelModel>()
    val meshes = ArrayList<Mesh>()

    var palette = defaultPalette
    val materials = HashMap<Int, Material>()

    // why ever this exists...
    var indexMap: ByteArray? = null

    private val nodes = ArrayList<Node>()
    private fun getNode(id: Int): Node {
        if (id < 0) throw IndexOutOfBoundsException()
        while (nodes.size <= id) nodes.add(Node())
        return nodes[id]
    }

    private var layerNegative = Layer("Default Layer")
    private val layers = ArrayList<Layer>()
    private fun getLayer(id: Int): Layer {
        if (id < 0) return layerNegative
        while (layers.size <= id) layers.add(Layer("Layer $id"))
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
                val model = DenseVoxelModel(size.x, size.y, size.z, full)
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
                for (i in 0 until 255) {
                    palette[i + 1] = bytes.int
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
                val plastic = if (hasPlastic) {
                    bytes.float
                } else 0f
                val hasRoughness = (properties and 2) != 0
                val roughness = if (hasRoughness) {
                    bytes.float
                } else 0.5f
                val hasSpecular = (properties and 4) != 0
                val specular = if (hasSpecular) {
                    bytes.float
                } else 0.5f
                val hasIOR = (properties and 8) != 0
                val ior = if (hasIOR) {
                    bytes.float
                } else 1.333f // water
                val hasAttenuation = (properties and 16) != 0
                val attenuation = if (hasAttenuation) {
                    bytes.float
                } else 0f //??
                val hasPower = (properties and 32) != 0
                val power = if (hasPower) {
                    bytes.float
                } else 0f // ?
                val hasGlow = (properties and 64) != 0
                val glow = if (hasGlow) {
                    bytes.float
                } else 0f // ?
                val isTotalPower = (properties and 128) != 0
                materials[matIndex] = Material().apply {
                    // todo set all relevant properties
                    // todo we should set the correct shader here as well :)
                }
            }
            nTRN -> {
                // transform
                // _t = space separated coordinates
                // _r = some rotation in degrees

                val nodeId = bytes.int
                val node = getNode(nodeId)

                val properties = readDict(bytes) ?: emptyMap() // can have _name
                node.name = properties["_name"] ?: node.name

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
                        val frames = Array(numFrames) { readDict(bytes) }
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
            IMAP -> indexMap = ByteArray(256) { bytes.get() }
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

    private fun decodeFrame(node: Node, frame: HashMap<String, String>?) {
        frame ?: return
        val translation = frame["_t"]?.split(' ')?.map { it.toDouble() } // typically just ints
        if (translation != null) {
            node.px = +translation[0]
            node.pz = -translation[1]
            node.py = +translation[2]
        }
        node.ry = frame["_r"]?.toDoubleOrNull() ?: 0.0
    }

    /**
     * skips over a dictionary
     * this skips a lot of string allocations
     * can be used, when there is no interesting data anyways
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
        return String(ByteArray(size) { bytes.get() })
    }

    companion object {

        private val LOGGER = LogManager.getLogger(VOXReader::class)

        fun encode(str: String) = str.mapIndexed { index, it -> it.code.shl(index * 8) }.sum()

        // old stuff
        val VOX = encode("VOX ")
        val MAIN = encode("MAIN")
        val PACK = encode("PACK")
        val SIZE = encode("SIZE")
        val BLOCK_DATA = encode("XYZI")
        val RGBA = encode("RGBA")
        val MATv1 = encode("MATT")

        // new stuff
        // https://github.com/ephtracy/voxel-model/issues/19
        val MATv2 = encode("MATL")
        val nSHP = encode("nSHP")
        val nTRN = encode("nTRN")
        val nGRP = encode("nGRP")
        val LAYER = encode("LAYR")
        val rOBJ = encode("rOBJ")
        val IMAP = encode("IMAP")

        // we could store it in a large list, but this algorithm probably is shorter
        val defaultPalette: IntArray

        init {

            val defaultPalette0 = IntArray(256)
            var i = 1

            // {ff,cc,99,66,33,00}³
            for (b in 0 until 6) {
                val bv = (15 - b * 3)
                for (g in 0 until 6) {
                    val gv = bv + (15 - g * 3).shl(8)
                    for (r in 0 until 6) {
                        val rv = gv + (15 - r * 3).shl(16)
                        defaultPalette0[i++] = rv * 0x11
                    }
                }
            }

            // black comes last
            i--

            // extra tones with just one channel none-black
            for (channel in 0 until 4) {
                val mul = if (channel != 3) 0x11 shl (channel * 8) else 0x111111
                for (value in 14 downTo 1) {
                    if (value % 3 != 0) {
                        defaultPalette0[i++] = value * mul
                    }
                }
            }

            defaultPalette = defaultPalette0

        }

        @JvmStatic
        fun main(args: Array<String>) {
            // extremely complex:
            // val file = OS.downloads.getChild("MagicaVoxel/vox/PrinceOfPersia.vox")
            // medium:
            // val file = OS.downloads.getChild("MagicaVoxel/vox/truck.vox")
            // VOXReader().read(file)
            /*writeImageInt(6, 256 / 6, false, "vox.png", 256) { _, _, i ->
                defaultPalette[i + 1]
            }*/
        }

    }


}