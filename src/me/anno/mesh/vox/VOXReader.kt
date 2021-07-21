package me.anno.mesh.vox

import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.Material
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.MeshRenderer
import me.anno.io.files.FileReference
import me.anno.utils.OS
import org.apache.logging.log4j.LogManager
import org.joml.Vector3d
import org.joml.Vector3i
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class VOXReader {

    fun read(file: FileReference) {
        file.inputStream().use { read(it) }
    }

    fun read(input: InputStream) {
        val bytes = ByteBuffer.wrap(input.readBytes()).order(ByteOrder.LITTLE_ENDIAN)
        if (bytes.int != VOX) throw IOException("Incorrect magic")
        /* val version = */ bytes.int // always 150
        readChunk(bytes)
        if (indexMap != null) {
            // todo function to apply index mapping to remove the need for it in further calls
            LOGGER.warn("index map not applied")
        }
        meshes.ensureCapacity(models.size)
        meshes.addAll(models.map { it.createMesh(palette) })
        if (nodes.isEmpty() && layers.isEmpty()) {
            // there hasn't been nodes in that version yet ->
            // create default nodes
            val node = getNode(0)
            node.models = IntArray(meshes.size) { it }
        }
    }

    // todo function toEntityPrefab

    fun toEntity(): Entity {
        val entity = Entity("Root")
        entity.add(toEntity(layerNegative, -1))
        for ((index, layer) in layers.withIndex()) {
            entity.add(toEntity(layer, index))
        }
        entity.updateTransform()
        return entity
    }

    fun toEntity(layer: Layer, index: Int): Entity {
        val entity = Entity(layer.name.ifEmpty { "Layer $index" })
        for (node in layer.nodes) {
            val child = toEntity(node)
            entity.add(child)
            child.transform.update(entity.transform)
        }
        return entity
    }

    fun toEntity(node: Node): Entity {
        val entity = Entity(node.name)
        val models = node.child?.models
        if (models != null) {
            // add these models as components
            for (modelIndex in models) {
                val mesh = meshes[modelIndex]
                entity.add(MeshComponent(mesh))
            }
            entity.add(MeshRenderer())
        }
        if (node.px != 0.0 || node.py != 0.0 || node.pz != 0.0) {
            entity.transform.localPosition = Vector3d(node.px, node.py, node.pz)
            // rotation is round, but looks wrong in the price of persia sample
        }
        node.children?.forEach {
            entity.add(toEntity(it))
        }
        return entity
    }

    val size = Vector3i()

    val models = ArrayList<VoxelModel>()
    val meshes = ArrayList<Mesh>()

    var palette = defaultPalette
    val materials = HashMap<Int, Material>()

    // why ever this exists...
    var indexMap: ByteArray? = null

    class Node {
        var name: String = ""
        var child: Node? = null
        var children: List<Node>? = null
        var models: IntArray? = null
        var px = 0.0
        var py = 0.0
        var pz = 0.0
        var ry = 0.0
    }

    val nodes = ArrayList<Node>()

    fun getNode(id: Int): Node {
        if (id < 0) throw IndexOutOfBoundsException()
        while (nodes.size <= id) nodes.add(Node())
        return nodes[id]
    }

    class Layer {
        var name: String = ""
        val nodes = ArrayList<Node>()
    }

    var layerNegative = Layer()
    val layers = ArrayList<Layer>()

    fun getLayer(id: Int): Layer {
        if (id < 0) return layerNegative
        while (layers.size <= id) layers.add(Layer())
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
                LOGGER.debug("Loaded model of size ${size.x},${size.y},${size.z}")
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
                val properties = readDict(bytes)
                val numModels = bytes.int
                if (numModels > 0) {
                    val models = IntArray(numModels) {
                        val modelId = bytes.int
                        // haven't had interesting properties
                        /*val properties2 = */ skipDict(bytes)
                        modelId// to properties2
                    }
                    node.models = models
                }
                // LOGGER.debug("nSHP $nodeId $properties with models ${node.models?.joinToString()}")
            }
            LAYER -> {
                val layerId = bytes.int
                // _name = the name of the layer
                val properties = readDict(bytes) ?: emptyMap()
                /*val unknown = */bytes.int
                val layer = getLayer(layerId)
                layer.name = properties["_name"] ?: layer.name
                // LOGGER.debug("Layer $layerId $properties")
            }
            MATv2 -> {
                val matId = bytes.int
                val properties = readDict(bytes)
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

        val defaultPalette = longArrayOf(
            0x00000000, 0xffffffff, 0xffccffff, 0xff99ffff, 0xff66ffff, 0xff33ffff, 0xff00ffff, 0xffffccff,
            0xffccccff, 0xff99ccff, 0xff66ccff, 0xff33ccff, 0xff00ccff, 0xffff99ff, 0xffcc99ff, 0xff9999ff,
            0xff6699ff, 0xff3399ff, 0xff0099ff, 0xffff66ff, 0xffcc66ff, 0xff9966ff, 0xff6666ff, 0xff3366ff,
            0xff0066ff, 0xffff33ff, 0xffcc33ff, 0xff9933ff, 0xff6633ff, 0xff3333ff, 0xff0033ff, 0xffff00ff,
            0xffcc00ff, 0xff9900ff, 0xff6600ff, 0xff3300ff, 0xff0000ff, 0xffffffcc, 0xffccffcc, 0xff99ffcc,
            0xff66ffcc, 0xff33ffcc, 0xff00ffcc, 0xffffcccc, 0xffcccccc, 0xff99cccc, 0xff66cccc, 0xff33cccc,
            0xff00cccc, 0xffff99cc, 0xffcc99cc, 0xff9999cc, 0xff6699cc, 0xff3399cc, 0xff0099cc, 0xffff66cc,
            0xffcc66cc, 0xff9966cc, 0xff6666cc, 0xff3366cc, 0xff0066cc, 0xffff33cc, 0xffcc33cc, 0xff9933cc,
            0xff6633cc, 0xff3333cc, 0xff0033cc, 0xffff00cc, 0xffcc00cc, 0xff9900cc, 0xff6600cc, 0xff3300cc,
            0xff0000cc, 0xffffff99, 0xffccff99, 0xff99ff99, 0xff66ff99, 0xff33ff99, 0xff00ff99, 0xffffcc99,
            0xffcccc99, 0xff99cc99, 0xff66cc99, 0xff33cc99, 0xff00cc99, 0xffff9999, 0xffcc9999, 0xff999999,
            0xff669999, 0xff339999, 0xff009999, 0xffff6699, 0xffcc6699, 0xff996699, 0xff666699, 0xff336699,
            0xff006699, 0xffff3399, 0xffcc3399, 0xff993399, 0xff663399, 0xff333399, 0xff003399, 0xffff0099,
            0xffcc0099, 0xff990099, 0xff660099, 0xff330099, 0xff000099, 0xffffff66, 0xffccff66, 0xff99ff66,
            0xff66ff66, 0xff33ff66, 0xff00ff66, 0xffffcc66, 0xffcccc66, 0xff99cc66, 0xff66cc66, 0xff33cc66,
            0xff00cc66, 0xffff9966, 0xffcc9966, 0xff999966, 0xff669966, 0xff339966, 0xff009966, 0xffff6666,
            0xffcc6666, 0xff996666, 0xff666666, 0xff336666, 0xff006666, 0xffff3366, 0xffcc3366, 0xff993366,
            0xff663366, 0xff333366, 0xff003366, 0xffff0066, 0xffcc0066, 0xff990066, 0xff660066, 0xff330066,
            0xff000066, 0xffffff33, 0xffccff33, 0xff99ff33, 0xff66ff33, 0xff33ff33, 0xff00ff33, 0xffffcc33,
            0xffcccc33, 0xff99cc33, 0xff66cc33, 0xff33cc33, 0xff00cc33, 0xffff9933, 0xffcc9933, 0xff999933,
            0xff669933, 0xff339933, 0xff009933, 0xffff6633, 0xffcc6633, 0xff996633, 0xff666633, 0xff336633,
            0xff006633, 0xffff3333, 0xffcc3333, 0xff993333, 0xff663333, 0xff333333, 0xff003333, 0xffff0033,
            0xffcc0033, 0xff990033, 0xff660033, 0xff330033, 0xff000033, 0xffffff00, 0xffccff00, 0xff99ff00,
            0xff66ff00, 0xff33ff00, 0xff00ff00, 0xffffcc00, 0xffcccc00, 0xff99cc00, 0xff66cc00, 0xff33cc00,
            0xff00cc00, 0xffff9900, 0xffcc9900, 0xff999900, 0xff669900, 0xff339900, 0xff009900, 0xffff6600,
            0xffcc6600, 0xff996600, 0xff666600, 0xff336600, 0xff006600, 0xffff3300, 0xffcc3300, 0xff993300,
            0xff663300, 0xff333300, 0xff003300, 0xffff0000, 0xffcc0000, 0xff990000, 0xff660000, 0xff330000,
            0xff0000ee, 0xff0000dd, 0xff0000bb, 0xff0000aa, 0xff000088, 0xff000077, 0xff000055, 0xff000044,
            0xff000022, 0xff000011, 0xff00ee00, 0xff00dd00, 0xff00bb00, 0xff00aa00, 0xff008800, 0xff007700,
            0xff005500, 0xff004400, 0xff002200, 0xff001100, 0xffee0000, 0xffdd0000, 0xffbb0000, 0xffaa0000,
            0xff880000, 0xff770000, 0xff550000, 0xff440000, 0xff220000, 0xff110000, 0xffeeeeee, 0xffdddddd,
            0xffbbbbbb, 0xffaaaaaa, 0xff888888, 0xff777777, 0xff555555, 0xff444444, 0xff222222, 0xff111111
        ).map { it.toInt() }.toIntArray()

        @JvmStatic
        fun main(args: Array<String>) {
            // extremely complex:
            val file = OS.downloads.getChild("MagicaVoxel/vox/PrinceOfPersia.vox")
            // medium:
            // val file = OS.downloads.getChild("MagicaVoxel/vox/truck.vox")
            val vox = VOXReader().read(file)
        }

    }


}