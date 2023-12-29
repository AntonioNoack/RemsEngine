package me.anno.tests.game.creeperworld

import me.anno.Time
import me.anno.config.DefaultConfig.style
import me.anno.gpu.GFX.addGPUTask
import me.anno.gpu.drawing.DrawTexts.drawSimpleTextCharByChar
import me.anno.gpu.drawing.DrawTexts.monospaceFont
import me.anno.gpu.drawing.DrawTextures.drawTexture
import me.anno.gpu.texture.Texture2D
import me.anno.gpu.texture.Texture2D.Companion.switchRGB2BGR
import me.anno.image.ImageCache
import me.anno.input.Input
import me.anno.input.Key
import me.anno.io.files.FileReference.Companion.getReference
import me.anno.maths.Maths.SECONDS_TO_NANOS
import me.anno.maths.Maths.max
import me.anno.maths.noise.PerlinNoise
import me.anno.studio.StudioBase
import me.anno.tests.game.creeperworld.RockTypes.dissolved
import me.anno.tests.game.creeperworld.RockTypes.hardness
import me.anno.tests.game.creeperworld.RockTypes.rock
import me.anno.tests.game.creeperworld.RockTypes.stone
import me.anno.tests.physics.fluid.RWState
import me.anno.ui.base.components.AxisAlignment
import me.anno.ui.base.groups.MapPanel
import me.anno.ui.debug.TestStudio.Companion.testUI3
import me.anno.utils.Color.withAlpha
import me.anno.utils.hpc.ProcessingGroup
import me.anno.utils.types.Floats.f3
import org.joml.Vector2i
import org.joml.Vector4f
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.sqrt

// todo 2d or 3d creeper world game:
//  - rock layers
//  - fluid may dissolve weak rock
//  - fluid may flow through sand
//  - agents that move by dissolving into pixels
//  - deliver resources by things that float through the world
//  - deliver them from main ship
// done:
//  - agents that can influence (paint) fluid layers
//  - fluid layers
//  - fluids may interact with each other
//  - fluids flow

// todo possibility to be written by fluids?... hard... we'd need to transfer that data from GPU to CPU or simulate on CPU
//  -> but we want maximum flexibility, so let's do everything on the CPU, it's fast enough for 2d (?)...
//  and also just simulate at 5fps or so, and interpolate intermediate results; we may also tick different fluids at different rates to make them appear viscous :3

open class Layer(val id: String)
class RockProperty(val blockDefault: Float, val airDefault: Float)

val worker = ProcessingGroup("CreeperWorld", Runtime.getRuntime().availableProcessors())

fun validateNotOnWall(hardness: FloatArray, levels: FloatArray) {
    for (i in hardness.indices) {
        if (isSolid(hardness[i]) && levels[i] != 0f) {
            throw IllegalStateException("Illegal: fluid on rock")
        }
    }
}

fun forAllEdgePixels(world: CreeperWorld, processEdgePixel: (x: Int, y: Int, i: Int) -> Unit) {
    val w = world.w
    val h = world.h
    worker.processBalanced(0, max(w, h), 16) { i0, i1 ->
        for (y in i0 until min(h, i1)) {
            processEdgePixel(0, y, y * w)
            processEdgePixel(w - 1, y, (w - 1) + y * w)
        }
        for (x in max(1, i0) until min(w - 1, i1)) {
            processEdgePixel(x, 0, x)
            processEdgePixel(x, h - 1, x + w * (h - 1))
        }
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

class RockFramebuffer(world: CreeperWorld) {
    val value = FloatArray(world.size)
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

fun isSolid(hardness: Float): Boolean {
    return hardness > 0.1f
}

val sumByType = HashMap<String, String>()

fun main() {

    val world = CreeperWorld(512, 256)

    val w = world.w
    val h = world.h

    // define a world
    // todo use 2d perlin to place rock types

    world.register(hardness)
    world.register(dissolved)

    world.hardness = world[hardness]
    world.dissolved = world[dissolved]

    for (fluid in world.fluidTypes.fluids) {
        world.register(fluid)
    }

    // fill in some sample data
    val cl = world.fluidTypes.creeper.data.level.read
    val al = world.fluidTypes.antiCreeper.data.level.read
    val aa = world.fluidTypes.acid.data.level.read
    val dr = min(w, h) / 3
    for (j in -dr..dr) {
        for (i in -dr..dr) {
            val level = dr - sqrt(i * i + j * j + 0.5f)
            if (level > 0f) {
                val x0 = i + w / 3
                val x1 = i + w * 2 / 3
                val y = j + h / 2
                cl[x0 + y * w] += level / dr
                aa[x0 + y * w] += level / dr
                al[x1 + y * w] += level / dr
            }
        }
    }

    val height = PerlinNoise(
        1234L, 5, 0.5f,
        h * 5 / 6f, h * 6 / 6f,
        Vector4f(10f / w)
    )

    for (x in 0 until w) {
        val hi = height[x.toFloat()].toInt()
        for (y in hi until hi + 3) {
            world.setBlock(x, y, stone)
        }
        for (y in hi + 3 until h) {
            world.setBlock(x, y, rock)
        }
    }

    if (false) {
        forAllEdgePixels(world) { _, _, i ->
            world.setBlock(i, stone)
        }
    }

    // spawn a few pixels and agents
    // todo spawn an agent with an actual image,
    //  and then use some key to reposition it (drag?), and let's see how it moves :3
    val chopperImage = ImageCache[getReference("res://textures/Chopper.png"), false]!!
    val chopper = Cannon(chopperImage, world.fluidTypes.creeper.data)
    chopper.position.set(10, 10)
    world.agents.add(chopper)

    val samplePixel = AgentPixel(IntArray(world.size) { it }, 0x00ff00.withAlpha(255), chopper)
    world.add(samplePixel)

    val multiplierImage = ImageCache[getReference("res://textures/Multiplier.png"), false]!!
    val multiplier = Multiplier(
        multiplierImage, 2f, 2,
        listOf(world.fluidTypes.creeper.data, world.fluidTypes.antiCreeper.data)
    )
    multiplier.position.set(w * 5 / 6, height[w * 5 / 6f].toInt() - 10)
    world.agents.add(multiplier)

    val sink = Multiplier(
        multiplierImage, 0f, 2,
        listOf(world.fluidTypes.creeper.data, world.fluidTypes.antiCreeper.data)
    )
    sink.position.set(w * 1 / 6, height[w * 1 / 6f].toInt() - 10)
    world.agents.add(sink)

    // create UI
    val updateInterval = 1f / 60f
    val image = IntArray(world.size)
    testUI3("Creeper World") {
        StudioBase.instance?.enableVSync = true
        val texture = Texture2D("tex", w, h, 1)
        var nextUpdate = 0L
        var tickIndex = 0
        val panel = object : MapPanel(style) {

            override fun shallMoveMap() = Input.isRightDown

            override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
                super.onDraw(x0, y0, x1, y1)
                updateIfNeeded()
                drawWorldTexture()
                drawFluidAtCursor()
                drawFluidTotals()
            }

            var downX = 0
            var downY = 0
            var downA: Agent? = null

            override fun onKeyDown(x: Float, y: Float, key: Key) {
                super.onKeyDown(x, y, key)
                if (key == Key.BUTTON_LEFT) {
                    downX = floor(windowToCoordsX(x)).toInt()
                    downY = floor(windowToCoordsY(y)).toInt()
                    downA = null
                    for (agent in world.agents) {
                        if (agent.isVisibleAt(downX, downY)) {
                            downA = agent
                            break
                        }
                    }
                }
            }

            override fun onKeyUp(x: Float, y: Float, key: Key) {
                super.onKeyUp(x, y, key)
                val agent = downA
                if (key == Key.BUTTON_LEFT && agent != null) {
                    val x0 = downX
                    val y0 = downY
                    val x1 = floor(windowToCoordsX(x)).toInt()
                    val y1 = floor(windowToCoordsY(y)).toInt()
                    val moved = agent.moveTo(world, Vector2i(agent.position).add(x1 - x0, y1 - y0))
                    println("tried moving agent, success?: $moved")
                }
            }

            fun updateIfNeeded() {
                val time = Time.gameTimeN
                if (time >= nextUpdate) {
                    nextUpdate = Long.MAX_VALUE
                    worker += {
                        world.update(tickIndex)
                        world.render(image)
                        switchRGB2BGR(image)
                        addGPUTask("texUpdate", 10) {
                            texture.createRGB(image, false)
                            tickIndex++
                            nextUpdate = time + (updateInterval * SECONDS_TO_NANOS).toLong()
                        }
                    }
                }
            }

            fun drawWorldTexture() {
                if (texture.wasCreated) {
                    val xi = coordsToWindowX(0f).toInt()
                    val yi = coordsToWindowY(0f).toInt()
                    val wi = coordsToWindowDirX(texture.width.toDouble()).toInt()
                    val hi = coordsToWindowDirY(texture.height.toDouble()).toInt()
                    drawTexture(xi, yi, wi, hi, texture)
                }
            }

            fun drawFluidAtCursor() {

                val window = window!!
                val mx = floor(windowToCoordsX(window.mouseX)).toInt()
                val my = floor(windowToCoordsY(window.mouseY)).toInt()

                if (mx in 0 until w && my in 0 until h) {
                    val mi = mx + my * w
                    var yj = this.y + this.height - monospaceFont.sizeInt * (world.fluidTypes.fluids.size + 1)
                    drawSimpleTextCharByChar(
                        this.x, yj, 1,
                        "$mx, $my"
                    )
                    yj += monospaceFont.sizeInt
                    for (fluid in world.fluidTypes.fluids) {
                        drawSimpleTextCharByChar(
                            this.x, yj, 1,
                            "${fluid.id}: ${fluid.data.level.read[mi].f3()}, " +
                                    "vx: ${fluid.data.impulseX.read[mi].toInt()}, " +
                                    "vy: ${fluid.data.impulseY.read[mi].toInt()}"
                        )
                        yj += monospaceFont.sizeInt
                    }
                }
            }

            fun drawFluidTotals() {
                var yj = this.y
                for (fluid in world.fluidTypes.fluids) {
                    drawSimpleTextCharByChar(
                        this.x + this.width, yj, 1,
                        "${fluid.id}: ${sumByType[fluid.id]}", AxisAlignment.MAX, AxisAlignment.MIN
                    )
                    yj += monospaceFont.sizeInt
                }
            }
        }
        panel
    }
}