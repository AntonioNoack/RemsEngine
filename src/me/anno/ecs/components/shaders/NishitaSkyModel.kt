package me.anno.ecs.components.shaders

import me.anno.Engine
import me.anno.gpu.drawing.DrawTextures
import me.anno.gpu.texture.Texture2D
import me.anno.image.raw.FloatImage
import me.anno.maths.Maths.PIf
import me.anno.maths.Maths.length
import me.anno.maths.Maths.max
import me.anno.maths.Maths.pow
import me.anno.maths.Maths.sq
import me.anno.ui.debug.TestDrawPanel.Companion.testDrawing
import me.anno.utils.hpc.HeavyProcessing
import me.anno.utils.pooling.JomlPools
import org.hsluv.HSLuvColorSpace.xyzToRgb
import org.joml.Vector3f
import kotlin.math.*

object NishitaSkyModel {

    private const val rayleighScale = 8e3f // rayleigh scale height in meters
    private const val mieScale = 1200f // mie scale height in meters
    private const val mieCoefficient = 2e-5f // mie scattering coefficient (1/m)
    private const val mieG = 0.76f // aerosols anisotropy
    private const val sqrG = mieG * mieG
    private const val earthRadius = 6.360e6f // in meters
    private const val earthRadiusSq = earthRadius * earthRadius
    private const val atmosphereRadius = 6.420e6f // in meters
    private const val atmosphereRadiusSq = atmosphereRadius * atmosphereRadius
    private const val steps = 32
    private const val numWavelengths = 21
    private const val minWavelength = 380 // nm
    private const val maxWavelength = 780 // nm
    private const val stepLambda = (maxWavelength - minWavelength) / (numWavelengths - 1f)

    // Sun irradiance on top of the atmosphere (W*m^-2*nm^-1)
    private val irradiance = floatArrayOf(
        1.45756829855592995315f, 1.56596305559738380175f, 1.65148449067670455293f,
        1.71496242737209314555f, 1.75797983805020541226f, 1.78256407885924539336f,
        1.79095108475838560302f, 1.78541550133410664714f, 1.76815554864306845317f,
        1.74122069647250410362f, 1.70647127164943679389f, 1.66556087452739887134f,
        1.61993437242451854274f, 1.57083597368892080581f, 1.51932335059305478886f,
        1.46628494965214395407f, 1.41245852740172450623f, 1.35844961970384092709f,
        1.30474913844739281998f, 1.25174963272610817455f, 1.19975998755420620867f
    )

    // Rayleigh scattering coefficient (m^-1)
    private val rayleighCoefficients = floatArrayOf(
        0.00005424820087636473f, 0.00004418549866505454f, 0.00003635151910165377f,
        0.00003017929012024763f, 0.00002526320226989157f, 0.00002130859310621843f,
        0.00001809838025320633f, 0.00001547057129129042f, 0.00001330284977336850f,
        0.00001150184784075764f, 0.00000999557429990163f, 0.00000872799973630707f,
        0.00000765513700977967f, 0.00000674217203751443f, 0.00000596134125832052f,
        0.00000529034598065810f, 0.00000471115687557433f, 0.00000420910481110487f,
        0.00000377218381260133f, 0.00000339051255477280f, 0.00000305591531679811f
    )

    // Ozone absorption coefficient (m^-1)
    private val ozoneCoefficients = floatArrayOf(
        0.00000000325126849861f, 0.00000000585395365047f, 0.00000001977191155085f,
        0.00000007309568762914f, 0.00000020084561514287f, 0.00000040383958096161f,
        0.00000063551335912363f, 0.00000096707041180970f, 0.00000154797400424410f,
        0.00000209038647223331f, 0.00000246128056164565f, 0.00000273551299461512f,
        0.00000215125863128643f, 0.00000159051840791988f, 0.00000112356197979857f,
        0.00000073527551487574f, 0.00000046450130357806f, 0.00000033096079921048f,
        0.00000022512612292678f, 0.00000014879129266490f, 0.00000016828623364192f
    )

    val cmfX = 10.8f
    val cmfY = 1.0f
    val cmfZ = 3.3f

    // CIE XYZ color matching functions
    private val cmfXYZ = floatArrayOf(
        0.00136800000f, 0.00003900000f, 0.00645000100f,
        0.01431000000f, 0.00039600000f, 0.06785001000f,
        0.13438000000f, 0.00400000000f, 0.64560000000f,
        0.34828000000f, 0.02300000000f, 1.74706000000f, // z, 3
        0.29080000000f, 0.06000000000f, 1.66920000000f,
        0.09564000000f, 0.13902000000f, 0.81295010000f,
        0.00490000000f, 0.32300000000f, 0.27200000000f,
        0.06327000000f, 0.71000000000f, 0.07824999000f,
        0.29040000000f, 0.95400000000f, 0.02030000000f,
        0.59450000000f, 0.99500000000f, 0.00390000000f, // y, 9
        0.91630000000f, 0.87000000000f, 0.00165000100f,
        1.06220000000f, 0.63100000000f, 0.00080000000f, // x, 11
        0.85444990000f, 0.38100000000f, 0.00019000000f,
        0.44790000000f, 0.17500000000f, 0.00002000000f,
        0.16490000000f, 0.06100000000f, 0.00000000000f,
        0.04677000000f, 0.01700000000f, 0.00000000000f,
        0.01135916000f, 0.00410200000f, 0.00000000000f,
        0.00289932700f, 0.00104700000f, 0.00000000000f,
        0.00069007860f, 0.00024920000f, 0.00000000000f,
        0.00016615050f, 0.00006000000f, 0.00000000000f,
        0.00004150994f, 0.00001499000f, 0.00000000000f
    )

    /** Parameters for optical depth quadrature.
     * See the comment in ray_optical_depth for more detail.
     * Computed using sympy and following Python code:
     * # from sympy.integrals.quadrature import gauss_laguerre
     * # from sympy import exp
     * # x, w = gauss_laguerre(8, 50)
     * # xend = 25
     * # print([(xi / xend).evalf(10) for xi in x])
     * # print([(wi * exp(xi) / xend).evalf(10) for xi, wi in zip(x, w)])
     */
    private const val quadratureSteps = 8
    private val quadratureNodes = floatArrayOf(
        0.006811185292f,
        0.03614807107f,
        0.09004346519f,
        0.1706680068f,
        0.2818362161f,
        0.4303406404f,
        0.6296271457f,
        0.9145252695f
    )
    private val quadratureWeights = floatArrayOf(
        0.01750893642f,
        0.04135477391f,
        0.06678839063f,
        0.09507698807f,
        0.1283416365f,
        0.1707430204f,
        0.2327233347f,
        0.3562490486f
    )

    private fun geographicalToDirection(lat: Float, lon: Float, dst: Vector3f): Vector3f {
        return dst.set(cos(lat) * cos(lon), cos(lat) * sin(lon), sin(lat))
    }

    private fun specToXYZ(spectrum: FloatArray, dst: Vector3f): Vector3f {
        dst.set(0f)
        val cmf = cmfXYZ
        for (i in spectrum.indices) {
            val s = spectrum[i]
            val i3 = i * 3
            dst.add(cmf[i3] * s, cmf[i3 + 1] * s, cmf[i3 + 2] * s)
        }
        dst.mul(stepLambda)
        return dst
    }

    /** atmosphere volume models*/
    fun densityRayleigh(height: Float): Float = exp(-height / rayleighScale)

    fun densityMie(height: Float): Float = exp(-height / mieScale)

    fun densityOzone(height: Float): Float {
        return if (height in 10e3f..40e3f) {
            if (height < 25e3f) {
                +1f / 15e3f * height - 2f / 3f
            } else {
                -1f / 15e3f * height - 8f / 3f
            }
        } else 0f
    }

    fun phaseRayleigh(mu: Float): Float {
        return 3f / (16f * PIf) * (1f + mu * mu)
    }

    fun phaseMie(mu: Float): Float {
        return (3f * (1f - sqrG) * (1f + mu * mu)) /
                (8f * PIf * (2f + sqrG) * pow((1f + sqrG - 2f * mieG * mu), 1.5f))
    }

    /** intersection helpers */
    fun surfaceIntersection(pos: Vector3f, dir: Vector3f): Boolean {
        if (dir.z >= 0f) return false // ???
        val b = 2f * dir.dot(pos)
        val c = pos.lengthSquared() - earthRadiusSq
        val t = b * b - 4f * c
        return t >= 0f
    }

    fun atmosphereIntersection(pos: Vector3f, dir: Vector3f, dst: Vector3f): Vector3f {
        val b = 2f * dir.dot(pos)
        val c = pos.lengthSquared() - atmosphereRadiusSq
        val x = b * b - 4f * c
        val t = (-b + sqrt(max(0f, x))) * 0.5f
        if (t.isNaN()) throw IllegalStateException()
        return dst.set(dir).mul(t).add(pos)
    }

    /** Optical depth along a ray.
     * Instead of using classic ray marching, the code is based on Gauss-Laguerre quadrature,
     * which is designed to compute the integral of f(x)*exp(-x) from 0 to infinity.
     * This works well here, since the optical depth along the ray tends to decrease exponentially.
     * By setting f(x) = g(x) exp(x), the exponentials cancel out and we get the integral of g(x).
     * The nodes and weights used here are the standard n=6 Gauss-Laguerre values, except that
     * the exp(x) scaling factor is already included in the weights.
     * The parametrization along the ray is scaled so that the last quadrature node is still within
     * the atmosphere. */
    fun rayOpticalDepth(pos: Vector3f, dir: Vector3f, dst: Vector3f): Vector3f {
        val rayEnd = atmosphereIntersection(pos, dir, dst)
        val rayLength = pos.distance(rayEnd)
        // val segment = rayLength * dir
        val sx = rayLength * dir.x
        val sy = rayLength * dir.y
        val sz = rayLength * dir.z
        if (sx.isNaN() || sy.isNaN() || sz.isNaN()) throw IllegalStateException()
        dst.set(0f)
        for (i in 0 until quadratureSteps) {
            // val p = pos + quadratureNodes[i] * segment
            val qn = quadratureNodes[i]
            val px = pos.x + qn * sx
            val py = pos.y + qn * sy
            val pz = pos.z + qn * sz
            // height above sea level
            val height = length(px, py, pz) - earthRadius
            val density0 = densityRayleigh(height)
            val density1 = densityMie(height)
            val density2 = densityOzone(height)
            val qw = quadratureWeights[i]
            dst.add(density0 * qw, density1 * qw, density2 * qw)
        }
        return dst.mul(rayLength)
    }

    /** single-inscattering along a ray through the atmosphere; overrides pos */
    fun singleScattering(
        pos: Vector3f,
        dir: Vector3f,
        sunDir: Vector3f,
        airDensity: Float,
        dustDensity: Float,
        ozoneDensity: Float,
        lightAccumulation: FloatArray,
        tmp: Vector3f
    ) {

        val rayEnd = atmosphereIntersection(pos, dir, tmp)
        val rayLength = pos.distance(rayEnd)

        // we step along the ray in segments and accumulate the inscattering as well as the optical depth along each segment
        val segmentLength = rayLength / steps
        // val segment = segmentLength * dir
        val sx = segmentLength * dir.x
        val sy = segmentLength * dir.y
        val sz = segmentLength * dir.z

        // instead of tracking the transmission spectrum across all wavelengths directly,
        // we use the fact that the density always has the same spectrum for each type of
        // scattering, so we split the density into a constant spectrum and a factor and
        // only track the factors

        lightAccumulation.fill(0f)

        // phase function for scattering and the density scale factor
        val mu = dir.dot(sunDir)
        val phaseFunc0 = phaseRayleigh(mu)
        val phaseFunc1 = phaseMie(mu)

        // the density and in-scattering of each segment is evaluated at its middle
        var px = pos.x + 0.5f * sx
        var py = pos.y + 0.5f * sy
        var pz = pos.z + 0.5f * sz

        var ox = 0f
        var oy = 0f
        var oz = 0f

        for (i in 0 until steps) {
            // height above sea level
            val height = length(px, py, pz) - earthRadius
            // evaluate and accumulate optical depth along the ray
            val density0 = airDensity * densityRayleigh(height)
            val density1 = dustDensity * densityMie(height)
            val density2 = ozoneDensity * densityOzone(height)
            ox += segmentLength * density0
            oy += segmentLength * density1
            oz += segmentLength * density2
            pos.set(px, py, pz)
            // if the Earth isn't in the way, evaluate inscattering from the sun
            if (!surfaceIntersection(pos, sunDir)) {
                val rod = rayOpticalDepth(pos, sunDir, tmp)
                // light optical depth
                val lod0 = airDensity * rod.x
                val lod1 = dustDensity * rod.y
                val lod2 = ozoneDensity * rod.z
                // total optical depth
                val tod0 = ox + lod0
                val tod1 = oy + lod1
                val tod2 = oz + lod2
                // attenuation of light
                for (wl in 0 until numWavelengths) {
                    // extinctionDensity
                    val rc = rayleighCoefficients[wl]
                    val ed0 = tod0 * rc
                    val ed1 = tod1 * 1.11f * mieCoefficient
                    val ed2 = tod2 * ozoneCoefficients[wl]
                    val attenuation = exp(-(ed0 + ed1 + ed2))
                    // scattering density
                    val sd0 = density0 * rc
                    val sd1 = density1 * mieCoefficient
                    // the total inscattered radiance from one segment is:
                    // Tr(A<->B) * Tr(B<->C) * sigma_s * phase * L * segment_length
                    //
                    // These terms are:
                    // Tr(A<->B): Transmission from start to scattering position (tracked in optical_depth)
                    // Tr(B<->C): Transmission from scattering position to light (computed in
                    // ray_optical_depth) sigma_s: Scattering density phase: Phase function of the scattering
                    // type (Rayleigh or Mie) L: Radiance coming from the light source segment_length: The
                    // length of the segment
                    //
                    // The code here is just that, with a bit of additional optimization to not store full
                    // spectra for the optical depth
                    lightAccumulation[wl] += attenuation *
                            (phaseFunc0 * sd0 + phaseFunc1 * sd1) *
                            irradiance[wl] * segmentLength
                }
                px += sx
                py += sy
                pz += sz
            }
        }


    }

    fun precomputeTexture(
        sunElevation: Float, altitude: Float, airDensity: Float, dustDensity: Float, ozoneDensity: Float,
        dst: FloatImage
    ) {
        val width = dst.width
        val height = dst.height
        val halfWidth = (width + 1) / 2
        val py = earthRadius + altitude
        val sunDir = geographicalToDirection(sunElevation, 0f, JomlPools.vec3f.create())
        val latStep = PIf * 0.5f / height
        val lonStep = PIf * 2f / width
        val halfLatStep = latStep * 0.5f
        val dst1 = dst.data
        val di0 = dst.numChannels - 3
        val di1 = dst.numChannels + 3
        HeavyProcessing.processBalanced(0, height, 1) { y0, y1 ->
            val pos = JomlPools.vec3f.create()
            val dir = JomlPools.vec3f.create()
            val tmp = JomlPools.vec3f.create()
            val lightAccumulation = FloatArray(numWavelengths)
            for (y in y0 until y1) {
                // sample more pixels towards horizon
                val lat = (PIf * 0.5f + halfLatStep) * sq(y.toFloat() / height)
                var index0 = dst.getIndex(0, y) * dst.numChannels
                var index1 = index0 + (width - 1) * dst.numChannels
                for (x in 0 until halfWidth) {
                    val lon = lonStep * x - PIf
                    geographicalToDirection(lat, lon, dir)
                    pos.set(0f, 0f, py)
                    singleScattering(pos, dir, sunDir, airDensity, dustDensity, ozoneDensity, lightAccumulation, tmp)
                    specToXYZ(lightAccumulation, tmp)
                    xyzToRgb(tmp, tmp)
                    // store pixels
                    dst1[index0++] = tmp.x
                    dst1[index0++] = tmp.y
                    dst1[index0++] = tmp.z
                    // mirror sky
                    dst1[index1++] = tmp.x
                    dst1[index1++] = tmp.y
                    dst1[index1++] = tmp.z
                    index0 += di0
                    index1 -= di1
                }
            }
            JomlPools.vec3f.sub(3)
        }
        JomlPools.vec3f.sub(1)
    }

    fun sunRadiation(
        dir: Vector3f,
        altitude: Float,
        airDensity: Float,
        dustDensity: Float,
        solidAngle: Float,
        spectrum: FloatArray
    ) {
        val pos = JomlPools.vec3f.create()
            .set(0f, 0f, earthRadius + altitude)
        val opticalDepth = rayOpticalDepth(pos, dir, JomlPools.vec3f.create())
        val odx = opticalDepth.x * airDensity
        val ody = opticalDepth.y * 1.11f * mieCoefficient * dustDensity
        val invSolid = 1f / solidAngle
        // compute spectrum
        for (i in 0 until numWavelengths) {
            // combine spectra and optical depth into transmittance
            val transmittance = rayleighCoefficients[i] * odx + ody
            spectrum[i] = irradiance[i] * exp(-transmittance) * invSolid
        }
    }

    fun precomputeSun(
        sunElevation: Float,
        angularDiameter: Float,
        altitude: Float,
        airDensity: Float,
        dustDensity: Float,
        tmpSpectrum: FloatArray, // numWavelengths
        dstTop: Vector3f,
        dstBottom: Vector3f
    ) {

        val halfAngular = angularDiameter * 0.5f
        val solidAngle = PIf * 2f * (1f - cos(halfAngular))

        // compute two pixels for sun dist
        geographicalToDirection(max(sunElevation - halfAngular, 0f), 0f, dstBottom)
        sunRadiation(dstBottom, altitude, airDensity, dustDensity, solidAngle, tmpSpectrum)
        specToXYZ(tmpSpectrum, dstBottom)

        geographicalToDirection(max(sunElevation + halfAngular, 0f), 0f, dstTop)
        sunRadiation(dstTop, altitude, airDensity, dustDensity, solidAngle, tmpSpectrum)
        specToXYZ(tmpSpectrum, dstTop)

    }

    @JvmStatic
    fun main(args: Array<String>) {
        val dst = FloatImage(127, 128, 3)
        val tex = Texture2D("sky", dst.width, dst.height, 1)
        testDrawing {
            precomputeTexture(Engine.gameTimeF * 0.1f, 3f, 1f, 1f, 1f, dst)
            dst.normalize()
            dst.createTexture(tex, false)
            DrawTextures.drawTexture(it.x, it.y+it.h, it.w, -it.h, tex, -1, null)
            // dst.write(desktop.getChild("sky.png"))
        }
    }

}