package me.anno.ecs.components.sprite

import me.anno.ecs.Transform
import me.anno.ecs.components.mesh.material.Material
import me.anno.ecs.components.mesh.unique.MeshEntry
import me.anno.ecs.components.mesh.unique.UniqueMeshRenderer
import me.anno.ecs.components.mesh.utils.MeshVertexData
import me.anno.ecs.systems.OnUpdate
import me.anno.engine.serialization.SerializedProperty
import me.anno.gpu.buffer.Attribute
import me.anno.gpu.buffer.AttributeType
import me.anno.gpu.buffer.DrawMode
import me.anno.gpu.buffer.StaticBuffer
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.builder.ShaderStage
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.maths.chunks.cartesian.IntArrayChunkSystem
import org.joml.AABBf
import org.joml.Vector2i

class SpriteLayer : UniqueMeshRenderer<SpriteMeshLike, Vector2i>(
    listOf(
        Attribute("coordsI", AttributeType.SINT16, 2, true),
        Attribute("spriteI", AttributeType.SINT16, 2, true),
    ),
    spriteVertexData,
    DrawMode.TRIANGLES,
), OnUpdate {

    companion object {
        val spriteVertexData = MeshVertexData(
            listOf(
                ShaderStage(
                    "coords", listOf(
                        Variable(GLSLType.V2I, "coordsI", VariableMode.ATTR),
                        Variable(GLSLType.V2I, "spriteI", VariableMode.ATTR),
                        Variable(GLSLType.V3F, "localPosition", VariableMode.OUT),
                        Variable(GLSLType.V2F, "uv", VariableMode.OUT),
                        Variable(GLSLType.V2I, "textureTileCount")
                    ), "localPosition = vec3(vec2(coordsI),0.0);\n" +
                            "uv = vec2(spriteI) / vec2(textureTileCount);\n"
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

    override fun getData(key: Vector2i, mesh: SpriteMeshLike): StaticBuffer? {
        val k = material.textureTileCount.x
        if (mesh.positions.isEmpty() || k < 1) return null
        val buffer = StaticBuffer("sprites", attributes, mesh.positions.size * 6)
        mesh.fillBuffer(chunks, key, buffer, k)
        return buffer
    }

    override fun forEachHelper(key: Vector2i, transform: Transform): Material {
        return material
    }

    @SerializedProperty
    var material = SpriteMaterial()
    override val materials = listOf(material.ref)

    // todo define clusters:
    //  tiles, which belong together (e.g. trees)

    // todo define collision shapes/polygons for each tile

    // stores, which sprites are placed where; 0 = nothing
    val chunks = IntArrayChunkSystem(5, 5, 0, 0)
    val invalidChunks = HashSet<Vector2i>()

    // todo (de)serialize stuff

    // todo edit mode, where you can paint it

    // todo two chunk systems; one all, one visible (?)
    //  -> good for larger worlds when we have a good 2d-camera

    fun setSprite(x: Int, y: Int, spriteId: Int) {
        val value = spriteId + 1
        chunks.setElementAt(x, y, 0, value != 0, value)
        invalidChunks.add(Vector2i(x shr chunks.bitsX, y shr chunks.bitsY))
    }

    fun getSprite(x: Int, y: Int, generateIfMissing: Boolean = false): Int {
        val value = chunks.getElementAt(x, y, 0, generateIfMissing) ?: 0
        return value - 1
    }

    override fun onUpdate() {
        for (key in invalidChunks) {
            val bounds = AABBf()
            val chunk = chunks.getChunk(key.x, key.y, 0, true)!!
            val count = chunk.count { it != 0 }
            if (count > 0) {
                val positions = ShortArray(count)
                val sprites = IntArray(count)
                var i = 0
                var j = 0
                for (y in 0 until chunks.sizeY) {
                    for (x in 0 until chunks.sizeX) {
                        val id = chunk[i] - 1
                        if (id >= 0) {
                            positions[j] = i.toShort()
                            sprites[j] = id
                            bounds.union(x.toFloat(), y.toFloat(), 0f)
                            j++
                        }
                        i++
                    }
                }
                val dx = key.x.shl(chunks.bitsX).toFloat()
                val dy = -key.y.shl(chunks.bitsY).toFloat()
                bounds.minX += dx
                bounds.minY += dy
                bounds.maxX += dx + 1f // extend bounds for 1x1-sized cells
                bounds.maxY += dy + 1f
                val mesh = SpriteMeshLike(positions, sprites, bounds, materials)
                val buffer = getData(key, mesh)!!
                val entry = MeshEntry(mesh, bounds, buffer)
                set(key, entry)
            } else {
                remove(key, true)
            }
        }
        invalidChunks.clear()
    }
}