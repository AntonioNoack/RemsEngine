package me.anno.tests.game.creeperworld

import me.anno.utils.Color
import me.anno.utils.Color.a

class CreeperWorld(val w: Int, val h: Int) {

    val size = w * h

    val rockTypes = IntArray(size)
    val properties = HashMap<RockProperty, FloatArray>()
    val fluids = HashMap<String, FluidFramebuffer>()

    val pixelIsSet = BooleanArray(size)
    val pixels = ArrayList<Pixel>()
    val agents = ArrayList<Agent>()

    val fluidTypes = FluidTypes(this)

    lateinit var hardness: FloatArray

    fun add(pixel: Pixel) {
        synchronized(pixels) {
            pixels.add(pixel)
        }
    }

    fun updatePixels() {
        synchronized(pixels) {
            pixels.removeIf { pixel ->
                if (pixel.isFinished()) {
                    pixel.onFinish(this)
                    true
                } else {
                    pixel.update(this)
                    false
                }
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

    fun render(image: IntArray) {
        // render all rock types and such

        worker.processBalanced(0, h, 16) { y0, y1 ->
            var i = y0 * w
            for (y in y0 until y1) {
                for (x in 0 until w) {
                    val skyTex = RockTypes.skyColor
                    image[i++] = skyTex.getRGB(x % skyTex.width, y % skyTex.height) or Color.black
                }
            }
        }

        for (fluid in fluidTypes.fluids) {
            fluid.data.render(image, fluid.color)
        }

        for (pixel in pixels) {
            val pos = pixel.path[pixel.progress]
            image[pos] = Color.mixARGB(image[pos], pixel.color, pixel.color.a())
        }

        for (agent in agents) {
            agent.render(this, image)
        }

        worker.processBalanced(0, h, 16) { y0, y1 ->
            var i = y0 * w
            for (y in y0 until y1) {
                for (x in 0 until w) {
                    val type = rockTypes[i]
                    if (type > 0) {
                        val rockTex = RockTypes.rockTextures[type]
                        image[i++] = rockTex.getRGB(x % rockTex.width, y % rockTex.height) or Color.black
                    } else i++
                }
            }
        }
    }

    fun update(tickIndex: Int) {
        // todo update all systems that need it
        //  - rocks
        // done:
        //  - ships
        //  - fluids
        //  - particles

        for (agent in agents) {
            agent.update(this)
        }

        for (fluid in fluidTypes.fluids) {
            if (tickIndex % fluid.viscosity == 0) {
                fluid.tick(this, sumByType)
            }
        }

        updatePixels()
    }

    operator fun get(property: RockProperty): FloatArray {
        return properties[property]!!
    }
}
