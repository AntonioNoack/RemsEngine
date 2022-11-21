package me.anno.maths.bvh

import me.anno.config.DefaultConfig.style
import me.anno.ecs.Entity
import me.anno.ecs.components.cache.MeshCache
import me.anno.ecs.components.camera.Camera
import me.anno.ecs.components.camera.control.OrbitControls
import me.anno.engine.ECSRegistry
import me.anno.gpu.GFX
import me.anno.gpu.drawing.DrawTexts
import me.anno.gpu.drawing.DrawTextures
import me.anno.gpu.shader.ComputeTextureMode
import me.anno.gpu.texture.Texture2D
import me.anno.input.Input
import me.anno.maths.Maths.SECONDS_TO_NANOS
import me.anno.maths.Maths.max
import me.anno.maths.bvh.BLASNode.Companion.createBLASTexture
import me.anno.maths.bvh.BLASNode.Companion.createTriangleTexture
import me.anno.ui.Panel
import me.anno.ui.base.SpyPanel
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.custom.CustomList
import me.anno.ui.debug.TestDrawPanel
import me.anno.ui.debug.TestStudio.Companion.testUI
import me.anno.utils.Color.toRGB
import me.anno.utils.OS.documents
import me.anno.utils.hpc.ProcessingGroup
import me.anno.utils.hpc.ThreadLocal2
import me.anno.utils.pooling.JomlPools
import me.anno.utils.structures.lists.Lists.any2
import me.anno.utils.types.Booleans.toInt
import org.apache.logging.log4j.LogManager
import org.joml.Quaternionf
import org.joml.Vector3f
import kotlin.math.pow

fun main() {
    ECSRegistry.init()
    val meshSource = documents.getChild("monkey.obj")
    val mesh = MeshCache[meshSource]!!
    val blas = BVHBuilder.buildBLAS(mesh, SplitMethod.MEDIAN, 8)!!
    blas.print()
    main2(blas, Vector3f(), Quaternionf(), 1f)
}

fun createCPUPanel(
    cameraPosition: Vector3f,
    cameraRotation: Quaternionf,
    fovZFactor: Float,
    cpuTexture: Texture2D,
    bvh: BLASNode,
    useGroups: Boolean,
): Panel {

    var isCPUComputing = false
    var cpuSpeed = -1L
    var cpuFPS = 0L

    fun computeSpeed(w: Int, h: Int, dt: Long) = dt / (w * h)

    val pipeline = ProcessingGroup("brt", 1f)
    val tileSize = 8
    val groups = ThreadLocal2 { RayGroup(tileSize, tileSize, RayGroup(tileSize, tileSize)) }
    return TestDrawPanel {

        it.clear()

        // render cpu side
        // render at lower resolution because of performance
        val w = it.w
        val h = it.h

        val cx = (w - 1) * 0.5f
        val cy = (h - 1) * 0.5f
        val fovZ = -w * fovZFactor

        fun nextFrame() {
            val camPos2 = Vector3f(cameraPosition)
            val camRot2 = Quaternionf(cameraRotation)
            pipeline += {
                val cpuBuffer = Texture2D.bufferPool[w * h * 4, false, false]
                val ints = cpuBuffer.asIntBuffer()
                val t0 = System.nanoTime()
                val maxDistance = 1e10
                val maxDistanceF = maxDistance.toFloat()
                pipeline.processBalanced2d(0, 0, w, h, tileSize, 1) { x0, y0, x1, y1 ->
                    if (useGroups) {
                        val tsm1 = 1f / (tileSize - 1)
                        val group = groups.get()
                        val dir = JomlPools.vec3f.create()
                        dir.set(x0 - cx, cy - y0, fovZ).normalize()
                        // define ray position & main ray
                        group.setMain(camPos2, camRot2.transform(dir), maxDistanceF)
                        // define ray gradients by sample at (tileSize-1,0) and (0,tileSize-1)
                        dir.set(x0 + tileSize - 1 - cx, cy - y0, fovZ).normalize()
                        group.setDx(camRot2.transform(dir))
                        dir.set(x0 - cx, cy - (y0 + tileSize - 1), fovZ).normalize()
                        group.setDy(camRot2.transform(dir))
                        for (j in 0 until tileSize) {
                            val y = y0 + j
                            for (i in 0 until tileSize) {
                                val x = x0 + i
                                // define ray
                                dir.set(x - cx, cy - y, fovZ).normalize()
                                camRot2.transform(dir)
                                val k = i + j * tileSize
                                group.setRay(k, dir)
                                group.dxs[k] = i * tsm1
                                group.dys[k] = j * tsm1
                            }
                        }
                        group.finishSetup()
                        bvh.intersect(group)
                        val normal = JomlPools.vec3f.create()
                        for (y in y0 until y1) {
                            for (x in x0 until x1) {
                                val i = x - x0
                                val j = y - y0
                                val k = i + j * tileSize
                                normal.set(group.normalX[k], group.normalY[k], group.normalZ[k])
                                val color = if (normal.lengthSquared() > 0f)
                                    normal.normalize(0.5f).add(0.5f, 0.5f, 0.5f).toRGB()
                                else sky1
                                ints.put(x + y * w, color)
                            }
                        }
                        JomlPools.vec3f.sub(2)
                    } else {
                        for (y in y0 until y1) {
                            for (x in x0 until x1) {
                                val dir = JomlPools.vec3f.create()
                                dir.set(x - cx, cy - y, fovZ).normalize()
                                camRot2.transform(dir)
                                val hit = localResult.get()
                                hit.normalWS.set(0.0)
                                hit.distance = maxDistance
                                bvh.intersect(camPos2, dir, hit)
                                val color = if (hit.normalWS.lengthSquared() > 0.0)
                                    hit.normalWS.normalize(0.5).add(0.5, 0.5, 0.5).toRGB()
                                else sky1 // Maths.mixARGB2(sky0, sky1, 0.1f * length(dirX, dirY))
                                JomlPools.vec3f.sub(1)
                                ints.put(x + y * w, color)
                            }
                        }
                    }
                }
                val t1 = System.nanoTime()
                val dt = max(t1 - t0, 1L)
                cpuSpeed = if (cpuSpeed > 0) {// smoothing
                    (cpuSpeed * 7 + computeSpeed(w, h, dt)) shr 3
                } else {
                    computeSpeed(w, h, dt)
                }
                cpuFPS = SECONDS_TO_NANOS / dt
                GFX.addGPUTask("brt-cpu", 1) {
                    cpuTexture.w = w
                    cpuTexture.h = h
                    cpuTexture.createRGBA(ints, false)
                    Texture2D.bufferPool.returnBuffer(cpuBuffer)
                    isCPUComputing = false
                }
            }
        }

        if (!isCPUComputing) {
            isCPUComputing = true
            nextFrame()
        }

        DrawTextures.drawTexture(it.x, it.y, it.w, it.h, cpuTexture, true, -1, null)
        DrawTexts.drawSimpleTextCharByChar(it.x + 4, it.y + it.h - 50, 2, "$cpuSpeed ns/e, $cpuFPS fps")

    }
}

fun main2(
    bvh: BLASNode,
    cameraPosition: Vector3f,
    cameraRotation: Quaternionf,
    fovZFactor: Float
) {

    LogManager.disableLogger("WorkSplitter")

    testUI {

        val cpuTex0 = Texture2D("cpu", 1, 1, 1)
        val cpuTex1 = Texture2D("cpu", 1, 1, 1)
        val gpuTex0 = Texture2D("gpu", 1, 1, 1)

        cpuTex0.createRGBA()
        cpuTex1.createRGBA()

        val main = PanelListY(style)
        val controls = OrbitControls()
        val camera = Camera()
        val camEntity = Entity()
        val base = Entity()

        camEntity.add(camera)
        base.add(camEntity)

        controls.camera = camera
        controls.position.set(cameraPosition)
        controls.base = base
        controls.radius = 1f
        controls.movementSpeed = 0.2f * bvh.bounds.volume().pow(1f / 3f)
        controls.rotationSpeed = 0.15f

        val window = GFX.someWindow
        var lx = window.mouseX
        var ly = window.mouseY
        var lz = Input.mouseWheelSumY
        main.add(SpyPanel(style) {
            if (window.windowStack.inFocus.any2 { it is TestDrawPanel }) {
                if (Input.isRightDown) {
                    val dx = window.mouseX - lx
                    val dy = window.mouseY - ly
                    controls.onMouseMoved(0f, 0f, dx, dy)
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

        val list = CustomList(false, style)

        list.add(createCPUPanel(cameraPosition, cameraRotation, fovZFactor, cpuTex0, bvh, false))
        list.add(createCPUPanel(cameraPosition, cameraRotation, fovZFactor, cpuTex1, bvh, true))

        val triangles = createTriangleTexture(bvh)
        val blasNodes = createBLASTexture(bvh)
        val shader = createComputeShader(true, bvh.maxDepth(), null)

        list.add(TestDrawPanel {

            it.clear()

            // render gpu side
            val w = it.w
            val h = it.h

            val cx = (w - 1) * 0.5f
            val cy = (h - 1) * 0.5f
            val fovZ = -w * fovZFactor

            if (!gpuTex0.isCreated || gpuTex0.w != w || gpuTex0.h != h) {
                gpuTex0.w = w
                gpuTex0.h = h
                gpuTex0.createRGBA()
            }

            shader.use()
            shader.v2i("size", w, h)
            shader.v3f("worldPos", cameraPosition)
            shader.v4f("worldRot", cameraRotation)
            shader.v3f("cameraOffset", cx, cy, fovZ)
            shader.v3f("sky0", sky0)
            shader.v3f("sky1", sky1)
            shader.v1i("drawMode", Input.isKeyDown('x').toInt(1))
            shader.bindTexture(0, triangles, ComputeTextureMode.READ)
            shader.bindTexture(1, blasNodes, ComputeTextureMode.READ)
            shader.bindTexture(2, gpuTex0, ComputeTextureMode.WRITE)
            shader.runBySize(w, h)

            DrawTextures.drawTexture(it.x, it.y + it.h, it.w, -it.h, gpuTex0, true, -1, null)

        })

        main.add(list)
        list.setWeight2(100f)
        main.setWeight2(100f)
        main

    }

}