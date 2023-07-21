package me.anno.gpu

/**
 * todo abstract away all rendering into this interface
 *  - lots of work :/
 * */
interface GraphicsAPI {

    fun draw()
    fun drawArrays()
    fun drawElements()
    fun drawInstanced()
    fun drawArraysInstanced()
    fun drawElementsInstanced()

    fun clear(fb: Int, mask: Int, r: Float, g: Float, b: Float, a: Float, d: Float, s: Int)

    fun createFB(): Int
    fun destroyFB(): Int

    fun createTex(): Int
    fun destroyTex(): Int

}