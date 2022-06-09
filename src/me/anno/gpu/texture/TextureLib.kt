package me.anno.gpu.texture

import me.anno.cache.data.ICacheData

object TextureLib {

    class IndestructibleTexture2D(
        name: String, w: Int, h: Int,
        private val creationData: Any
    ) : Texture2D(name, w, h, 1) {

        override fun destroy() {}

        fun doDestroy() {
            super.destroy()
        }

        private fun checkExistence() {
            checkSession()
            if (!isCreated || isDestroyed) {
                isDestroyed = false
                when (creationData) {
                    is ByteArray -> createRGBA(creationData, false)
                    is IntArray -> createBGRA(
                        IntArray(creationData.size) { creationData[it] },
                        false
                    )
                    else -> throw IllegalArgumentException()
                }
            }
        }

        override fun bind(index: Int, filtering: GPUFiltering, clamping: Clamping): Boolean {
            checkExistence()
            return super.bind(index, filtering, clamping)
        }

        override fun bind(index: Int, filtering: Filtering, clamping: Clamping): Boolean {
            checkExistence()
            return super.bind(index, filtering, clamping)
        }

        override fun bindTrulyNearest(index: Int): Boolean {
            checkExistence()
            return super.bindTrulyNearest(index)
        }
    }


    class IndestructibleTexture3D(
        name: String, w: Int, h: Int, d: Int,
        private val creationData: Any
    ) : Texture3D(name, w, h, d) {

        override fun destroy() {}

        fun doDestroy() {
            super.destroy()
        }

        private fun checkExistence() {
            checkSession()
            if (!isCreated || isDestroyed) {
                isDestroyed = false
                when (creationData) {
                    is ByteArray -> createRGBA(creationData)
                    else -> throw IllegalArgumentException()
                }
            }
        }

        override fun bind(index: Int, filtering: GPUFiltering) {
            checkExistence()
            super.bind(index, filtering)
        }

    }


    val invisibleTexture = IndestructibleTexture2D("invisible", 1, 1, ByteArray(4))
    val invisibleTex3d = IndestructibleTexture3D("invisible", 1, 1,1, ByteArray(4))
    val whiteTexture = IndestructibleTexture2D("white", 1, 1, ByteArray(4) { -1 })
    val whiteTex3d = IndestructibleTexture3D("white3d", 1, 1, 1, ByteArray(4) { -1 })
    val stripeTexture = IndestructibleTexture2D("stripes", 5, 1, IntArray(5) { if (it == 2) -1 else 0xffffff })
    val colorShowTexture =
        IndestructibleTexture2D("color-show", 2, 2, intArrayOf(0xccffffff.toInt(), -1, -1, 0xccffffff.toInt()))
    val normalTexture = IndestructibleTexture2D("normal", 1, 1, byteArrayOf(127, 127, -1, -1))
    val blackTexture = IndestructibleTexture2D("black", 1, 1, byteArrayOf(0, 0, 0, -1))

    @Suppress("ClassName")
    object nullTexture : ICacheData {
        override fun destroy() {}
    }

    fun bindWhite(index: Int): Boolean {
        return whiteTexture.bind(index, whiteTexture.filtering, whiteTexture.clamping ?: Clamping.CLAMP)
    }

    fun destroy() {
        invisibleTexture.doDestroy()
        whiteTexture.doDestroy()
        stripeTexture.doDestroy()
        colorShowTexture.doDestroy()
        normalTexture.doDestroy()
        blackTexture.doDestroy()
    }

}