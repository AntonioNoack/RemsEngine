package me.anno.gpu.texture

import me.anno.cache.ICacheData
import me.anno.gpu.framebuffer.TargetType
import me.anno.utils.Color.black

/**
 * library of standard textures like white, black, transparent, striped
 * */
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
                    is ByteArray -> {
                        if (creationData.size == w * h) {
                            createMonochrome(creationData, false)
                        } else {
                            createRGBA(creationData, false)
                        }
                    }
                    is IntArray -> createBGRA(
                        IntArray(creationData.size) { creationData[it] },
                        false
                    )
                    "depth" -> create(TargetType.DEPTH16)
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

    class IndestructibleCubemap(
        name: String, w: Int,
        private val creationData: Any
    ) : CubemapTexture(name, w, 1) {

        override fun destroy() {}

        fun doDestroy() {
            super.destroy()
        }

        private fun checkExistence() {
            checkSession()
            if (!isCreated || isDestroyed) {
                isDestroyed = false
                when (creationData) {
                    is ByteArray -> createRGBA(Array(6) { creationData }.toList())
                    "depth" -> create(TargetType.DEPTH16)
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

        @Suppress("unused")
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

        override fun bind(index: Int, filtering: GPUFiltering, clamping: Clamping): Boolean {
            checkExistence()
            return super.bind(index, filtering, clamping)
        }

    }

    class IndestructibleTexture2DArray(
        name: String, w: Int, h: Int, d: Int,
        private val creationData: Any
    ) : Texture2DArray(name, w, h, d) {

        override fun destroy() {}

        @Suppress("unused")
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

        override fun bind(index: Int, filtering: GPUFiltering, clamping: Clamping): Boolean {
            checkExistence()
            return super.bind(index, filtering, clamping)
        }

    }

    private val white = byteArrayOf(-1, -1, -1, -1)

    val invisibleTexture = IndestructibleTexture2D("invisible", 1, 1, ByteArray(4))
    val invisibleTex3d = IndestructibleTexture3D("invisible", 1, 1, 1, ByteArray(4))
    val whiteTexture = IndestructibleTexture2D("white", 1, 1, intArrayOf(-1))
    val whiteTex3d = IndestructibleTexture3D("white3d", 1, 1, 1, white)
    val whiteTex2da = IndestructibleTexture2DArray("white2da", 1, 1, 1, white)
    val whiteCube = IndestructibleCubemap("whiteCube", 1, white)
    val depthTexture = IndestructibleTexture2D("depth", 1, 1, "depth")
    val depthCube = IndestructibleCubemap("depth", 1, "depth")
    val stripeTexture = IndestructibleTexture2D("stripes", 5, 1, IntArray(5) { if (it == 2) -1 else 0xffffff })
    val colorShowTexture =
        IndestructibleTexture2D("color-show", 2, 2, intArrayOf(0xccffffff.toInt(), -1, -1, 0xccffffff.toInt()))
    val normalTexture = IndestructibleTexture2D("normal", 1, 1, byteArrayOf(127, 127, -1, -1))
    val gradientXTex = IndestructibleTexture2D("gradientX", 5, 1, byteArrayOf(0, 63, 127, -64, -1))
    val blackTexture = IndestructibleTexture2D("black", 1, 1, byteArrayOf(0, 0, 0, -1))
    val blackCube = IndestructibleCubemap("blackCube", 1, byteArrayOf(0, 0, 0, -1))
    val missingTexture = IndestructibleTexture2D("missing", 2, 2,
        IntArray(4) { (if (it in 1..2) 0 else 0xff00ff) or black })

    // todo is this still used? if not, remove it
    @Suppress("ClassName")
    object nullTexture : ICacheData {
        override fun destroy() {}
    }

    fun bindWhite(index: Int): Boolean {
        return whiteTexture.bind(index, whiteTexture.filtering, whiteTexture.clamping ?: Clamping.CLAMP)
    }

    fun destroy() {
        invisibleTexture.doDestroy()
        invisibleTex3d.doDestroy()
        whiteTexture.doDestroy()
        whiteTex3d.doDestroy()
        whiteTex2da.doDestroy()
        whiteCube.doDestroy()
        depthCube.doDestroy()
        depthTexture.doDestroy()
        stripeTexture.doDestroy()
        colorShowTexture.doDestroy()
        normalTexture.doDestroy()
        gradientXTex.doDestroy()
        blackTexture.doDestroy()
        missingTexture.doDestroy()
    }

}