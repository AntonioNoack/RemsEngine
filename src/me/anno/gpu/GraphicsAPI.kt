package me.anno.gpu

/**
 * todo abstract away all rendering into this interface
 *  - lots of work :/ -> maybe it's easier to implement our subset of used OpenGL in other APIs
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