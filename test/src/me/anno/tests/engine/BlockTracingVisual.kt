package me.anno.tests.engine

import me.anno.Time
import me.anno.config.DefaultConfig.style
import me.anno.ecs.components.camera.control.OrbitControls
import me.anno.engine.raycast.BlockTracing
import me.anno.engine.raycast.RayQuery
import me.anno.gpu.GFX
import me.anno.gpu.drawing.DrawTexts
import me.anno.gpu.drawing.DrawTextures
import me.anno.gpu.texture.Texture2D
import me.anno.maths.Maths
import me.anno.maths.Maths.SECONDS_TO_NANOS
import me.anno.maths.Maths.max
import me.anno.tests.rtrt.engine.DrawMode
import me.anno.tests.rtrt.engine.createControls
import me.anno.tests.rtrt.engine.drawMode
import me.anno.tests.rtrt.engine.pipeline
import me.anno.tests.utils.TestWorld
import me.anno.ui.Panel
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.debug.TestDrawPanel
import me.anno.ui.debug.TestEngine.Companion.testUI3
import me.anno.utils.Color.black
import me.anno.utils.Color.mixARGB
import me.anno.utils.hpc.ThreadLocal2
import me.anno.utils.pooling.ByteBufferPool
import me.anno.utils.pooling.JomlPools
import org.joml.AABBf
import org.joml.AABBi
import org.joml.Quaternionf
import org.joml.Vector3d
import org.joml.Vector3f
import java.nio.ByteBuffer
import java.nio.IntBuffer
import kotlin.math.floor

fun interface PixelRenderer {
    fun render(pos: Vector3f, dir: Vector3f): Int
}

fun main() {

    val cameraPosition = Vector3f(64f, 16f, 64f)
    val cameraRotation = Quaternionf()

    val main = PanelListY(style)
    val controls = createControls(
        cameraPosition, cameraRotation, AABBf(
            -30f, -30f, -30f,
            30f, 30f, 30f
        ), main
    )

    val world = TestWorld()
    val bounds = AABBi(0, 0, 0, 127, 31, 127)

    val queries = ThreadLocal2 { RayQuery(Vector3d(), Vector3d(), 1e3) }
    val blockGetter = BlockTracing.BlockChecker { x, y, z ->
        if (world.getElementAt(x, y, z).toInt() != 0) -0.5 else 0.5
    }

    val skyColor = 0x84bee5 or black
    val sunDirInv = Vector3d(1.0, -0.9, 0.3).normalize()
    val ui = createCPUPanel(
        4, cameraPosition, cameraRotation, 1f,
        controls, false
    ) { start, dir ->
        val query = queries.get()
        query.start.set(start)
        query.direction.set(dir)
        query.result.distance = 1e3
        if (BlockTracing.blockTrace(query, 1000, bounds, blockGetter)) {
            val pos = query.result.positionWS
            val nor = query.result.geometryNormalWS
            val x = floor(pos.x - nor.x * 0.5).toInt()
            val y = floor(pos.y - nor.y * 0.5).toInt()
            val z = floor(pos.z - nor.z * 0.5).toInt()
            val block = world.getElementAt(x, y, z).toInt()
            val color = world.palette[block]
            // get block color, and tint by normal
            mixARGB(black, color, sunDirInv.dot(nor).toFloat() * 0.1f + 0.9f)
        } else skyColor
    }.fill(1f)
    testUI3("BlockTracing", main.add(ui))
}

fun createCPUPanel(
    scale: Int,
    cameraPosition: Vector3f,
    cameraRotation: Quaternionf,
    fovZFactor: Float,
    controls: OrbitControls,
    randomizePixels: Boolean,
    renderer: PixelRenderer
): Panel {

    var cpuSpeed = -1L
    var cpuFPS = 0L

    val cpuTexture by lazy {
        Texture2D("cpu", 1, 1, 1)
            .apply { createRGBA() }
    }

    val tileSize = 4
    var frameIndex = 0
    var lastDrawMode: DrawMode? = null
    val lastPos = Vector3f()
    val lastRot = Quaternionf()
    var lastW = 0
    var lastH = 0
    var isRendering = false
    var cpuBuffer: IntBuffer? = null
    var cpuBytes: ByteBuffer? = null
    var dt = 0L
    return TestDrawPanel {

        it.clear()

        // render cpu side
        // render at lower resolution because of performance
        val w = it.width / scale
        val h = it.height / scale

        val parent = it.uiParent!!
        val rs = if (randomizePixels) 1f else 0f
        val cx = (Maths.random().toFloat() - 0.5f) * rs + (parent.width * 0.5f - it.x) / scale
        val cy = (Maths.random().toFloat() - 0.5f) * rs + (parent.height * 0.5f) / scale
        val fovZ = -parent.height * fovZFactor / scale

        // if camera was moved, set frameIndex back to zero
        if (lastDrawMode != drawMode ||
            lastPos.distance(cameraPosition) > controls.radius * 0.01f ||
            lastRot.difference(cameraRotation, JomlPools.quat4f.borrow()).angle() > 1e-3f
        ) {
            lastPos.set(cameraPosition)
            lastRot.set(cameraRotation)
            lastDrawMode = drawMode
            frameIndex = 0
            dt = 0L
        }

        if (lastW != w || lastH != h) {
            lastW = w
            lastH = h
            frameIndex = 0
            dt = 0L
        }

        fun nextFrame() {
            if (isRendering || frameIndex >= 256) return
            isRendering = true
            pipeline += {

                val size = w * h * 4
                cpuBytes = if (cpuBytes?.capacity() != size) {
                    ByteBufferPool.free(cpuBytes)
                    val bytes = ByteBufferPool.allocateDirect(size)
                    cpuBuffer = bytes.asIntBuffer()
                    bytes
                } else cpuBytes!!

                val ints = cpuBuffer!!
                val t0 = Time.nanoTime
                pipeline.processBalanced2d(0, 0, w, h, tileSize, 1) { x0, y0, x1, y1 ->
                    val dir = JomlPools.vec3f.create()
                    for (y in y0 until y1) {
                        var i = x0 + y * w
                        for (x in x0 until x1) {
                            dir.set(x - cx, cy - y, fovZ).normalize()
                            lastRot.transform(dir)
                            ints.put(i++, renderer.render(lastPos, dir))
                        }
                    }
                    JomlPools.vec3f.sub(1)
                }
                val t1 = Time.nanoTime
                dt += t1 - t0
                frameIndex++
                cpuSpeed = dt / max(1L, w * h * frameIndex.toLong())
                cpuFPS = SECONDS_TO_NANOS * frameIndex / max(1L, dt)
                GFX.addGPUTask("brt-cpu", 1) {
                    cpuTexture.width = w
                    cpuTexture.height = h
                    cpuTexture.createRGBA(ints, false)
                    isRendering = false
                }
            }
        }

        nextFrame()

        DrawTextures.drawTexture(it.x, it.y, it.width, it.height, cpuTexture, true, -1, null)
        DrawTexts.drawSimpleTextCharByChar(
            it.x + 4,
            it.y + it.height - 50,
            2,
            "$cpuSpeed ns/e, $cpuFPS fps, $frameIndex spp"
        )

    }
}
