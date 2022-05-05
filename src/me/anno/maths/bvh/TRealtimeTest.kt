package me.anno.maths.bvh

import me.anno.config.DefaultConfig.style
import me.anno.ecs.Entity
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
import me.anno.ui.base.SpyPanel
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.custom.CustomList
import me.anno.ui.debug.TestDrawPanel
import me.anno.ui.debug.TestStudio.Companion.testUI
import me.anno.utils.Color.a
import me.anno.utils.Color.b
import me.anno.utils.Color.g
import me.anno.utils.Color.r
import me.anno.utils.Color.toRGB
import me.anno.utils.hpc.HeavyProcessing
import me.anno.utils.pooling.JomlPools
import me.anno.utils.structures.lists.Lists.any2
import me.anno.utils.types.AABBs.volume
import me.anno.utils.types.Booleans.toInt
import org.apache.logging.log4j.LogManager
import org.joml.Quaternionf
import org.joml.Vector3f
import kotlin.math.pow

fun main() {
    ECSRegistry.initNoGFX()
    val (tlas, cameraPosition, cameraRotation, fovZFactor) = createSampleTLAS(4)
    main(tlas, cameraPosition, cameraRotation, fovZFactor)
}

enum class BVHDrawMode(val id: Int) {
    COLOR(0),
    NORMAL(1),
    TLAS_HITS(2),
    BLAS_HITS(3),
}

fun main(
    bvh: TLASNode,
    cameraPosition: Vector3f, cameraRotation: Quaternionf,
    fovZFactor: Float
) {

    val cpuTexture = Texture2D("cpu", 1, 1, 1)
    val gpuTexture = Texture2D("gpu", 1, 1, 1)

    LogManager.disableLogger("WorkSplitter")

    testUI {

        cpuTexture.createRGBA()

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
        controls.rotationSpeed = 0.07f
        controls.mouseWheelSpeed = 20f

        val window = GFX.someWindow
        var lx = window.mouseX
        var ly = window.mouseY
        var lz = Input.mouseWheelSumY
        main.add(SpyPanel(style) {
            if (window.windowStack.inFocus.any2 { it is TestDrawPanel }) {
                if (Input.mouseKeysDown.isNotEmpty()) {
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
        var isCPUComputing = false
        var cpuSpeed = -1L
        var cpuFPS = 0L

        fun computeSpeed(w: Int, h: Int, dt: Long) = dt / (w * h)

        list.add(TestDrawPanel {

            it.clear()

            // render cpu side
            // render at lower resolution because of performance
            val w = it.w / 4
            val h = it.h / 4

            val cx = (w - 1) * 0.5f
            val cy = (h - 1) * 0.5f
            val fovZ = -w * fovZFactor

            fun nextFrame() {
                val tmpPos = Vector3f(cameraPosition)
                val tmpRot = Quaternionf(cameraRotation)
                HeavyProcessing.addTask("") {
                    val cpuBuffer = Texture2D.bufferPool[w * h * 4, false, false]
                    val t0 = System.nanoTime()
                    HeavyProcessing.processBalanced2d(0, 0, w, h, 8, 1) { x0, y0, x1, y1 ->
                        for (y in y0 until y1) {
                            for (x in x0 until x1) {
                                val dir = JomlPools.vec3f.create()
                                dir.set(x - cx, cy - y, fovZ).normalize()
                                tmpRot.transform(dir)
                                val hit = localResult.get()
                                hit.normalWS.set(0.0)
                                val maxDistance = 1e15
                                hit.distance = maxDistance
                                bvh.intersect(tmpPos, dir, hit)
                                val color = if (hit.normalWS.lengthSquared() > 0.0) {
                                    val normal = hit.normalWS
                                    normal.normalize()
                                    normal.mul(0.5).add(0.5, 0.5, 0.5)
                                    normal.toRGB()
                                } else sky1 // Maths.mixARGB2(sky0, sky1, 0.1f * length(dirX, dirY))
                                JomlPools.vec3f.sub(1)
                                val i = (x + y * w) * 4
                                cpuBuffer.put(i, color.r().toByte())
                                cpuBuffer.put(i + 1, color.g().toByte())
                                cpuBuffer.put(i + 2, color.b().toByte())
                                cpuBuffer.put(i + 3, color.a().toByte())
                            }
                        }
                    }
                    val t1 = System.nanoTime()
                    val dt = max(t1 - t0, 1L)
                    cpuSpeed = computeSpeed(w, h, dt)
                    cpuFPS = SECONDS_TO_NANOS / dt
                    GFX.addGPUTask(1) {
                        cpuTexture.w = w
                        cpuTexture.h = h
                        cpuTexture.createRGBA(cpuBuffer, false)
                        // Texture2D.bufferPool.returnBuffer(cpuBuffer)
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

        })

        val (shader, triangles, blasNodes, tlasNodes) = createShader(bvh)

        list.add(TestDrawPanel {

            it.clear()

            // render gpu side
            val w = it.w / 2
            val h = it.h / 2

            val cx = (w - 1) * 0.5f
            val cy = (h - 1) * 0.5f
            val fovZ = -w * fovZFactor

            if (!gpuTexture.isCreated || gpuTexture.w != w || gpuTexture.h != h) {
                gpuTexture.w = w
                gpuTexture.h = h
                gpuTexture.createRGBA()
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
            shader.bindTexture(2, tlasNodes, ComputeTextureMode.READ)
            shader.bindTexture(3, gpuTexture, ComputeTextureMode.WRITE)
            shader.runBySize(w, h)

            DrawTextures.drawTexture(it.x, it.y + it.h, it.w, -it.h, gpuTexture, true, -1, null)

        })

        main.add(list)
        list.setWeight(100f)
        main.setWeight(100f)
        main

    }

}