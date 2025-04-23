package me.anno.gpu.texture

class IndestructibleTexture3D(
    name: String, w: Int, h: Int, d: Int,
    private val creationData: Any
) : Texture3D(name, w, h, d) {

    override fun destroy() {}

    private fun checkExistence() {
        checkSession()
        if (!wasCreated || isDestroyed) {
            isDestroyed = false
            when (creationData) {
                is ByteArray -> createRGBA(creationData)
                is IntArray -> createRGBA8(creationData)
                else -> throw IllegalArgumentException("Unknown constructor data")
            }
        }
    }

    override fun bind(index: Int, filtering: Filtering, clamping: Clamping): Boolean {
        checkExistence()
        return super.bind(index, filtering, clamping)
    }
}