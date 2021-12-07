package me.anno.ecs.components.physics.fluidsim

import me.anno.ecs.annotations.DebugAction
import me.anno.ecs.annotations.ExecuteInEditMode
import me.anno.ecs.annotations.Range
import me.anno.ecs.annotations.Type
import me.anno.ecs.components.mesh.ManualProceduralMesh
import me.anno.ecs.components.mesh.ProceduralMesh
import me.anno.ecs.components.mesh.terrain.TerrainUtils.generateRegularQuadHeightMesh
import me.anno.ecs.components.physics.fluidsim.setups.CircularDamBreak
import me.anno.ecs.components.physics.fluidsim.setups.FluidSimSetup
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.gpu.GFX
import me.anno.io.serialization.NotSerializedProperty
import me.anno.io.serialization.SerializedProperty
import me.anno.utils.hpc.HeavyProcessing.processBalanced
import me.anno.utils.maths.Maths.clamp
import me.anno.utils.maths.Maths.mixARGB
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * simple 3d mesh, which simulates water
 * */
@ExecuteInEditMode
class FluidSim : ProceduralMesh() {

    // todo mode to visualize momentum

    // todo apply force/brush on bathymetry
    // todo apply force/brush on fluid depth


    // todo option to create border mesh for nicer looks

    @Type("ManualProceduralMesh/PrefabSaveable")
    var bathymetryMesh: ManualProceduralMesh? = null

    @NotSerializedProperty
    var wantsReset = false

    @DebugAction
    fun reset() {
        wantsReset = true
        hasValidBathymetryMesh = false
    }

    var hasValidBathymetryMesh = false

    @SerializedProperty
    var width = 10

    @SerializedProperty
    var height = 10

    @NotSerializedProperty
    var fluidHeight: FloatArray = f0

    @NotSerializedProperty
    var fluidMomentumX: FloatArray = f0

    @NotSerializedProperty
    var fluidMomentumY: FloatArray = f0

    @NotSerializedProperty
    var bathymetry: FloatArray = f0

    @SerializedProperty
    var gravity = 9.81f

    @SerializedProperty
    var cellSizeMeters = 1f
        set(value) {
            if (field != value && value > 0f) {
                field = value
                invalidateMesh()
            }
        }

    @SerializedProperty
    @Range(0.0, 0.5)
    var cfl2d = 0.45f

    val stride get() = width + 2

    @NotSerializedProperty
    private var tmpH = f0

    @NotSerializedProperty
    private var tmpHuX = f0

    @NotSerializedProperty
    private var tmpHuY = f0

    fun getIndex(x: Int, y: Int): Int {
        val width = width
        val height = height
        val lx = clamp(x + 1, 0, width + 1)
        val ly = clamp(y + 1, 0, height + 1)
        return lx + ly * (width + 2)
    }

    fun getSurfaceHeightAt(x: Int, y: Int): Float {
        val index = getIndex(x, y)
        return fluidHeight[index] + bathymetry[index]
    }

    fun getFluidHeightAt(x: Int, y: Int): Float {
        return fluidHeight[getIndex(x, y)]
    }

    fun getMomentumXAt(x: Int, y: Int): Float {
        return fluidMomentumX[getIndex(x, y)]
    }

    fun getMomentumYAt(x: Int, y: Int): Float {
        return fluidMomentumY[getIndex(x, y)]
    }

    fun getBathymetryAt(x: Int, y: Int): Float {
        return bathymetry[getIndex(x, y)]
    }

    // initialize using setups, just like in the Tsunami Lab module
    @Type("FluidSimSetup/PrefabSaveable")
    var setup: FluidSimSetup? = CircularDamBreak()

    private fun ensureFieldSize() {
        val targetSize = (width + 2) * (height + 2)
        if (fluidHeight.size != targetSize || wantsReset) {
            wantsReset = false
            hasValidBathymetryMesh = false
            fluidHeight = FloatArray(targetSize)
            fluidMomentumX = FloatArray(targetSize)
            fluidMomentumY = FloatArray(targetSize)
            tmpH = FloatArray(targetSize)
            tmpHuX = FloatArray(targetSize)
            tmpHuY = FloatArray(targetSize)
            bathymetry = FloatArray(targetSize)
            initWithSetup(setup ?: return)
        }
    }

    fun initWithSetup(setup: FluidSimSetup) {
        ensureFieldSize()
        var i = 0
        val w = width
        val h = height
        val h0 = fluidHeight
        val hu = fluidMomentumX
        val hv = fluidMomentumY
        val b = bathymetry
        for (y in -1..h) {
            for (x in -1..w) {
                h0[i] = setup.getHeight(x, y, w, h)
                hu[i] = setup.getMomentumX(x, y, w, h)
                hv[i] = setup.getMomentumY(x, y, w, h)
                b[i] = setup.getBathymetry(x, y, w, h)
                i++
            }
        }
    }

    override fun onUpdate(): Int {
        ensureFieldSize()
        try {
            step(GFX.deltaTime)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        invalidateMesh()
        // why is this function not called automatically?
        if (GFX.isGFXThread()) ensureBuffer()
        return 1 // update every tick
    }

    override fun generateMesh() {
        val width = width
        val height = height
        if (fluidHeight.size == (width + 2) * (height + 2)) {
            val bm = bathymetryMesh
            if (!hasValidBathymetryMesh && bm != null) generateBathymetryMesh(bm)
            generateFluidMesh(this)
        } // else wait for simulation
    }

    private fun generateBathymetryMesh(mesh: ProceduralMesh) {
        hasValidBathymetryMesh = true
        val bathymetry = bathymetry
        val cellSizeMeters = cellSizeMeters
        generateRegularQuadHeightMesh(
            width + 2, height + 2, getIndex(-1, -1),
            stride, cellSizeMeters, mesh.mesh2,
            { bathymetry[it] },
            { x, y, _, dst ->
                dst.x = getBathymetryAt(x + 1, y) - getBathymetryAt(x - 1, y)
                dst.y = cellSizeMeters * 2f
                dst.z = getBathymetryAt(x, y + 1) - getBathymetryAt(x, y - 1)
                dst.normalize()
            },
            { getLandColor(bathymetry[it]) }
        )
    }

    private fun generateFluidMesh(mesh: ProceduralMesh) {
        val bathymetry = bathymetry
        val fluidHeight = fluidHeight
        val cellSizeMeters = cellSizeMeters
        generateRegularQuadHeightMesh(
            width + 2, height + 2, getIndex(-1, -1),
            stride, cellSizeMeters, mesh.mesh2,
            { fluidHeight[it] + bathymetry[it] },
            { x, y, _, dst ->
                dst.x = getSurfaceHeightAt(x + 1, y) - getSurfaceHeightAt(x - 1, y)
                dst.y = cellSizeMeters * 2f
                dst.z = getSurfaceHeightAt(x, y + 1) - getSurfaceHeightAt(x, y - 1)
                dst.normalize()
            },
            { getWaterColor(fluidHeight[it]) }
        )
    }

    fun getLandColor(height: Float): Int {
        return -1
    }

    fun getWaterColor(height: Float): Int {
        return mixARGB(0xabbee3, 0x103273, clamp(height * 0.1f, 0f, 1f))
    }

    fun step(dt: Float, numMaxIterations: Int = 10): Float {
        var done = 0f
        var i = 0
        val t0 = System.nanoTime()
        while (done < dt && i++ < numMaxIterations) {
            val maxTimeStep = computeMaxTimeStep()
            val step = min(dt - done, maxTimeStep)
            if (step > 0f && step.isFinite()) {
                val scaling = step / cellSizeMeters
                computeStep(scaling)
                done += step
            } else break
            val t1 = System.nanoTime()
            if (t1 - t0 > 1e9 / 60f) break
        }
        return done
    }

    private fun copy(src: FloatArray, dst: FloatArray = FloatArray(src.size)): FloatArray {
        System.arraycopy(src, 0, dst, 0, min(src.size, dst.size))
        return dst
    }

    // valid, as long as this is single-threaded
    private val tmp4f = FloatArray(4)

    fun computeStep(scaling: Float) {

        val width = width
        val height = height

        val h0 = fluidHeight
        val hx = fluidMomentumX
        val hy = fluidMomentumY

        setGhostOutflow(h0, hx, hy)

        val g0 = copy(h0, tmpH)
        val gx = copy(hx, tmpHuX)

        val b = bathymetry

        val stride = stride
        val gravity = gravity

        // step on x axis
        processBalanced(0, height, 4) { y0, y1 ->
            val tmp4f = FloatArray(4)
            for (y in y0 until y1) {
                var index0 = getIndex(-1, y)
                for (x in -1 until width) {
                    FWaveSolver.solve(index0, index0 + 1, h0, hx, b, g0, gx, gravity, scaling, tmp4f)
                    index0++
                }
            }
        }

        setGhostOutflow(g0, gx, hy)

        // copy data, only height has changed, so only height needs to be switched
        copy(g0, h0)
        val gy = copy(hy, tmpHuY)

        // step on y axis
        processBalanced(-1, height, 4) { y0, y1 ->
            val tmp4f = FloatArray(4)
            for (y in y0 until y1) {
                var index0 = getIndex(0, y)
                for (x in 0 until width) {
                    FWaveSolver.solve(index0, index0 + stride, g0, hy, b, h0, gy, gravity, scaling, tmp4f)
                    index0++
                }
            }
        }

        // switch the buffers for the momentum
        var t = fluidMomentumX
        fluidMomentumX = tmpHuX
        tmpHuX = t

        t = fluidMomentumY
        fluidMomentumY = tmpHuY
        tmpHuY = t

    }

    fun setGhostOutflow(h: FloatArray = fluidHeight, hx: FloatArray = fluidMomentumX, hy: FloatArray = fluidMomentumY) {
        // set the ghost zone to be outflow conditions
        // left boundary
        val width = width
        val height = height
        for (y in 0 until height) {
            val outside = getIndex(-1, y)
            val inside = getIndex(0, y)
            h[outside] = h[inside]
            hx[outside] = hx[inside]
            hy[outside] = hy[inside]
        }
        for (y in 0 until height) {
            val outside = getIndex(width, y)
            val inside = getIndex(width - 1, y)
            h[outside] = h[inside]
            hx[outside] = hx[inside]
            hy[outside] = hy[inside]
        }
        for (x in 0 until width) {
            val outside = getIndex(x, -1)
            val inside = getIndex(x, 0)
            h[outside] = h[inside]
            hx[outside] = hx[inside]
            hy[outside] = hy[inside]
        }
        for (x in 0 until width) {
            val outside = getIndex(x, height)
            val inside = getIndex(x, height - 1)
            h[outside] = h[inside]
            hx[outside] = hx[inside]
            hy[outside] = hy[inside]
        }
    }

    private fun computeMaxTimeStep(): Float {
        val width = width
        val height = height
        val h = fluidHeight
        val hu = fluidMomentumX
        val hv = fluidMomentumY
        val stride = stride
        val gravity = gravity
        var maxVelocity = 0f
        for (y in 0 until height) {
            for (x in 0 until width) {
                val index = getIndex(x, y)
                val h0 = h[index]
                if (h0 > 0f) {
                    val h1 = max(
                        h0, max(
                            max(h[index - 1], h[index + 1]),
                            max(h[index - stride], h[index + stride])
                        )
                    )
                    val impulse = max(abs(hu[index]), abs(hv[index]))
                    val velocity = impulse / h0
                    val expectedVelocity = velocity + sqrt(gravity * h1)
                    maxVelocity = max(maxVelocity, expectedVelocity)
                }
            }
        }
        val is2D = width > 1 || height > 1
        val cflFactor = if (is2D) cfl2d else 0.5f
        return cflFactor * cellSizeMeters / maxVelocity
    }

    override fun clone(): FluidSim {
        val clone = FluidSim()
        copy(clone)
        return clone
    }

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as FluidSim
        // copy the scalar properties
        clone.width = width
        clone.height = height
        clone.cellSizeMeters = cellSizeMeters
        clone.gravity = gravity
        clone.cfl2d = cfl2d
        // copy the array properties
        clone.fluidHeight = copy(fluidHeight)
        clone.fluidMomentumX = copy(fluidMomentumX)
        clone.fluidMomentumY = copy(fluidMomentumY)
        clone.bathymetry = copy(bathymetry)
        val size = fluidHeight.size
        clone.tmpH = FloatArray(size)
        clone.tmpHuX = FloatArray(size)
        clone.tmpHuY = FloatArray(size)
        clone.needsUpdate = needsUpdate
        clone.wantsReset = wantsReset
        // copy the reference
        clone.bathymetryMesh = getInClone(bathymetryMesh, clone)
    }

    override val className: String = "FluidSim"

    companion object {
        private val f0 = FloatArray(0)
    }

}