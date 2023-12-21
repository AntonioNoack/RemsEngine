package me.anno.tests.game.creeperworld

class World {

    val rockTypes = IntArray(size)
    val properties = HashMap<RockProperty, FloatArray>()
    val fluids = HashMap<String, FluidFramebuffer>()

    val pixelIsSet = BooleanArray(size)
    val pixels = ArrayList<Pixel>()

    val agents = ArrayList<Agent>()

    lateinit var hardness: FloatArray

    fun updatePixels() {
        pixels.removeIf { pixel ->
            if (pixel.progress == pixel.path.lastIndex) {
                // complete
                pixelIsSet[pixel.path.last()] = false
                pixel.agent.loadingState++
                true
            } else {
                val nextPos = pixel.path[pixel.progress + 1]
                if (!pixelIsSet[nextPos]) {
                    pixelIsSet[pixel.path[pixel.progress++]] = false
                    pixelIsSet[nextPos] = true
                }
                false
            }
        }
    }

    fun setBlock(x: Int, y: Int, type: RockType?) {
        setBlock(x + y * w, type)
    }

    fun setBlock(i: Int, type: RockType?) {
        if (type != null) {
            rockTypes[i] = type.id
            for ((property, values) in properties) {
                val value = type.properties[property] ?: property.blockDefault
                values[i] = value
            }
            for (fluid in fluids) {
                fluid.value.level.read[i] = 0f
            }
        } else {
            rockTypes[i] = 0
            for ((property, values) in properties) {
                values[i] = property.airDefault
            }
        }
    }

    fun register(property: RockProperty) {
        val data = FloatArray(size)
        if (property.airDefault != 0f) {
            data.fill(property.airDefault)
        }
        properties[property] = data
    }

    fun register(fluid: FluidLayer) {
        fluids[fluid.id] = fluid.data
    }

    operator fun get(property: RockProperty): FloatArray {
        return properties[property]!!
    }
}
