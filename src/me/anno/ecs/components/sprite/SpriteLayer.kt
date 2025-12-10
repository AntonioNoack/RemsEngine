package me.anno.ecs.components.sprite

import me.anno.cache.FileCacheList
import me.anno.ecs.annotations.Docs
import me.anno.ecs.annotations.EditorField
import me.anno.ecs.annotations.Range
import me.anno.ecs.components.mesh.material.Material
import me.anno.ecs.components.mesh.unique.MeshEntry
import me.anno.ecs.components.mesh.unique.UniqueMeshRendererImpl
import me.anno.ecs.components.mesh.utils.MeshVertexData
import me.anno.ecs.interfaces.CustomEditMode
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.ecs.systems.OnUpdate
import me.anno.engine.serialization.NotSerializedProperty
import me.anno.engine.serialization.SerializedProperty
import me.anno.engine.ui.render.RenderView
import me.anno.gpu.buffer.Attribute
import me.anno.gpu.buffer.AttributeType
import me.anno.gpu.buffer.BufferUsage
import me.anno.gpu.buffer.CompactAttributeLayout.Companion.bind
import me.anno.gpu.buffer.DrawMode
import me.anno.gpu.buffer.StaticBuffer
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.builder.ShaderStage
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.input.Input
import me.anno.input.Key
import me.anno.io.base.BaseWriter
import me.anno.utils.assertions.assertTrue
import org.joml.AABBf
import org.joml.Matrix4x3
import org.joml.Vector2i
import org.joml.Vector3d
import org.joml.Vector3f
import kotlin.math.floor
import kotlin.math.sign

/**
 * Renderer for sprites; one z-layer; supports an atlas with upto 256x256 tiles.
 * The tile layout can be configured inside the material.
 *
 * For large worlds, you need a chunk-manager to load/unload them dynamically.
 * Mesh: IntArray of packed values: [uint8: px, uint8: py, uint16: spiteId]
 * */
class SpriteLayer :
    UniqueMeshRendererImpl<Vector2i, IntArray>(attributes, spriteVertexData, false, DrawMode.TRIANGLES),
    OnUpdate, CustomEditMode {

    companion object {

        private val attributes = bind(
            Attribute("coordsI", AttributeType.SINT16, 2),
            Attribute("spriteI", AttributeType.SINT16, 2),
        )

        private const val SPRITE_BITS_X = 5
        private const val SPRITE_BITS_Y = 5

        private const val SPRITE_SIZE_X = 1 shl SPRITE_BITS_X
        private const val SPRITE_SIZE_Y = 1 shl SPRITE_BITS_Y

        private const val SPRITE_MASK_X = SPRITE_SIZE_X - 1
        private const val SPRITE_MASK_Y = SPRITE_SIZE_Y - 1

        val spriteVertexData = MeshVertexData(
            listOf(
                ShaderStage(
                    "positions", listOf(
                        Variable(GLSLType.V2I, "coordsI", VariableMode.ATTR),
                        Variable(GLSLType.V2I, "spriteI", VariableMode.ATTR),
                        Variable(GLSLType.V3F, "localPosition", VariableMode.OUT),
                        Variable(GLSLType.V1I, "spriteIndex", VariableMode.OUT),
                        Variable(GLSLType.V2F, "uv", VariableMode.OUT)
                    ), "localPosition = vec3(vec2(coordsI),0.0);\n" +
                            "spriteIndex = spriteI.x;\n" +
                            "uv = vec2(spriteI.y & 1, (spriteI.y>>1) & 1);\n"
                )
            ),
            listOf(
                ShaderStage(
                    "nor", listOf(
                        Variable(GLSLType.V3F, "normal", VariableMode.OUT),
                        Variable(GLSLType.V4F, "tangent", VariableMode.OUT)
                    ), "normal = vec3(0.0,0.0,1.0); tangent = vec4(0.0);\n"
                )
            ),
            listOf(
                ShaderStage(
                    "def-col", listOf(
                        Variable(GLSLType.V4F, "vertexColor0", VariableMode.OUT)
                    ), "vertexColor0 = vec4(1.0);\n"
                )
            ),
            MeshVertexData.DEFAULT.loadMotionVec,
            emptyList()
        )
    }

    override fun createBuffer(key: Vector2i, mesh: IntArray): Pair<StaticBuffer, IntArray?>? {
        val k = material.numTiles.x
        if (mesh.isEmpty() || k < 1) return null
        return getDataSafely(key, mesh) to null
    }

    fun getDataSafely(key: Vector2i, mesh: IntArray): StaticBuffer {
        val k = material.numTiles.x
        assertTrue(mesh.isNotEmpty() && k >= 1)
        val buffer = StaticBuffer("sprites", attributes, mesh.size * 6, BufferUsage.STATIC)
        fillBuffer(mesh, key, buffer)
        return buffer
    }

    private fun StaticBuffer.addVertex(px: Int, py: Int, spriteId: Short, uvId: Short) {
        putShort(px.toShort())
        putShort(py.toShort())
        putShort(spriteId)
        putShort(uvId)
    }

    private fun fillBuffer(entries: IntArray, key: Vector2i, buffer: StaticBuffer) {
        // create a quad for each mesh
        for (i in entries.indices) {
            val entry = entries[i]
            val pos = entry.ushr(16)
            val spiteId = entry.toShort()
            val posX = key.x.shl(SPRITE_BITS_X) + pos.and(SPRITE_MASK_X)
            val posY = key.y.shl(SPRITE_BITS_Y) + pos.shr(SPRITE_BITS_X).and(SPRITE_MASK_Y)
            buffer.addVertex(posX, posY, spiteId, 0)
            buffer.addVertex(posX + 1, posY, spiteId, 1)
            buffer.addVertex(posX + 1, posY + 1, spiteId, 3)
            buffer.addVertex(posX, posY, spiteId, 0)
            buffer.addVertex(posX + 1, posY + 1, spiteId, 3)
            buffer.addVertex(posX, posY + 1, spiteId, 2)
        }
    }

    @EditorField
    @NotSerializedProperty
    @Range(0.0, 65535.0)
    @Docs("Used for edit mode: which tile to place")
    var drawingId = 0

    /**
     * Allows/Implements painting in the editor.
     * */
    override fun onEditMove(x: Float, y: Float, dx: Float, dy: Float): Boolean {
        if (!Input.isLeftDown && !Input.isRightDown) return false
        editMove(if (Input.isLeftDown) Key.BUTTON_LEFT else Key.BUTTON_RIGHT)
        return true
    }

    override fun onEditClick(button: Key, long: Boolean): Boolean {
        editMove(button)
        return true
    }

    private fun editMove(button: Key) {
        // project ray onto this
        val globalTransform = transform?.globalTransform?.invert(Matrix4x3())
            ?: Matrix4x3()
        val ri = RenderView.currentInstance ?: return
        val pos = globalTransform.transformPosition(ri.mousePosition, Vector3d())
        val dir = globalTransform.transformDirection(ri.mouseDirection, Vector3f())
        if (dir.z * sign(pos.z) > 0.0) return
        val distance = -pos.z / dir.z
        val posX = floor(pos.x + dir.x * distance).toInt()
        val posY = floor(pos.y + dir.y * distance).toInt()
        if (button == Key.BUTTON_MIDDLE) {
            val newDrawingId = getSprite(posX, posY)
            if (newDrawingId >= 0) drawingId = newDrawingId
        } else {
            // set tile to drawingId
            setSprite(posX, posY, if (button == Key.BUTTON_LEFT) drawingId else -1)
        }
    }

    @SerializedProperty
    var material = SpriteMaterial()
        set(value) {
            if (field !== value) {
                value.copyInto(field)
            }
        }

    override val materials: FileCacheList<Material> =
        FileCacheList.of(material)

    // todo define clusters:
    //  tiles, which belong together (e.g. trees)

    // todo define collision shapes/polygons for each tile

    // stores, which sprites are placed where; 0 = nothing
    val chunks = HashMap<Vector2i, SpriteChunk>()
    val invalidChunks = HashSet<Vector2i>()

    fun setSprite(x: Int, y: Int, spriteId: Int) {
        val value = spriteId + 1
        val key = getKey(x, y)
        val chunk = getChunkAt(key, value > 0) ?: return
        chunk.values[getLocalIndex(x, y)] = value
        invalidChunks.add(key)
    }

    fun getSprite(x: Int, y: Int): Int {
        val chunk = getChunkAt(getKey(x, y), false) ?: return -1
        return chunk.values[getLocalIndex(x, y)] - 1
    }

    private fun getLocalIndex(x: Int, y: Int): Int {
        val lx = x and SPRITE_MASK_X
        val ly = y and SPRITE_MASK_Y
        return lx + ly.shl(SPRITE_BITS_X)
    }

    private fun getKey(x: Int, y: Int): Vector2i {
        return Vector2i(x shr SPRITE_BITS_X, y shr SPRITE_BITS_Y)
    }

    fun getChunkAt(key: Vector2i, generateIfMissing: Boolean): SpriteChunk? {
        return if (generateIfMissing) chunks.getOrPut(key) {
            SpriteChunk(key, IntArray(SPRITE_SIZE_X * SPRITE_SIZE_Y))
        } else chunks[key]
    }

    override fun onUpdate() {
        validateChunks()
    }

    private fun validateChunks() {
        if (invalidChunks.isEmpty()) return // skip allocating iterator
        invalidChunks.forEach(::validateChunk)
        invalidChunks.clear()
    }

    private fun validateChunk(key: Vector2i) {
        val chunk = chunks[key]?.values ?: return
        val count = chunk.count { it != 0 }
        if (count > 0) {
            val entry = createMeshData(key, chunk, count)
            set(key, entry)
        } else {
            remove(key, true)
        }
    }

    private fun createMeshData(key: Vector2i, chunk: IntArray, count: Int): MeshEntry<IntArray> {
        val bounds = AABBf()
        val faces = IntArray(count)
        fillMeshData(chunk, faces, bounds)
        val dx = key.x.shl(SPRITE_BITS_X).toFloat()
        val dy = key.y.shl(SPRITE_BITS_Y).toFloat()
        bounds.minX += dx
        bounds.minY += dy
        bounds.maxX += dx + 1f // extend bounds for 1x1-sized cells
        bounds.maxY += dy + 1f
        val buffer = getDataSafely(key, faces)
        return MeshEntry(faces, bounds, buffer, null)
    }

    private fun fillMeshData(srcChunk: IntArray, dstData: IntArray, dstBounds: AABBf) {
        var posIndex = 0
        var j = 0
        for (y in 0 until SPRITE_SIZE_Y) {
            for (x in 0 until SPRITE_SIZE_X) {
                val id = srcChunk[posIndex] - 1
                if (id >= 0) {
                    dstData[j] = posIndex.shl(16) or id
                    dstBounds.union(x.toFloat(), y.toFloat(), 0f)
                    j++
                }
                posIndex++
            }
        }
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeObject(this, "material", material)
        writer.writeObjectList(this, "chunks", chunks.values.toList())
    }

    override fun setProperty(name: String, value: Any?) {
        when (name) {
            "chunks" -> {
                if (value !is List<*>) return
                for (vi in value.indices) {
                    val chunk = value[vi] as? SpriteChunk ?: continue
                    chunks[chunk.key] = chunk
                }
            }
            "material" -> material = value as? SpriteMaterial ?: return
            else -> super.setProperty(name, value)
        }
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        if (dst !is SpriteLayer || dst === this) return
        dst.chunks.clear()
        dst.chunks.putAll(chunks.mapValues { it.value.clone() })
    }
}