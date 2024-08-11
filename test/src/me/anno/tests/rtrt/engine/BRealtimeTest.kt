package me.anno.tests.rtrt.engine

import me.anno.Time
import me.anno.config.DefaultConfig.style
import me.anno.ecs.Entity
import me.anno.ecs.components.camera.Camera
import me.anno.ecs.components.camera.control.OrbitControls
import me.anno.ecs.components.mesh.MeshCache
import me.anno.engine.EngineBase
import me.anno.engine.OfficialExtensions
import me.anno.engine.raycast.RayHit
import me.anno.gpu.GFX
import me.anno.gpu.GFXState.blendMode
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.RenderDoc.forceLoadRenderDoc
import me.anno.gpu.blending.BlendMode
import me.anno.gpu.buffer.ComputeBuffer
import me.anno.gpu.buffer.SimpleBuffer.Companion.flat01
import me.anno.gpu.drawing.DrawTexts
import me.anno.gpu.drawing.DrawTextures
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.query.GPUClockNanos
import me.anno.gpu.shader.ComputeShader
import me.anno.gpu.shader.ComputeTextureMode
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.GPUShader
import me.anno.gpu.shader.Reduction
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderLib
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.gpu.texture.Texture2D
import me.anno.input.Input
import me.anno.maths.Maths
import me.anno.maths.Maths.SECONDS_TO_NANOS
import me.anno.maths.Maths.clamp
import me.anno.maths.Maths.fract
import me.anno.maths.Maths.max
import me.anno.maths.bvh.BLASNode
import me.anno.maths.bvh.BLASNode.Companion.PIXELS_PER_TRIANGLE
import me.anno.maths.bvh.BLASNode.Companion.PIXELS_PER_VERTEX
import me.anno.maths.bvh.BLASNode.Companion.createBLASBuffer
import me.anno.maths.bvh.BLASNode.Companion.createBLASTexture
import me.anno.maths.bvh.BLASNode.Companion.createTriangleBuffer
import me.anno.maths.bvh.BLASNode.Companion.createTriangleTexture
import me.anno.maths.bvh.BVHBuilder
import me.anno.maths.bvh.BVHNode
import me.anno.maths.bvh.RayGroup
import me.anno.maths.bvh.SplitMethod
import me.anno.maths.bvh.TLASNode
import me.anno.ui.Panel
import me.anno.ui.base.SpyPanel
import me.anno.ui.base.groups.PanelGroup
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.custom.CustomList
import me.anno.ui.debug.TestDrawPanel
import me.anno.ui.debug.TestEngine.Companion.testUI3
import me.anno.utils.Clock
import me.anno.utils.Color.convertABGR2ARGB
import me.anno.utils.Color.mixARGB
import me.anno.utils.Color.toRGB
import me.anno.utils.OS.documents
import me.anno.utils.hpc.ProcessingGroup
import me.anno.utils.hpc.threadLocal
import me.anno.utils.pooling.ByteBufferPool
import me.anno.utils.pooling.JomlPools
import me.anno.utils.structures.lists.Lists.any2
import org.apache.logging.log4j.LogManager
import org.joml.AABBf
import org.joml.JomlMath.invsqrt
import org.joml.Quaternionf
import org.joml.Vector3f
import java.nio.ByteBuffer
import java.nio.IntBuffer
import kotlin.math.pow

var drawMode = DrawMode.NORMAL

val localResult = threadLocal { RayHit() }
const val sky0 = 0x2f5293
const val sky1 = 0x5c729b
val sky0BGR = convertABGR2ARGB(sky0)
val sky1BGR = convertABGR2ARGB(sky1)

fun main() {
    OfficialExtensions.initForTests()
    val clock = Clock("BRealtimeTest")
    forceLoadRenderDoc()
    val meshSource = documents.getChild("monkey.obj")
    val mesh = MeshCache[meshSource]!!
    clock.stop("Loading mesh")
    val blas = BVHBuilder.buildBLAS(mesh, SplitMethod.MEDIAN_APPROX, 8)!!
    clock.stop("Building BLAS")
    main2(blas, Vector3f(), Quaternionf(), 1f)
}

fun hitToColor(normal: Vector3f, dir: Vector3f, tlas: Int, blas: Int, tris: Int): Int {
    return when (drawMode) {
        DrawMode.NORMAL -> {
            val lenSq = normal.lengthSquared()
            return if (lenSq > 0f) {
                normal.mul(0.5f * invsqrt(lenSq))
                normal.add(0.5f, 0.5f, 0.5f)
                normal.set(normal.z, normal.y, normal.x).toRGB() // reversed
            } else mixARGB(sky0BGR, sky1BGR, dir.y * 0.5f + 0.5f)
        }
        DrawMode.TLAS_DEPTH -> coloring1(tlas * 0.1f)
        DrawMode.BLAS_DEPTH -> coloring1(blas * 0.1f)
        DrawMode.TRIS_DEPTH -> coloring1(tris * 0.1f)
        else -> 0
    }
}

fun coloring1(t: Float): Int {
    val base = fract(t) * .8f + .2f
    // reversed colors
    val a = 0xea8261
    val b = 0x4b5edc
    val c = 0xdbdcdd
    val f = clamp(t * 0.05f, 0f, 2f)
    return mixARGB(0, if (f < 1f) mixARGB(a, b, f) else mixARGB(b, c, f - 1f), base)
}

fun createControls(
    cameraPosition: Vector3f,
    cameraRotation: Quaternionf,
    bvhBounds: AABBf,
    main: PanelGroup
): OrbitControls {

    val controls = OrbitControls()
    val camera = Camera()
    val camEntity = Entity()
    val base = Entity()

    camEntity.add(camera)
    base.add(camEntity)
    base.add(controls)

    controls.camera = camera
    controls.position.set(cameraPosition)
    controls.radius = bvhBounds.volume.pow(1f / 3f).toDouble()
    controls.movementSpeed = 0.20 * controls.radius * 100
    controls.rotationSpeed = 0.15
    controls.friction = 20f

    val window = GFX.someWindow
    var lx = window.mouseX
    var ly = window.mouseY
    var lz = Input.mouseWheelSumY
    main.addChild(SpyPanel(style) {
        if (window.windowStack.inFocus.any2 { it is TestDrawPanel }) {
            if (Input.isRightDown) {
                val dx = window.mouseX - lx
                val dy = window.mouseY - ly
                controls.onMouseMoved(0f, 0f, dx, dy)
            }
            if (Input.wasKeyPressed('v')) {
                EngineBase.instance?.toggleVsync()
            }
            val dw = Input.mouseWheelSumY - lz
            controls.onMouseWheel(0f, 0f, 0f, dw, true)
        }
        lx = window.mouseX
        ly = window.mouseY
        lz = Input.mouseWheelSumY
        controls.onUpdate()
        controls.throwWarning()
        base.validateTransform()
        camEntity.validateTransform()
        cameraPosition.set(camEntity.transform.globalPosition)
        cameraRotation.set(camEntity.transform.globalRotation)
    })

    return controls
}

fun fillTile(
    x0: Int, x1: Int, y0: Int, y1: Int, group: RayGroup, bvh: BVHNode,
    cx: Float, cy: Float, fovZ: Float, maxDistanceF: Float,
    camPos2: Vector3f, camRot2: Quaternionf,
    ints: IntBuffer, w: Int, alpha: Float,
) {
    // todo center primary ray
    val tileSize = max(2, max(x1 - x0, y1 - y0))
    val tsm1 = 1f / (tileSize - 1)
    group.size = (y1 - y0) * (x1 - x0)
    group.local?.size = group.size
    val dir = JomlPools.vec3f.create()
    dir.set(x0 - cx, cy - y0, fovZ).normalize()
    // define ray position & main ray
    group.setMain(camPos2, camRot2.transform(dir), maxDistanceF)
    // define ray gradients by sample at (tileSize-1,0) and (0,tileSize-1)
    dir.set(x0 + tileSize - 1 - cx, cy - y0, fovZ).normalize()
    group.setDx(camRot2.transform(dir))
    dir.set(x0 - cx, cy - (y0 + tileSize - 1), fovZ).normalize()
    group.setDy(camRot2.transform(dir))
    var k = 0
    for (j in 0 until y1 - y0) {
        val y = y0 + j
        val dysK = j * tsm1
        for (i in 0 until x1 - x0) {
            val x = x0 + i
            // define ray
            dir.set(x - cx, cy - y, fovZ).normalize()
            camRot2.transform(dir)
            group.setRay(k, dir)
            group.dxs[k] = i * tsm1
            group.dys[k] = dysK
            k++
        }
    }
    group.tlasCtr = 0
    group.blasCtr = 0
    group.trisCtr = 0
    group.hit.tlasCtr = 0
    group.hit.blasCtr = 0
    group.hit.trisCtr = 0
    bvh.findClosestHit(group)
    val normal = JomlPools.vec3f.create()
    k = 0
    val tlas = group.tlasCtr + group.hit.tlasCtr
    val blas = group.blasCtr + group.hit.blasCtr
    val tris = group.trisCtr + group.hit.trisCtr
    for (y in y0 until y1) {
        var i = x0 + y * w
        for (x in x0 until x1) {
            normal.set(group.normalSX[k], group.normalSY[k], group.normalSZ[k])
            val color = hitToColor(normal, group.dir, tlas, blas, tris)
            ints.put(i, mixARGB(ints[i], color, alpha))
            k++
            i++
        }
    }
    JomlPools.vec3f.sub(2)
}

val pipeline = ProcessingGroup("brt", 1f)
fun createCPUPanel(
    scale: Int,
    cameraPosition: Vector3f,
    cameraRotation: Quaternionf,
    fovZFactor: Float,
    bvh: BVHNode,
    controls: OrbitControls,
    useGroups: Boolean,
): Panel {

    var cpuSpeed = -1L
    var cpuFPS = 0L

    val cpuTexture by lazy {
        Texture2D("cpu", 1, 1, 1)
            .apply { createRGBA() }
    }

    val tileSize = 4
    val groups = threadLocal { RayGroup(tileSize, tileSize, RayGroup(tileSize, tileSize)) }
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
        val cx = Maths.random().toFloat() - 0.5f + (parent.width * 0.5f - it.x) / scale
        val cy = Maths.random().toFloat() - 0.5f + (parent.height * 0.5f) / scale
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

        val alpha = 1f / (frameIndex + 1f)

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
                val maxDistance = 1e10
                val maxDistanceF = maxDistance.toFloat()
                pipeline.processBalanced2d(0, 0, w, h, tileSize, 1) { x0, y0, x1, y1 ->
                    if (useGroups) {
                        fillTile(
                            x0, x1, y0, y1, groups.get(), bvh,
                            cx, cy, fovZ, maxDistanceF,
                            lastPos, lastRot, ints, w, alpha
                        )
                    } else {
                        val dir = JomlPools.vec3f.create()
                        val nor = JomlPools.vec3f.create()
                        for (y in y0 until y1) {
                            var i = x0 + y * w
                            for (x in x0 until x1) {
                                dir.set(x - cx, cy - y, fovZ).normalize()
                                lastRot.transform(dir)
                                val hit = localResult.get()
                                hit.shadingNormalWS.set(0.0)
                                hit.distance = maxDistance
                                hit.tlasCtr = 0
                                hit.blasCtr = 0
                                hit.trisCtr = 0
                                bvh.findClosestHit(lastPos, dir, hit)
                                val color =
                                    hitToColor(nor.set(hit.shadingNormalWS), dir, hit.tlasCtr, hit.blasCtr, hit.trisCtr)
                                ints.put(i, mixARGB(ints[i], color, alpha))
                                i++
                            }
                        }
                        JomlPools.vec3f.sub(2)
                    }
                }
                val t1 = Time.nanoTime
                dt += t1 - t0
                frameIndex++
                cpuSpeed = dt / max(1L, w * h * frameIndex.toLong())
                cpuFPS = SECONDS_TO_NANOS * frameIndex / dt
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

val drawShader = Shader(
    "draw", emptyList(), ShaderLib.coordsUVVertexShader, ShaderLib.uvList, listOf(
        Variable(GLSLType.V1B, "enableToneMapping"),
        Variable(GLSLType.V1F, "brightness"),
        Variable(GLSLType.S2D, "tex"),
        Variable(GLSLType.V4F, "result", VariableMode.OUT)
    ), "" +
            "void main(){\n" +
            "   vec3 color = texture(tex,uv).rgb;\n" +
            "   if(enableToneMapping) color /= (color + brightness);\n" +
            "   result = vec4(color, 1.0);\n" +
            "}"
)

fun createGPUPanel(
    scale: Int,
    cameraPosition: Vector3f,
    cameraRotation: Quaternionf,
    fovZFactor: Float,
    bvh: BVHNode,
    controls: OrbitControls,
    useComputeShader: Boolean,
    useComputeBuffer: Boolean
): Panel {

    val avgBuffer = Framebuffer("avg", 1, 1, 1, TargetType.Float32x4, DepthBufferType.NONE)

    val lastPos = Vector3f()
    val lastRot = Quaternionf()
    var lastDrawMode: DrawMode? = null

    val clockNanos = GPUClockNanos()

    var frameIndex = 0
    fun prepareShader(shader: GPUShader, it: Panel) {

        it.clone()

        val w = it.width / scale
        val h = it.height / scale

        // if camera was moved, set frameIndex back to zero
        if (lastDrawMode != drawMode ||
            lastPos.distance(cameraPosition) > controls.radius * 0.01f ||
            lastRot.difference(cameraRotation, JomlPools.quat4f.borrow()).angle() > 1e-3f
        ) {
            lastPos.set(cameraPosition)
            lastRot.set(cameraRotation)
            lastDrawMode = drawMode
            frameIndex = 0
            clockNanos.scaleWeight()
        }

        if (avgBuffer.width != w || avgBuffer.height != h) {
            avgBuffer.width = w
            avgBuffer.height = h
            avgBuffer.destroy()
            avgBuffer.create()
            frameIndex = 0
            clockNanos.scaleWeight()
        }

        val parent = it.uiParent!!
        val cx = Maths.random().toFloat() - 0.5f + (parent.width * 0.5f - it.x) / scale
        val cy = Maths.random().toFloat() - 0.5f + (parent.height * 0.5f) / scale - 1 // why ever -1 is needed...
        val fovZ = -parent.height * fovZFactor / scale

        shader.use()
        shader.v2i("size", w, h)
        shader.v3f("cameraPosition", cameraPosition)
        shader.v4f("cameraRotation", cameraRotation)
        shader.v3f("cameraOffset", cx, cy, fovZ)
        shader.v3f("sky0", sky0)
        shader.v3f("sky1", sky1)
        shader.v1i("drawMode", drawMode.id)
        shader.v1i("frameIndex", frameIndex)
        shader.v1f("alpha", 1f / (frameIndex + 1f))
    }

    fun drawResult(it: Panel, prefix: String) {

        val tex = avgBuffer.getTexture0()

        val drawMode = lastDrawMode
        val enableAutoExposure = drawMode == DrawMode.GLOBAL_ILLUMINATION

        // draw with auto-exposure
        val brightness = if (enableAutoExposure)
            max(Reduction.reduce(tex, Reduction.MAX).x, 1e-7f) else 1f
        drawShader.use()
        drawShader.v1f("brightness", 0.2f * brightness)
        drawShader.v1b("enableToneMapping", enableAutoExposure)
        tex.bindTrulyNearest(0)
        flat01.draw(drawShader)

        val gpuFPS = SECONDS_TO_NANOS / max(1, clockNanos.average)
        DrawTexts.drawSimpleTextCharByChar(it.x + 4, it.y + it.height - 50, 2, "$prefix: $gpuFPS fps, $frameIndex spp")
    }

    if (useComputeShader) {
        val shader: ComputeShader
        if (useComputeBuffer) {
            val triangles: ComputeBuffer
            val blasNodes: ComputeBuffer
            val tlasNodes0: ComputeBuffer?
            val tlasNodes1: ComputeBuffer?
            when (bvh) {
                is TLASNode -> {
                    val (shader1, buffers) = createTLASBufferComputeShader(bvh)
                    shader = shader1
                    triangles = buffers[0]
                    blasNodes = buffers[1]
                    tlasNodes0 = buffers[2]
                    tlasNodes1 = buffers[3]
                }
                is BLASNode -> {
                    triangles = createTriangleBuffer(listOf(bvh), PIXELS_PER_VERTEX)
                    blasNodes = createBLASBuffer(listOf(bvh))
                    shader = createBLASBufferComputeShader(bvh.maxDepth())
                    tlasNodes0 = null
                    tlasNodes1 = null
                }
                else -> throw IllegalStateException()
            }
            return TestDrawPanel {
                prepareShader(shader, it)
                if (frameIndex < 256) {
                    val avgTexture = avgBuffer.getTexture0()
                    shader.bindBuffer(0, triangles)
                    shader.bindBuffer(1, blasNodes)
                    if (tlasNodes0 != null) shader.bindBuffer(2, tlasNodes0)
                    if (tlasNodes1 != null) shader.bindBuffer(3, tlasNodes1)
                    shader.bindTexture(4, avgTexture as Texture2D, ComputeTextureMode.READ_WRITE)
                    clockNanos.start()
                    shader.runBySize(avgBuffer.width, avgBuffer.height)
                    clockNanos.stop()
                    frameIndex++
                }
                drawResult(it, "CB")
            }
        } else {
            val triangles: Texture2D
            val blasNodes: Texture2D
            val tlasNodes: Texture2D?
            when (bvh) {
                is TLASNode -> {
                    val (shader1, triangles1, blasNodes1, tlasNodes1) = createTLASTextureComputeShader(bvh)
                    shader = shader1
                    triangles = triangles1
                    blasNodes = blasNodes1
                    tlasNodes = tlasNodes1
                }
                is BLASNode -> {
                    triangles = createTriangleTexture(listOf(bvh), PIXELS_PER_VERTEX)
                    blasNodes = createBLASTexture(listOf(bvh), PIXELS_PER_TRIANGLE)
                    shader = createBLASTextureComputeShader(bvh.maxDepth())
                    tlasNodes = null
                }
                else -> throw IllegalStateException()
            }
            return TestDrawPanel {
                prepareShader(shader, it)
                if (frameIndex < 256) {
                    val avgTexture = avgBuffer.getTexture0()
                    shader.bindTexture(0, triangles, ComputeTextureMode.READ)
                    shader.bindTexture(1, blasNodes, ComputeTextureMode.READ)
                    if (tlasNodes != null) shader.bindTexture(2, tlasNodes, ComputeTextureMode.READ)
                    shader.bindTexture(3, avgTexture as Texture2D, ComputeTextureMode.READ_WRITE)
                    clockNanos.start()
                    shader.runBySize(avgBuffer.width, avgBuffer.height)
                    clockNanos.stop()
                    frameIndex++
                }
                drawResult(it, "CT")
            }
        }
    } else {

        val shader: Shader
        val triangles: Texture2D
        val blasNodes: Texture2D
        val tlasNodes: Texture2D?

        when (bvh) {
            is TLASNode -> {
                val (shader1, meshes) = createTLASTextureGraphicsShader(bvh)
                shader = shader1
                triangles = createTriangleTexture(meshes, PIXELS_PER_VERTEX)
                blasNodes = createBLASTexture(meshes, PIXELS_PER_TRIANGLE)
                tlasNodes = bvh.createTLASTexture()
            }
            is BLASNode -> {
                triangles = createTriangleTexture(listOf(bvh), PIXELS_PER_VERTEX)
                blasNodes = createBLASTexture(listOf(bvh), PIXELS_PER_TRIANGLE)
                shader = createBLASTextureGraphicsShader(bvh)
                tlasNodes = null
            }
            else -> throw IllegalStateException()
        }

        return TestDrawPanel {
            useFrame(avgBuffer) {
                blendMode.use(BlendMode.DEFAULT) {
                    prepareShader(shader, it)
                    if (frameIndex < 256) {
                        triangles.bindTrulyNearest(0)
                        blasNodes.bindTrulyNearest(1)
                        tlasNodes?.bindTrulyNearest(2)
                        clockNanos.start()
                        flat01.draw(shader)
                        clockNanos.stop()
                        frameIndex++
                    }
                }
            }
            drawResult(it, "GT")
        }
    }
}

fun main2(
    bvh: BLASNode,
    cameraPosition: Vector3f,
    cameraRotation: Quaternionf,
    fovZFactor: Float
) {
    LogManager.disableLogger("WorkSplitter")
    testUI3("BLAS - Realtime") {

        val main = PanelListY(style)
        val controls = createControls(cameraPosition, cameraRotation, bvh.bounds, main)

        val list = CustomList(false, style)

        val scale = 1
        list.add(createCPUPanel(scale, cameraPosition, cameraRotation, fovZFactor, bvh, controls, true))
        list.add(createCPUPanel(scale, cameraPosition, cameraRotation, fovZFactor, bvh, controls, false))

        val ucs = true
        // with monkey and two spheres: 340 fps, 570 fps, 540 fps on RTX 3070
        list.add(createGPUPanel(scale, cameraPosition, cameraRotation, fovZFactor, bvh, controls, ucs, false))
        list.add(createGPUPanel(scale, cameraPosition, cameraRotation, fovZFactor, bvh, controls, ucs, true))
        list.add(createGPUPanel(scale, cameraPosition, cameraRotation, fovZFactor, bvh, controls, !ucs, false))

        list.weight = 1f
        main.add(list)
        main.add(typeInput())
        main

    }
}