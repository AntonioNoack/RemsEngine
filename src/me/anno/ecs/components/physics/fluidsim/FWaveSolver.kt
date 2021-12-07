package me.anno.ecs.components.physics.fluidsim

import me.anno.ecs.components.physics.fluidsim.setups.FluidSimSetup
import me.anno.ecs.components.physics.fluidsim.setups.LinearDamBreak
import me.anno.io.files.FileReference.Companion.getReference
import org.apache.logging.log4j.LogManager
import kotlin.math.abs
import kotlin.math.sqrt

object FWaveSolver {

    private val LOGGER = LogManager.getLogger(FWaveSolver::class)

    // SHALLOW_WATER_EQUATIONS // for tsunami-like waves, using the FWave solver
    fun solve(
        i0: Int, i1: Int,
        hSrc: FloatArray,
        huSrc: FloatArray,
        b: FloatArray,
        hDst: FloatArray,
        huDst: FloatArray,
        gravity: Float,
        scaling: Float,
        tmp4f: FloatArray
    ) {

        var h0 = hSrc[i0]
        var h1 = hSrc[i1]

        val wet0 = h0 > 0f
        val wet1 = h1 > 0f

        if (wet0 || wet1) {

            var b0 = b[i0]
            var b1 = b[i1]

            var hu0 = huSrc[i0]
            var hu1 = huSrc[i1]

            // apply dry-wet condition
            if (!wet0) {// left cell is dry
                h0 = h1
                b0 = b1
                hu0 = -hu1
            } else if (!wet1) {// right cell is dry
                h1 = h0
                b1 = b0
                hu1 = -hu0
            }

            solve(
                h0, h1,
                hu0, hu1,
                b0, b1,
                gravity,
                tmp4f
            )

            // apply changes left
            if (wet0) {
                hDst[i0] -= scaling * tmp4f[0]
                huDst[i0] -= scaling * tmp4f[1]
            }

            // apply changes right
            if (wet1) {
                hDst[i1] -= scaling * tmp4f[2]
                huDst[i1] -= scaling * tmp4f[3]
            }

            /*if (tmp4f.any { it > 0f }) {
                LOGGER.debug("$i0/$i1 -> $h0 $h1 $hu0 $hu1 $b0 $b1 x $gravity x $scaling = ${tmp4f.joinToString()} -> ${hDst[i0]} ${hDst[i1]}")
            }*/

        }
    }

    fun solve(
        h0: Float, h1: Float,
        hu0: Float, hu1: Float,
        b0: Float, b1: Float,
        gravity: Float,
        dst: FloatArray
    ) {
        val roeHeight = (h0 + h1) * 0.5f
        val sqrt0 = sqrt(h0)
        val sqrt1 = sqrt(h1)
        val u0 = if (h0 > 0f) hu0 / h0 else 0f
        val u1 = if (h1 > 0f) hu1 / h1 else 0f
        val roeVelocity = (u0 * sqrt0 + u1 * sqrt1) / (sqrt0 + sqrt1)
        val gravityTerm = sqrt(gravity * roeHeight)
        val lambda0 = roeVelocity - gravityTerm
        val lambda1 = roeVelocity + gravityTerm
        val deltaLambda = 2f * gravityTerm
        val bathymetryTermV2 = roeHeight * (b1 - b0)
        val df0 = hu1 - hu0
        val df1 = hu1 * u1 - hu0 * u0 + gravity * (0.5f * (h1 * h1 - h0 * h0) + bathymetryTermV2)
        val deltaH0 = +(df0 * lambda1 - df1) / deltaLambda
        val deltaH1 = -(df0 * lambda0 - df1) / deltaLambda
        val deltaHu0 = deltaH0 * lambda0
        val deltaHu1 = deltaH1 * lambda1
        if (lambda0 < 0f) {// first wave to the left
            dst[0] = deltaH0
            dst[1] = deltaHu0
            dst[2] = 0f
            dst[3] = 0f
        } else {
            dst[0] = 0f
            dst[1] = 0f
            dst[2] = deltaH0
            dst[3] = deltaHu0
        }
        if (lambda1 < 0f) {// second wave to the right
            dst[0] += deltaH1
            dst[1] += deltaHu1
        } else {
            dst[2] += deltaH1
            dst[3] += deltaHu1
        }
    }

    @JvmStatic
    fun main(args: Array<String>) {

        testWithHandselected()

        testSmall()

        testFromFile()

    }

    private fun testSmall() {
        val prop = FluidSim()
        prop.width = 100
        prop.height = 1
        val setup = LinearDamBreak()
        setup.height0 = 10f
        setup.height1 = 8f
        prop.initWithSetup(setup)
        prop.setGhostOutflow()
        val step = 0.1f
        println((0 until 100).joinToString { prop.getFluidHeightAt(it, 0).toString() })
        prop.computeStep(step)
        println((0 until 100).joinToString { prop.getFluidHeightAt(it, 0).toString() })
        for (i in 0 until 49) {
            assert(prop.getFluidHeightAt(i, 0) == 10f)
            assert(prop.getMomentumXAt(i, 0) == 0f)
        }
        assert(prop.getFluidHeightAt(49, 0), 10 - step * 9.394671362f, 0.01f)
        assert(prop.getMomentumXAt(49, 0), step * 88.25985f, 0.01f)
        assert(prop.getFluidHeightAt(50, 0), 8 + step * 9.394671362f, 0.01f)
        assert(prop.getMomentumXAt(50, 0), step * 88.25985f, 0.01f)
        for (i in 51 until 100) {
            assert(prop.getFluidHeightAt(i, 0), 8f, 1e-5f)
            assert(prop.getMomentumXAt(i, 0), 0f, 1e-5f)
        }
    }

    fun assert(b: Boolean) {
        if (!b) throw RuntimeException()
    }

    fun assert(a: Float, b: Float, delta: Float) {
        if (abs(a - b) > delta) throw RuntimeException("$a != $b, ${abs(a - b)} > $delta")
    }

    private fun testWithHandselected() {
        // tests
        val dst = FloatArray(4)
        val g = 9.81f
        solve(10f, 10f, 4f, 4f, 0f, 0f, g, dst)
        assert(dst[0] == 0f)
        assert(dst[1] == 0f)
        assert(dst[2] == 0f)
        assert(dst[3] == 0f)
        solve(10f, 0f, 10f, 0f, 0f, 0f, g, dst)
        println(dst.joinToString())
        assert(abs(+30.017855 - dst[0]) < 0.01)
        assert(abs(-180.21432 - dst[1]) < 0.01)
        assert(abs(-40.017855 - dst[2]) < 0.01)
        assert(abs(-320.28574 - dst[3]) < 0.01)
        solve(10f, 10f, 10f, -10f, 0f, 0f, g, dst)
        println(dst.joinToString())
        assert(abs(-10.000000f - dst[0]) < 0.01)
        assert(abs(+99.045684f - dst[1]) < 0.01)
        assert(abs(-10.000000f - dst[2]) < 0.01)
        assert(abs(-99.045684f - dst[3]) < 0.01)
        solve(10f, 50f, 0f, 0f, 50f, 10f, g, dst)
        println(dst.joinToString())
        assert(abs(dst[0]) < 0.001)
        assert(abs(dst[1]) < 0.001)
        assert(abs(dst[2]) < 0.001)
        assert(abs(dst[3]) < 0.001)
    }

    private fun testFromFile() {

        val sim = FluidSim()
        sim.width = 32
        sim.height = 1
        sim.gravity = 9.80665f

        val halfIndex = sim.width / 2

        var hl = 0f
        var hr = 0f
        var hul = 0f
        var hur = 0f

        val setup = object : FluidSimSetup() {
            override fun getHeight(x: Int, y: Int, w: Int, h: Int): Float {
                return if (x * 2 <= w) hl else hr
            }

            override fun getMomentumX(x: Int, y: Int, w: Int, h: Int): Float {
                return if (x * 2 <= w) hul else hur
            }
        }

        val lines = getReference("E:/Documents/Uni/Master/WS2122/tsunami/data/middle_states.csv")
            .inputStream().bufferedReader()

        var passedCtr = 0

        var index = -1
        while (index < 500e3) {

            val line = lines.readLine() ?: break
            if (line.isEmpty() || line[0] == 'h' || line[0] == '#') continue

            index++

            val parts = line.split(',').map { it.toFloat() }
            hl = parts[0]
            hr = parts[1]
            hul = parts[2]
            hur = parts[3]

            sim.initWithSetup(setup)

            val hStar = parts[4]
            val targetPrecision = 0.05f * abs(hStar)

            val maxSteps = sim.width
            for (i in 0 until maxSteps) {

                try {
                    sim.step(1e9f, 1)
                } catch (e: Exception) {
                    // e.printStackTrace()
                    LOGGER.warn("failed line[$index] $line with $e")
                    break
                }

                val hComputed = sim.getFluidHeightAt(halfIndex, 0)

                val passed = abs(hStar - hComputed) < targetPrecision
                if (passed) {
                    // println("passed $index $line :)")
                    passedCtr++
                    break
                }
                if (i == maxSteps - 1) LOGGER.warn("did not pass, got $hComputed instead of $hStar from $line, line $index")
            }
        }

        LOGGER.info("$passedCtr/$index passed")

    }

}