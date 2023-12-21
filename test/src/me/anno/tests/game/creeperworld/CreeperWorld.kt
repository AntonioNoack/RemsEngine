package me.anno.tests.game.creeperworld

import me.anno.Time
import me.anno.config.DefaultConfig.style
import me.anno.gpu.drawing.DrawTexts.drawSimpleTextCharByChar
import me.anno.gpu.drawing.DrawTexts.monospaceFont
import me.anno.gpu.drawing.DrawTextures.drawTexture
import me.anno.gpu.texture.Texture2D
import me.anno.gpu.texture.Texture2D.Companion.switchRGB2BGR
import me.anno.image.ImageCache
import me.anno.io.files.FileReference.Companion.getReference
import me.anno.language.translation.NameDesc
import me.anno.maths.Maths.SECONDS_TO_NANOS
import me.anno.maths.noise.PerlinNoise
import me.anno.maths.paths.PathFinding
import me.anno.studio.StudioBase
import me.anno.tests.physics.fluid.RWState
import me.anno.ui.base.components.AxisAlignment
import me.anno.ui.base.groups.MapPanel
import me.anno.ui.debug.TestStudio.Companion.testUI3
import me.anno.utils.Color.a
import me.anno.utils.Color.mixARGB
import me.anno.utils.Color.withAlpha
import me.anno.utils.hpc.ProcessingGroup
import org.joml.Vector2i
import org.joml.Vector4f
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.sqrt

// todo 2d or 3d creeper world game:
//  - fluid layers
//  - fluids may interact with each other
//  - fluids flow
//  - rock layers
//  - fluid may dissolve weak rock
//  - fluid may flow through sand
//  - agents that move by dissolving into pixels
//  - agents that can influence (paint) fluid layers
//  - deliver resources by things that float through the world
//  - deliver them from main ship

// todo possibility to be written by fluids?... hard... we'd need to transfer that data from GPU to CPU or simulate on CPU
//  -> but we want maximum flexibility, so let's do everything on the CPU, it's fast enough for 2d (?)...
//  and also just simulate at 5fps or so, and interpolate intermediate results; we may also tick different fluids at different rates to make them appear viscous :3

open class Layer(val id: String)
class RockProperty(val blockDefault: Float, val airDefault: Float)

val worker = ProcessingGroup("CreeperWorld", Runtime.getRuntime().availableProcessors())

fun validateNotOnWall(hardness: FloatArray, levels: FloatArray) {
    for (i in 0 until size) {
        if (isSolid(hardness[i]) && levels[i] != 0f) {
            throw IllegalStateException("Illegal: fluid on rock")
        }
    }
}

fun forAllEdgePixels(processEdgePixel: (x: Int, y: Int, i: Int) -> Unit) {
    for (y in 0 until h) {
        processEdgePixel(0, y, y * w)
        processEdgePixel(w - 1, y, (w - 1) + y * w)
    }

    for (x in 1 until w - 1) {
        processEdgePixel(x, 0, x)
        processEdgePixel(x, h - 1, x + w * (h - 1))
    }
}

class RockProcessor(val shader: RockShader, val dependencies: List<RockProperty> = emptyList())
fun interface RockShader {
    fun process(
        rockTypes: IntArray,
        src: RockFramebuffer, dst: RockFramebuffer,
        properties: Map<String, FloatArray>
    )
}

class RockFramebuffer {
    val value = FloatArray(size)
}

/**
 * each rock type has different properties;
 * rock types define properties; they then get saved to the GPU in separate framebuffers;
 * which then get used by shaders
 * */
class RockPropertyLayer(
    id: String,
    val data: RWState<RockFramebuffer>,
    val tick: List<FluidShader>,
) : Layer(id) {

}

val w = 64
val h = 64
val size = w * h

fun isSolid(hardness: Float): Boolean {
    return hardness > 0.1f
}

fun findPixelPath(
    rockTypes: IntArray,
    start: Vector2i,
    end: Vector2i,
): IntArray? {
    val path = PathFinding.aStar(
        start, end, start.gridDistance(end).toDouble(),
        Int.MAX_VALUE.toDouble(), 64,
        includeStart = true, includeEnd = true,
    ) { node, callback ->
        // query all neighbors
        fun call(dx: Int, dy: Int) {
            val x = node.x + dx
            val y = node.y + dy
            val i = x + y * w
            if (rockTypes[i] == 0) {
                val to = Vector2i(x, y)
                callback(to, 1.0, to.gridDistance(end).toDouble())
            }
        }
        if (node.x > 0) call(-1, 0)
        if (node.x < w - 1) call(+1, 0)
        if (node.y > 0) call(0, -1)
        if (node.y < h - 1) call(0, +1)
    } ?: return null
    return IntArray(path.size) { index ->
        val node = path[index]
        node.x + node.y * w
    }
}

fun main() {

    // define a world
    // todo use perlin to place rock types

    val hardness = RockProperty(0.5f, 0f)

    val stone = RockType(1, NameDesc("Stone"), mapOf(hardness to 0.7f), 0x777777)
    val rock = RockType(2, NameDesc("Rock"), mapOf(hardness to 0.9f), 0x333337)
    val clay = RockType(3, NameDesc("Clay"), mapOf(hardness to 0.1f), 0x889999)

    // todo gravity for sand
    val sand = RockType(4, NameDesc("Sand"), mapOf(hardness to 0f), 0xffeeaa)

    val world = World()

    val properties = listOf(hardness)
    for (property in properties) {
        world.register(property)
    }

    val creeper = FluidLayer(
        "creeper", listOf(
            FluidAccelerate(-0.1f, 0.5f),
            FluidClampVelocity(world[hardness]),
            FluidMove(),
            FluidBlur(1f / 8f, 1f - 1f / 100f),
            FluidExpand(),
        ), 3, 0x33ff33.withAlpha(200)
    )

    val antiCreeper = FluidLayer("antiCreeper", listOf(
        FluidAccelerate(-0.1f, 0.5f),
        FluidClampVelocity(world[hardness]),
        FluidMove(),
        FluidBlur(1f / 8f, 1f - 1f / 100f),
        FluidExpand(),
    ), 3, 0x3377ff.withAlpha(100))

    world.hardness = world[hardness]

    // todo creep + anti-creep = 0
    // todo acid + rock = water
    // todo lava + water = obsidian/rock
    val acid = FluidLayer("acid", listOf(), 2, 0xff8833.withAlpha(100))
    val lava = FluidLayer("lava", listOf(), 10, 0xffaa00.withAlpha(255))
    val water = FluidLayer("water", listOf(), 1, 0x99aaff.withAlpha(50))
    val fluids = listOf(creeper, antiCreeper)

    for (fluid in fluids) {
        world.register(fluid)
    }

    val skyColor = 0xcceeff

    val rockTypes = listOf(stone, rock, clay, sand)
    val rockById = rockTypes.associateBy { it.id }
    val rockColors = IntArray(rockTypes.maxOf { it.id } + 1) { id ->
        (rockById[id]?.color ?: skyColor).withAlpha(255)
    }

    // fill in some sample data
    val cl = creeper.data.level.read
    val al = antiCreeper.data.level.read
    val dr = min(w, h) / 3
    for (j in -dr..dr) {
        for (i in -dr..dr) {
            val level = dr - sqrt(i * i + j * j + 0.5f)
            if (level > 0f) {
                val x0 = i + w / 3
                val x1 = i + w * 2 / 3
                val y = j + h / 2
                cl[x0 + y * w] += level / dr
                al[x1 + y * w] += level / dr
            }
        }
    }

    val height = PerlinNoise(
        1234L, 5, 0.5f,
        h * 5 / 6f, h * 6 / 6f,
        Vector4f(0.1f)
    )

    for (x in 0 until w) {
        val hi = height[x.toFloat()].toInt()
        for (y in hi until h) {
            world.setBlock(x, y, stone)
        }
    }

    if (false) {
        forAllEdgePixels { _, _, i ->
            world.setBlock(i, stone)
        }
    }

    val sumByType = HashMap<String, String>()

    // spawn a few pixels and agents
    // todo spawn an agent with an actual image,
    //  and then use some key to reposition it (drag?), and let's see how it moves :3
    val chopperImage = ImageCache[getReference("res://textures/chopper.png"), false]!!
    val chopper = object : Agent(chopperImage, Vector2i(10, 10)) {
        override fun onUpdate() {}
    }
    world.agents.add(chopper)
    val samplePixel = Pixel(IntArray(size) { it }, 0, 0x00ff00.withAlpha(255), chopper)
    world.pixels.add(samplePixel)

    // create UI
    var updateInterval = 1f / 20f
    val image = IntArray(size)
    testUI3("Creeper World") {
        StudioBase.instance?.enableVSync = true
        val texture = Texture2D("tex", w, h, 1)
        var nextUpdate = 0L
        var tickIndex = 0
        val panel = object : MapPanel(style) {
            override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
                super.onDraw(x0, y0, x1, y1)
                val time = Time.gameTimeN
                if (time >= nextUpdate) {

                    nextUpdate = time + (updateInterval * SECONDS_TO_NANOS).toLong()
                    // todo update all systems that need it
                    //  - rocks
                    //  - ships
                    //  - particles
                    // todo render all rock types and such...
                    // done:
                    //  - fluids

                    for (i in 0 until size) {
                        image[i] = rockColors[world.rockTypes[i]]
                    }

                    for (fluid in fluids) {
                        if (tickIndex % fluid.viscosity == 0) {
                            fluid.tick(world, sumByType)
                        }
                        // render fluid
                        fluid.data.render(image, fluid.color)
                    }

                    world.updatePixels()

                    for (pixel in world.pixels) {
                        val pos = pixel.path[pixel.progress]
                        image[pos] = mixARGB(image[pos], pixel.color, pixel.color.a())
                    }

                    for (agent in world.agents) {
                        agent.render(image)
                    }

                    switchRGB2BGR(image)
                    texture.createRGB(image, false)
                    tickIndex++
                }

                val xi = coordsToWindowX(0f).toInt()
                val yi = coordsToWindowY(0f).toInt()
                val wi = coordsToWindowDirX(texture.width.toDouble()).toInt()
                val hi = coordsToWindowDirY(texture.height.toDouble()).toInt()
                drawTexture(xi - wi / 2, yi - hi / 2, wi, hi, texture)

                val window = window!!
                val mx = floor(windowToCoordsX(window.mouseX) + w / 2).toInt()
                val my = floor(windowToCoordsY(window.mouseY) + h / 2).toInt()

                if (mx in 0 until w && my in 0 until h) {
                    val mi = mx + my * w
                    var yj = this.y + this.height - monospaceFont.sizeInt * fluids.size
                    for (fluid in fluids) {
                        drawSimpleTextCharByChar(
                            this.x, yj, 1,
                            "${fluid.data.level.read[mi]}, vx: ${fluid.data.impulseX.read[mi]}, vy: ${fluid.data.impulseY.read[mi]}"
                        )
                        yj += monospaceFont.sizeInt
                    }
                }

                var yj = this.y
                for (fluid in fluids) {
                    drawSimpleTextCharByChar(
                        this.x + this.width, yj, 1,
                        "${sumByType[fluid.id]}", AxisAlignment.MAX, AxisAlignment.MIN
                    )
                    yj += monospaceFont.sizeInt
                }
            }
        }
        panel
    }
}