package me.anno.tests.game.creeperworld

import me.anno.tests.game.creeperworld.FluidFramebuffer.Companion.mixRGB2
import me.anno.utils.Color
import me.anno.utils.Color.a
import me.anno.utils.Color.mixARGB

class CreeperWorld(val w: Int, val h: Int) {

    val size = w * h

    val rockTypes = IntArray(size)
    val properties = HashMap<RockProperty, FloatArray>()
    val fluids = HashMap<String, FluidFramebuffer>()

    val pixelIsSet = BooleanArray(size)
    val pixels = ArrayList<Pixel>()
    val agents = ArrayList<Agent>()

    val fluidTypes = FluidTypes(this)

    val weightSum = FloatArray(size)

    lateinit var hardness: FloatArray
    lateinit var dissolved: FloatArray

    fun add(pixel: Pixel) {
        synchronized(pixels) {
            pixels.add(pixel)
        }
    }

    fun updatePixels() {
        synchronized(pixels) {
            pixels.removeAll { pixel ->
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
            image[pos] = mixARGB(image[pos], pixel.color, pixel.color.a())
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
                        val alpha = 1f - dissolved[i]
                        val rockTex = RockTypes.rockTextures[type]
                        val skyTex = RockTypes.skyColor
                        image[i++] = mixRGB2(
                            skyTex.getRGB(x % skyTex.width, y % skyTex.height),
                            rockTex.getRGB(x % rockTex.width, y % rockTex.height),
                            alpha
                        )
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

        weightSum.fill(0f)
        worker.processBalanced(0, size, 256) { i0, i1 ->
            for (fluid in fluidTypes.fluids) {
                val density = fluid.density
                val level = fluid.data.level.read
                for (i in i0 until i1) {
                    weightSum[i] += density * level[i]
                }
            }
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
