package me.anno.mesh.assimp

import org.lwjgl.opengl.GL30.*
import org.lwjgl.system.MemoryUtil
import java.nio.ByteBuffer
import java.nio.FloatBuffer
import kotlin.math.sqrt


open class AssimpMesh(
    positions: FloatArray,
    textCoords: FloatArray,
    normals: FloatArray,
    indices: IntArray,
    jointIndices: IntArray = IntArray(MAX_WEIGHTS * positions.size / 3 * 4),
    weights: FloatArray = FloatArray(MAX_WEIGHTS * positions.size / 3 * 4)
) {

    var vaoId = 0

    private val vboIdList = IntArray(6) { -1 }

    var vertexCount = 0
    var material: Material? = null
    var boundingRadius = 0f

    private fun calculateBoundingRadius(positions: FloatArray) {
        var radiusSq = 0f
        for (i in positions.indices step 3) {
            val x = positions[i]
            val y = positions[i + 1]
            val z = positions[i + 2]
            val distanceSq = x * x + y * y + z * z
            if (distanceSq > radiusSq) radiusSq = distanceSq
        }
        boundingRadius = sqrt(radiusSq)
    }

    fun startRender() {

        renderReference.value

        // todo bind all relevant textures
        // todo - color
        // todo - normal
        // todo - alpha?
        // todo - occlusion

        /*val texture = material?.texture
        if (texture != null) {
            // Activate first texture bank
            glActiveTexture(GL_TEXTURE0)
            // Bind the texture
            glBindTexture(GL_TEXTURE_2D, texture.getId())
        }

        val normalMap = material?.normalMap
        if (normalMap != null) {
            // Activate second texture bank
            glActiveTexture(GL_TEXTURE1)
            // Bind the texture
            glBindTexture(GL_TEXTURE_2D, normalMap.getId())
        }*/

        // Draw the mesh
        glBindVertexArray(vaoId)

    }

    fun endRender() {
        // Restore state
        glBindVertexArray(0)
        glBindTexture(GL_TEXTURE_2D, 0)
    }

    fun render() {
        startRender()
        glDrawElements(GL_TRIANGLES, vertexCount, GL_UNSIGNED_INT, 0)
        endRender()
    }

    /*fun renderList(gameItems: List<GameItem>, consumer: Consumer<GameItem?>) {
        initRender()
        for (gameItem in gameItems) {
            if (gameItem.isInsideFrustum()) {
                // Set up data required by GameItem
                consumer.accept(gameItem)
                // Render this game item
                glDrawElements(GL_TRIANGLES, vertexCount, GL_UNSIGNED_INT, 0)
            }
        }
        endRender()
    }*/

    fun cleanUp() {

        glDisableVertexAttribArray(0)

        // Delete the VBOs
        glBindBuffer(GL_ARRAY_BUFFER, 0)
        for (vboId in vboIdList) {
            glDeleteBuffers(vboId)
        }

        // Delete the texture
        /*val texture: Texture = material.getTexture()
        if (texture != null) {
            texture.cleanup()
        }*/

        // Delete the VAO
        glBindVertexArray(0)
        glDeleteVertexArrays(vaoId)

    }

    fun deleteBuffers() {

        // Delete the VBOs
        glBindBuffer(GL_ARRAY_BUFFER, 0)
        for (vboId in vboIdList) {
            glDeleteBuffers(vboId)
        }

        // Delete the VAO
        glBindVertexArray(0)
        glDeleteVertexArrays(vaoId)

    }

    fun create(
        buffer: FloatBuffer, data: FloatArray,
        index: Int, size: Int
    ) {
        buffer.position(0)
        buffer.put(data)
        buffer.position(0)
        buffer.limit(data.size)
        glBindBuffer(GL_ARRAY_BUFFER, vboIdList[index])
        glBufferData(GL_ARRAY_BUFFER, buffer, GL_STATIC_DRAW)
        glEnableVertexAttribArray(0)
        glVertexAttribPointer(index, size, GL_FLOAT, false, 0, 0)
    }

    fun create(
        buffer: ByteBuffer, data: IntArray,
        index: Int, size: Int
    ) {
        buffer.position(0)
        buffer.asIntBuffer().put(data)
        buffer.position(0)
        buffer.limit(data.size)
        glBindBuffer(GL_ARRAY_BUFFER, vboIdList[index])
        glBufferData(GL_ARRAY_BUFFER, buffer, GL_STATIC_DRAW)
        glEnableVertexAttribArray(0)
        glVertexAttribPointer(index, size, GL_FLOAT, false, 0, 0)
    }

    init {
        calculateBoundingRadius(positions)
    }

    val renderReference = lazy {
        var byteBuffer: ByteBuffer? = null
        try {

            vertexCount = indices.size
            vaoId = glGenVertexArrays()
            glBindVertexArray(vaoId)

            glGenBuffers(vboIdList)

            byteBuffer = MemoryUtil.memAlloc(positions.size / 3 * 4 * 4)
            val dataBuffer = byteBuffer.asFloatBuffer()

            create(dataBuffer, positions, 0, 3)
            create(dataBuffer, textCoords, 1, 2)
            create(dataBuffer, normals, 2, 3)
            create(dataBuffer, weights, 3, MAX_WEIGHTS)
            create(byteBuffer, jointIndices, 4, MAX_WEIGHTS)

            // Index VBO
            val vboId = vboIdList[5]
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, vboId)
            glBufferData(GL_ELEMENT_ARRAY_BUFFER, indices, GL_STATIC_DRAW)
            glBindBuffer(GL_ARRAY_BUFFER, 0)
            glBindVertexArray(0)

        } finally {
            if (byteBuffer != null) {
                MemoryUtil.memFree(byteBuffer)
            }
        }
    }

    companion object {
        const val MAX_WEIGHTS = 4
    }
}