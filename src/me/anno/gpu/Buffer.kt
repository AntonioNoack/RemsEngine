package me.anno.gpu

import me.anno.gpu.buffer.Attribute
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL15
import org.lwjgl.opengl.GL20.*
import java.nio.ByteBuffer

abstract class Buffer(val attributes: List<Attribute>, val stride: Int, val usage: Int = GL15.GL_STATIC_DRAW){

    constructor(attributes: List<Attribute>): this(attributes, attributes.sumBy { it.components * it.type.byteSize })

    init {
        var offset = 0L
        attributes.forEach {
            it.offset = offset
            offset += it.byteSize
        }
    }

    fun getName() = getName(0)
    fun getName(index: Int) = attributes[index].name

    var nioBuffer: ByteBuffer? = null

    var buffer = -1
    var isUpToDate = false
    fun upload(){
        if(!isUpToDate){
            if(buffer < 0) buffer = glGenBuffers()
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, buffer)
            if(nioBuffer == null){
                createNioBuffer()
                println("called create nio buffer")
            }
            val nio = nioBuffer!!
            nio.position(0)
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, nio, usage)
            isUpToDate = true
        }
    }

    abstract fun createNioBuffer()

    open fun draw(shader: Shader) = draw(shader, GL_TRIANGLES)
    open fun draw(shader: Shader, mode: Int){
        if(!isUpToDate) upload()
        else glBindBuffer(GL15.GL_ARRAY_BUFFER, buffer)
        shader.use()
        attributes.forEach { attr ->
            val index = shader.getAttributeLocation(attr.name)
            if(index > -1){
                val type = attr.type
                glVertexAttribPointer(index, attr.components, type.glType, type.normalized, stride, attr.offset)
                glEnableVertexAttribArray(index)
            }
        }
        glDrawArrays(mode, 0, nioBuffer!!.capacity() / stride)
    }


    fun destroy(){
        if(buffer > -1){
            GL15.glDeleteBuffers(buffer)
            buffer = -1
        }
    }




}