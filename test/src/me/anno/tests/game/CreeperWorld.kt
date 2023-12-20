package me.anno.tests.game

import me.anno.Time
import me.anno.config.DefaultConfig.style
import me.anno.gpu.drawing.DrawTexts.drawSimpleTextCharByChar
import me.anno.gpu.drawing.DrawTexts.monospaceFont
import me.anno.gpu.drawing.DrawTextures.drawTexture
import me.anno.gpu.texture.Texture2D
import me.anno.gpu.texture.Texture2D.Companion.switchRGB2BGR
import me.anno.image.Image
import me.anno.image.raw.IntImage
import me.anno.language.translation.NameDesc
import me.anno.maths.Maths.SECONDS_TO_NANOS
import me.anno.maths.Maths.max
import me.anno.maths.Maths.sq
import me.anno.maths.noise.PerlinNoise
import me.anno.maths.paths.PathFinding
import me.anno.studio.StudioBase
import me.anno.tests.physics.fluid.RWState
import me.anno.ui.base.components.AxisAlignment
import me.anno.ui.base.groups.MapPanel
import me.anno.ui.debug.TestStudio.Companion.testUI3
import me.anno.utils.Color.a
import me.anno.utils.Color.a01
import me.anno.utils.Color.mixARGB
import me.anno.utils.Color.withAlpha
import me.anno.utils.hpc.ProcessingGroup
import org.joml.Vector2i
import org.joml.Vector4f
import kotlin.math.abs
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
class FluidProcessor(val shader: FluidShader, val dependencies: List<RockProperty> = emptyList())
class RockProcessor(val shader: RockShader, val dependencies: List<RockProperty> = emptyList())

val worker = ProcessingGroup("CreeperWorld", Runtime.getRuntime().availableProcessors())

fun interface FluidShader {
    fun process(
        fluid: FluidFramebuffer,
        world: World,
    )
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

interface FluidShader2 : FluidShader {
    override fun process(
        fluid: FluidFramebuffer,
        world: World,
    ) {
        worker.processBalanced(1, h - 1, 8) { y0, y1 ->
            for (y in y0 until y1) {
                val i0 = y * w
                processInnerPixels(i0 + 1, i0 + w - 1, fluid, world)
            }
        }
        forAllEdgePixels { x, y, i ->
            processEdgePixel(x, y, i, fluid, world)
        }
    }

    fun processInnerPixels(
        i0: Int, i1: Int,
        fluid: FluidFramebuffer,
        world: World,
    )

    fun processEdgePixel(
        x: Int, y: Int, i: Int,
        fluid: FluidFramebuffer,
        world: World,
    )
}

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

class FluidFramebuffer {

    val level = RWState { FloatArray(size) } // 0..1: good, 1.. high pressure
    val impulseX = RWState { FloatArray(size) }
    val impulseY = RWState { FloatArray(size) }

    fun render(dst: IntArray, color: Int) {
        val alpha = color.a01()
        val pressure = level.read
        for (i in 0 until size) {
            val v = min(pressure[i], 1000f)
            if (v > 0f) {
                dst[i] = mixARGB(dst[i], color, alpha * (v / (v + 1f)))
            }
        }
    }
}

class FluidLayer(
    id: String,
    val steps: List<FluidProcessor>,
    val viscosity: Int,
    val color: Int
) : Layer(id) {
    val data = FluidFramebuffer()
}

/**
 * each rock type has different properties;
 * rock types define properties; they then get saved to the GPU in separate framebuffers;
 * which then get used by shaders
 * */
class RockPropertyLayer(
    id: String,
    val data: RWState<RockFramebuffer>,
    val tick: List<FluidProcessor>,
) : Layer(id) {

}

class RockType(
    val id: Int, val name: NameDesc,
    val properties: Map<RockProperty, Float>,
    val color: Int
)

val w = 8
val h = 8
val size = w * h

class FluidAccelerate : FluidShader {
    fun limitH(w: Float, f: Float): Float {
        return max(w - 1f, 0f) * f
    }

    override fun process(fluid: FluidFramebuffer, world: World) {

        val srcH = fluid.level.read
        val srcVX = fluid.impulseX.read
        val srcVY = fluid.impulseY.read
        val dstVX = fluid.impulseX.write
        val dstVY = fluid.impulseY.write

        val pressureDiff = -0.05f
        val gravity = 0.5f

        // add flow by gravity
        // add flow by height difference
        fun expensive(i: Int) {
            val srcH0 = srcH[i]
            dstVX[i] = srcVX[i] +
                    pressureDiff * limitH(srcH0, srcH.getOrElse(i + 1) { 0f } - srcH.getOrElse(i - 1) { 0f })
            dstVY[i] = srcVY[i] +
                    pressureDiff * limitH(srcH0, srcH.getOrElse(i + h) { 1e3f } + srcH.getOrElse(i - h) { 1e3f }) +
                    gravity * srcH0 // gravity
        }
        for (i in 0 until h + 1) {
            expensive(i)
        }
        for (i in h + 1 until size - h - 1) {
            val srcH0 = srcH[i]
            dstVX[i] = srcVX[i] + pressureDiff * limitH(srcH0, srcVX[i + 1] - srcVX[i - 1])
            dstVY[i] = srcVY[i] + pressureDiff * limitH(srcH0, srcVX[i + h] - srcVX[i - h]) + gravity * srcH0
        }
        for (i in h + 1 until size) {
            expensive(i)
        }

        // not working???
        forAllEdgePixels { _, _, i ->
            dstVX[i] = 0f
            dstVY[i] = 0f
        }

        fluid.impulseX.swap()
        fluid.impulseY.swap()
    }
}

fun isSolid(hardness: Float): Boolean {
    return hardness > 0.1f
}

class FluidClampVelocity(val hardness: FloatArray) : FluidShader2 {

    fun safeClamp(v: Float, min: Float, max: Float): Float {
        return if (v < min) min
        else if (v > max) max
        else if (v < max) v
        else 0f // NaN
    }

    override fun processEdgePixel(x: Int, y: Int, i: Int, fluid: FluidFramebuffer, world: World) {
        val srcH = fluid.level.read
        val srcVX = fluid.impulseX.read
        val srcVY = fluid.impulseY.read
        val dstVX = fluid.impulseX.write
        val dstVY = fluid.impulseY.write
        val h = srcH[i]
        dstVX[i] = safeClamp(srcVX[i], check(x - 1, y, -h), check(x + 1, y, h))
        dstVY[i] = safeClamp(srcVY[i], check(x, y - 1, -h), check(x, y + 1, h))
    }

    fun check(x: Int, y: Int, v: Float): Float {
        return if (x in 0 until w && y in 0 until h)
            check(x + y * w, v) else 0f
    }

    fun check(i: Int, v: Float): Float {
        return if (isSolid(hardness[i])) 0f else v
    }

    override fun processInnerPixels(i0: Int, i1: Int, fluid: FluidFramebuffer, world: World) {
        val srcH = fluid.level.read
        val srcVX = fluid.impulseX.read
        val srcVY = fluid.impulseY.read
        val dstVX = fluid.impulseX.write
        val dstVY = fluid.impulseY.write
        for (i in i0 until i1) {
            val h = srcH[i]
            dstVX[i] = safeClamp(srcVX[i], check(i - 1, -h), check(i + 1, h))
            dstVY[i] = safeClamp(srcVY[i], check(i - w, -h), check(i + w, h))
        }
    }

    override fun process(fluid: FluidFramebuffer, world: World) {
        super.process(fluid, world)
        fluid.impulseX.swap()
        fluid.impulseY.swap()
    }
}

class FluidMove(val hardness: FloatArray) : FluidShader2 {

    override fun process(fluid: FluidFramebuffer, world: World) {
        super.process(fluid, world)
        fluid.level.swap()
        fluid.impulseX.swap()
        fluid.impulseY.swap()
    }

    override fun processInnerPixels(i0: Int, i1: Int, fluid: FluidFramebuffer, world: World) {

        val srcH = fluid.level.read
        val srcVX = fluid.impulseX.read
        val srcVY = fluid.impulseY.read

        val dstH = fluid.level.write
        val dstVX = fluid.impulseX.write
        val dstVY = fluid.impulseY.write

        for (i in i0 until i1) {
            // based on transfer, transfer fluid and velocity
            if (!isSolid(hardness[i])) {
                var sumH = 0f
                var sumVX = 0f
                var sumVY = 0f
                for (dy in -1..1) {
                    for (dx in -1..1) {
                        val j = i + dx + dy * w
                        val level = srcH[j]
                        if (!(level > 0f)) continue // negative or no fluid
                        // calculate flow from that cell into this cell
                        val flowX = max(1f - abs(srcVX[j] / level + dx), 0f) // [0, +1]
                        val flowY = max(1f - abs(srcVY[j] / level + dy), 0f) // [0, +1]
                        val flow = flowX * flowY // [0, +1]
                        sumH += level * flow
                        sumVX += srcVX[j] * flow
                        sumVY += srcVY[j] * flow
                    }
                }
                dstH[i] = sumH
                dstVX[i] = sumVX
                dstVY[i] = sumVY
            }
        }
    }

    override fun processEdgePixel(x: Int, y: Int, i: Int, fluid: FluidFramebuffer, world: World) {
        val srcH = fluid.level.read
        val srcVX = fluid.impulseX.read
        val srcVY = fluid.impulseY.read
        if (!isSolid(hardness[i])) {

            val dstH = fluid.level.write
            val dstVX = fluid.impulseX.write
            val dstVY = fluid.impulseY.write

            var sumH = 0f
            var sumVX = 0f
            var sumVY = 0f
            for (dy in -1..1) {
                for (dx in -1..1) {
                    val xi = x + dx
                    val yi = y + dy
                    if (xi !in 0 until w || yi !in 0 until h) continue
                    val j = xi + yi * w
                    val level = srcH[j]
                    if (!(level > 0f)) continue // negative or no fluid
                    // calculate flow from that cell into this cell
                    val flowX = max(1f - abs(srcVX[j] / level + dx), 0f) // [0, +1]
                    val flowY = max(1f - abs(srcVY[j] / level + dy), 0f) // [0, +1]
                    val flow = flowX * flowY // [0, +1]
                    sumH += level * flow
                    sumVX += srcVX[j] * flow
                    sumVY += srcVY[j] * flow
                }
            }
            dstH[i] = sumH
            dstVX[i] = sumVX
            dstVY[i] = sumVY
        }
    }
}

class FluidBlur(val hardness: FloatArray) : FluidShader2 {

    val wei = 0.125f
    val revFriction = 0.9f

    override fun process(fluid: FluidFramebuffer, world: World) {
        super.process(fluid, world)
        fluid.level.swap()
        fluid.impulseX.swap()
        fluid.impulseY.swap()
    }

    override fun processEdgePixel(x: Int, y: Int, i: Int, fluid: FluidFramebuffer, world: World) {

        val dstH = fluid.level.write
        val dstVX = fluid.impulseX.write
        val dstVY = fluid.impulseY.write

        if (isSolid(hardness[i])) {
            var sumH = 0f
            var sumVX = 0f
            var sumVY = 0f
            var sumW = 0f

            val srcH = fluid.level.read
            val srcVX = fluid.impulseX.read
            val srcVY = fluid.impulseY.read

            val flow = wei
            fun addValue(x: Int, y: Int) {
                val j = x + y * w
                if (!isSolid(hardness[j])) {
                    sumH += srcH[j] * flow
                    sumVX += srcVX[j] * flow
                    sumVY += srcVY[j] * flow
                    sumW += flow
                }
            }

            if (x > 0) addValue(x - 1, y)
            if (y > 0) addValue(x, y - 1)
            if (x + 1 < w) addValue(x + 1, y)
            if (y + 1 < h) addValue(x, y + 1)

            val rem = 1f - sumW
            dstH[i] = srcH[i] * rem + sumH
            dstVX[i] = (srcVX[i] * rem + sumVX) * revFriction
            dstVY[i] = (srcVY[i] * rem + sumVY) * revFriction
        }
    }

    override fun processInnerPixels(i0: Int, i1: Int, fluid: FluidFramebuffer, world: World) {

        val srcH = fluid.level.read
        val srcVX = fluid.impulseX.read
        val srcVY = fluid.impulseY.read

        val dstH = fluid.level.write
        val dstVX = fluid.impulseX.write
        val dstVY = fluid.impulseY.write

        val flow = wei
        for (i in i0 until i1) {
            if (!isSolid(hardness[i])) {
                var sumH = 0f
                var sumVX = 0f
                var sumVY = 0f
                var sumW = 0f
                fun addValue(dx: Int, dy: Int) {
                    val j = i + dx + dy * w
                    if (!isSolid(hardness[j])) {
                        sumH += srcH[j] * flow
                        sumVX += srcVX[j] * flow
                        sumVY += srcVY[j] * flow
                        sumW += flow
                    }
                }
                addValue(-1, 0)
                addValue(+1, 0)
                addValue(0, -1)
                addValue(0, +1)
                val rem = 1f - sumW
                dstH[i] = srcH[i] * rem + sumH
                dstVX[i] = (srcVX[i] * rem + sumVX) * revFriction
                dstVY[i] = (srcVY[i] * rem + sumVY) * revFriction
            }
        }
    }
}

class Pixel(
    val path: IntArray,
    var progress: Int,
    val color: Int,
    val agent: Agent
)

class World {

    val rockTypes = IntArray(size)
    val properties = HashMap<RockProperty, FloatArray>()
    val fluids = HashMap<String, FluidFramebuffer>()

    val pixelIsSet = BooleanArray(size)
    val pixels = ArrayList<Pixel>()

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

abstract class Agent(
    val pixels: Image,
    val position: Vector2i
) {
    // todo fire logic and such...
    abstract fun onUpdate()

    val completeState = (0 until pixels.width * pixels.height).count {
        pixels.getRGB(it).a() > 0
    }
    var loadingState = completeState

    fun moveTo(world: World, newPos: Vector2i): Boolean {
        // check if pixels can find path
        // dissolve into pixels
        val oldPos = position
        // todo first check if all new positions are valid (non-rock)
        for (dy in 0 until pixels.height) {
            for (dx in 0 until pixels.width) {
                val color = pixels.getRGB(dx, dy)
                if (color.a() == 0) continue
                val path = findPixelPath(
                    world.rockTypes, // todo we need an array of is-solid instead, kind of...
                    Vector2i(oldPos.x + dx, oldPos.y + dy),
                    Vector2i(newPos.x + dx, newPos.y + dy)
                )
                if (path != null) {
                    world.pixels.add(Pixel(path, 0, color, this))
                    loadingState--
                } else return false
            }
        }
        // spawn into new location
        oldPos.set(newPos)
        // wait for pixels to appear...
        return true
    }

    // todo pixels: particles that use path finding, but must wait for each other to have space to move
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
            FluidProcessor(FluidAccelerate()),
            FluidProcessor(FluidClampVelocity(world[hardness])),
            FluidProcessor(FluidMove(world[hardness])),
            FluidProcessor(FluidBlur(world[hardness])),
        ), 1, 0x33ff33.withAlpha(200)
    )
    val acid = FluidLayer("acid", listOf(), 2, 0xff8833.withAlpha(100))
    val antiCreeper = FluidLayer("antiCreeper", listOf(), 3, 0x3377ff.withAlpha(100))
    val lava = FluidLayer("lava", listOf(), 10, 0xffaa00.withAlpha(255))
    val water = FluidLayer("water", listOf(), 1, 0x99aaff.withAlpha(50))
    val fluids = listOf(creeper)

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
    val dr = min(w, h) / 8
    for (j in -dr..dr) {
        for (i in -dr..dr) {
            val level = dr - sqrt(i * i + j * j + 0.5f)
            if (level > 0f) {
                val x = i + w / 2
                val y = j + h * 3 / 4
                cl[x + y * w] += 5000f * level / dr
            }
        }
    }

    cl[(w / 2) + (h / 2) * w] = 5000f

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

    forAllEdgePixels { _, _, i ->
        world.setBlock(i, stone)
    }

    val sumByType = HashMap<String, String>()

    // spawn a few pixels and agents
    // todo spawn an agent with an actual image,
    //  and then use some key to reposition it (drag?), and let's see how it moves :3
    val agent = object : Agent(IntImage(1, 1, false), Vector2i()) {
        override fun onUpdate() {}
    }
    val pixel = Pixel(IntArray(size) { it }, 0, 0x00ff00.withAlpha(255), agent)
    world.pixels.add(pixel)

    // create UI
    var updateInterval = 1f / 5f
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
                            for (step in fluid.steps) {
                                step.shader.process(fluid.data, world)
                                // todo how/where is the fluid multiplying?
                                sumByType[fluid.id] = "F: ${fluid.data.level.read.sum()}, E: ${
                                    (fluid.data.impulseX.read.sumOf { sq(it.toDouble()) } +
                                            fluid.data.impulseY.read.sumOf { sq(it.toDouble()) }).toFloat()
                                }"
                            }
                        }
                        // render fluid
                        fluid.data.render(image, fluid.color)
                    }

                    world.updatePixels()

                    for (pixel in world.pixels) {
                        val pos = pixel.path[pixel.progress]
                        image[pos] = mixARGB(image[pos], pixel.color, pixel.color.a())
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