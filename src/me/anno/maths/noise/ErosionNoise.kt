package me.anno.maths.noise

import me.anno.maths.Maths.absClamp
import me.anno.maths.Maths.clamp
import me.anno.maths.Maths.mix
import me.anno.maths.Maths.pow
import me.anno.maths.Maths.sq
import me.anno.maths.MinMax.max
import me.anno.maths.Smoothstep.smoothstep
import me.anno.utils.pooling.JomlPools
import org.joml.Vector2f
import org.joml.Vector3f
import org.joml.Vector4f
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.sign
import kotlin.math.sqrt

/**
 * Expensive noise function that models ridges and valleys well, by runevision,
 * https://www.shadertoy.com/view/sf23W1
 * https://www.youtube.com/watch?v=r4V21_uUK8Y
 *
 * This noise can be applied to any underlying height function.
 *
 * Advanced terrain erosion filter based on stacked faded gullies,
 * with controls for erosion strength, detail, ridge and crease rounding,
 * and producing a ridge map output useful for e.g. water drainage.
 *
 * For more on the technique, see:
 * https://blog.runevision.com/2026/03/fast-and-gorgeous-erosion-filter.html
 *
 * This buffer has three parts:
 *
 *  - Phacelle Nose function (used by the erosion function)
 *  - Erosion function
 *  - Demonstration
 *
 * For explanations of the erosion parameters, see the demonstration section.
 *
 * This erosion technique was originally derived from versions by
 * Clay John (https://www.shadertoy.com/view/MtGcWh)
 * and Fewes (https://www.shadertoy.com/view/7ljcRW)
 * and my own cleaned up version (https://www.shadertoy.com/view/33cXW8),
 * but at this point has little in common with them, apart from the high level concept.
 *
 * Also see "Advanced Terrain Erosion Filter" variation with animated parameters.
 * https://www.shadertoy.com/view/wXcfWn
 * */
abstract class ErosionNoise(val baseNoise: PhacelleNoise) {

    /**
     * get height, derivative X and derivative Y from baseline terrain (smooth)
     * */
    abstract fun sampleTerrain(px: Float, py: Float, dst: Vector3f): Vector3f

    var grassHeight = 0.465f

    var addWater = true
    var waterHeight = 0.46f

    /**
     * The scale of the erosion effect, affecting it both horizontally and vertically.
     * */
    var scale = 0.15F

    /**
     * The strength of the erosion effect, affecting the magnitude of all octaves,
     * and indirectly affecting the directions of the gullies as a result.
     * */
    var strength = 0.22f

    /**
     * The magnitude of the gullies as a weight value from 0 to 1.
     * A value of 0.0 can sharpen peaks and valleys but feature virtually no gullies.
     * A value of 1.0 produces full gullies but may leave peaks and valleys rounded.
     * Adjusting erosion gully weight while inversely adjusting erosion scale can be
     * used to control the sharpness of peaks and valleys while leaving gully
     * magnitudes largely untocuhed.
     * */
    var gullyWeight = 0.5f

    /**
     * The overall detail of the erosion. Lower values restrict the effect of higher
     * frequency gullies to steeper slopes.
     * */
    var detail = 1.5f

    /**
     * Separate rounding control of ridges and creases.
     *  x: Rounding of ridges.
     *  y: Rounding of creases.
     *  z: Multiplier applied to the initial height function.
     *     E.g. if the height function has noise of 5 times lower frequency
     *     than the largest gullies, a value of 0.2 can compensate for that.
     *  w: Multiplier applied to each subsequent gully octave after the first.
     *     Setting it to the same value as the erosion lacunarity will produce
     *     consistent rounding of all octaves.
     * */
    var rounding = Vector4f(0.1f, 0.0f, 0.1f, 2.0f)

    /**
     * Control over how far away from ridges/creases the erosion takes effect.
     *  x: Onset used on the initial height function.
     *  y: Onset used on each gully octave.
     *  z: RidgeMap-specific onset used on the initial height function.
     *  w: RidgeMap-specific onset used on each gully octave.
     * */
    var erosionOnset = Vector4f(0.7f, 1.25f, 2.8f, 1.5f)

    /**
     * Control over the erosion octaves, with each successive octave layering smaller gullies onto the terrain.
     * */
    var erosionOctaves = 5

    /**
     * Control over the assumed slope of the initial height function.
     * In practise, assuming a slope can work better than using the input slope,
     * since the final terrain can be shaped quite differently than the input.
     *  x: An assumed slope value to override the actual slope.
     *  y: The amount (from 0 to 1) to override the actual slope.
     * */
    var assumedSlope = Vector2f(0.7f, 1.0f)

    /**
     * Gullies are based on stripes within Voronoi-like cells in the Phacelle noise function.
     * The cell scale parameter controls the sizes of the cells relative to the overall erosion scale,
     * while keeping the stripe widths unaffected.
     * Values close to 1 usually produce good results.
     * Smaller values produce more grainy gullies while larger values produce longer unbroken gullies,
     * but too large values produce chaotic curved gullies that are not aligned with the slopes.
     * Value changes can cause abrupt changes in output, especially far away from the origin,
     * so this parameter is not well suited for animation or for modulation by other functions.
     * */
    var cellSize = 0.7f

    /**
     * The degree of normalization applied in the Phacelle noise, between 0 and 1.
     * The erosion filter depends on a certain consistency in magnitude of the Phacelle output.
     * However, high values can create loopy results where ridges and creases meet up at a point,
     * which produces unnatural looking results.
     * */
    var normalization = 0.5f

    /**
     * The lacunarity controls the frequency (the inverse horizontal scale) of each octave relative to the last.
     * */
    var lacunarity = 2.0f

    /**
     * The gain controls the magnitude (the vertical scale) of each octave relative to the last.
     * */
    var falloff = 0.5f

    /**
     * Control over whether the erosion effect raises or lowers the terrain.
     *  x: An offset value between -1 and 1, where a value of -1 only lowers, while
     *     1 only raises. The offset is proportional to the erosion strength
     *     parameter, so if that parameter is the same for the entire terrain, the
     *     effect of the height offset will move the entire terrain surface up or
     *     down by the same emount.
     *  y: A value between 0 and 1 which is the degree to which the offset value is
     *     replaced by the negated erosion fade target value. This has the effect
     *     of only raising at valleys and only lowering at peaks, which, due to how
     *     the erosion filter works, has the effect of largely preserving the minima
     *     and maxima of the terrain.
     * */
    var heightOffset = Vector2f(0f, 0f)

    var defaultHeight = 0.45f
    var enableTrees = true

    /**
     * Returns gradient noise
     * From https://www.shadertoy.com/view/XdXBRH
     * */
    private fun noisedX(px: Float, py: Float): Float {
        val ix = floor(px)
        val iy = floor(py)
        val fx = px - ix
        val fy = py - iy

        val ux = fx * fx * fx * (fx * (fx * 6f - 15f) + 10f)
        val uy = fy * fy * fy * (fy * (fy * 6f - 15f) + 10f)
        val hash = baseNoise.hash
        val tmp = JomlPools.vec2f.borrow()
        val va = hash[ix, iy, tmp].dot(fx, fy)
        val vb = hash[ix + 1f, iy, tmp].dot(fx - 1f, fy)
        val vc = hash[ix, iy + 1f, tmp].dot(fx, fy - 1f)
        val vd = hash[ix + 1f, iy + 1f, tmp].dot(fx - 1f, fy - 1f)
        return va + ux * (vb - va) + uy * (vc - va) + ux * uy * (va - vb - vc + vd)
    }

    private fun powInv(t: Float, power: Float): Float {
        return 1f - pow(1f - clamp(t), power)
    }

    private fun easeOut(t: Float): Float {
        // Flip by subtracting from one.
        val v = 1f - clamp(t)
        // Raise to a power of two and flip back.
        return 1f - v * v
    }

    private fun smoothStart(t: Float, smoothing: Float): Float {
        return if (t >= smoothing) t - 0.5f * smoothing
        else 0.5f * t * t / smoothing
    }

    private fun getSafeNormalizeFactor(nx: Float, ny: Float): Float {
        val l = hypot(nx, ny)
        return if (l > 1e-10f) 1f / l else 1f
    }

    /**
     * Used for tree coverage on the height map.
     * */
    fun calculateTreeCoverage(height: Float, normalY: Float, occlusion: Float, ridgeMap: Float): Float {
        val t0 = smoothstep(
            grassHeight + 0.05f,
            grassHeight + 0.01f,
            height + 0.01f + (occlusion - 0.8f) * 0.05f
        )
        val t1 = smoothstep(
            0.0f,
            0.4f,
            occlusion
        )
        val t2 = smoothstep(0.95f, 1.0f, normalY)
        val t3 = smoothstep(-1.4f, 0.0f, ridgeMap)
        var x = t0 * t1 * t2 * t3
        if (addWater) {
            x *= smoothstep(
                waterHeight + 0.000f,
                waterHeight + 0.007f,
                height
            )
        }
        return (x - 0.5f) / 0.6f
    }

    operator fun get(px: Float, py: Float): Float {
        val tmp = JomlPools.vec4f.create()
        val h = this[px, py, tmp].x
        JomlPools.vec4f.sub(1)
        return h
    }

    /**
     * Applies erosion onto baseline heightmap,
     *  x: eroded height
     *  y: erosion delta, [0,1]
     *  z: ridge-map [0,1]
     *  w: trees [0,1]
     * */
    operator fun get(px: Float, py: Float, dst: Vector4f): Vector4f {

        val heightAndSlope = sampleTerrain(px, py, JomlPools.vec3f.create())

        // Define the erosion fade target based on the altitude of the pre-eroded terrain.
        // The fade target should strive to be -1 at valleys and 1 at peaks, but overshooting is ok.
        var fadeTarget = (heightAndSlope.x - defaultHeight) / 0.15f

        // Store erosion in h (x : height delta, yz : slope delta, w : magnitude).
        // The output ridge map is -1 on creases and 1 on ridges.
        // The output debug value can be set to various values inside the erosion function.

        // Advanced Terrain Erosion Filter copyright (c) 2025 Rune Skovbo Johansen
        // This Source Code Form is subject to the terms of the Mozilla Public
        // License, v. 2.0. If a copy of the MPL was not distributed with this
        // file, You can obtain one at https://mozilla.org/MPL/2.0/.
        val gullyWeight = gullyWeight
        val detail = detail
        val rounding = rounding
        val onset = erosionOnset
        val assumedSlope = assumedSlope
        val scale = scale
        val octaves = erosionOctaves
        val lacunarity = lacunarity
        val falloff = falloff
        val cellScale = cellSize
        val normalization = normalization

        var strength = strength * scale
        fadeTarget = absClamp(fadeTarget, 1f)

        val inputHeight = heightAndSlope.x
        var freq = 1f / (scale * cellScale)
        val slopeLength = max(hypot(heightAndSlope.y, heightAndSlope.z), 1e-10f)
        var magnitude = 0f
        var roundingMultiplier = 1f

        val roundingForInput = mix(rounding.y, rounding.x, clamp(fadeTarget + 0.5f)) * rounding.z
        // The combined accumulating mask, based first on initial slope, and later on slope of each octave too.
        var combiMask = easeOut(smoothStart(slopeLength * onset.x, roundingForInput * onset.x))

        // Initialize the ridgeMap fadeTarget and mask.
        var ridgeMapCombiMask = easeOut(slopeLength * onset.z)
        var ridgeMapFadeTarget = fadeTarget

        // Determining the strength of the initial slope used for gully directions
        // based on the specified mix of the actual slope and an assumed slope.
        var gullySlopeX = mix(heightAndSlope.y, heightAndSlope.y / slopeLength * assumedSlope.x, assumedSlope.y)
        var gullySlopeY = mix(heightAndSlope.z, heightAndSlope.z / slopeLength * assumedSlope.x, assumedSlope.y)

        val phacelle = JomlPools.vec4f.create()
        repeat(octaves) {
            // Calculate and add gullies to the height and slope.
            val sng = getSafeNormalizeFactor(gullySlopeX, gullySlopeY)
            val phacelle = baseNoise[
                px * freq, py * freq,
                gullySlopeX * sng, gullySlopeY * sng,
                cellScale, 0.25f, normalization, phacelle
            ]

            // Multiply with freq since p was multiplied with freq.
            // Negate since we use slope directions that point down.
            phacelle.z *= -freq
            phacelle.w *= -freq

            // Amount of slope as value from 0 to 1.
            val sloping = abs(phacelle.y)

            // Add non-masked, normalized slope to gullySlope, for use by subsequent octaves.
            // It's normalized to use the steepest part of the sine wave everywhere.
            gullySlopeX += sign(phacelle.y) * phacelle.z * strength * gullyWeight
            gullySlopeY += sign(phacelle.y) * phacelle.w * strength * gullyWeight

            // Handle height offset and approximate output slope.

            // Gullies has height offset (from -1 to 1) in x and derivative in yz.
            val gulliesX = phacelle.x
            val gulliesY = phacelle.y * phacelle.z
            val gulliesZ = phacelle.y * phacelle.w

            // Fade gullies towards fadeTarget based on combiMask.
            val fadedGulliesX = mix(fadeTarget, gulliesX * gullyWeight, combiMask)
            val fadedGulliesY = mix(0f, gulliesY * gullyWeight, combiMask)
            val fadedGulliesZ = mix(0f, gulliesZ * gullyWeight, combiMask)

            // Apply height offset and derivative (slope) according to strength of current octave.
            heightAndSlope.x += fadedGulliesX * strength
            heightAndSlope.y += fadedGulliesY * strength
            heightAndSlope.z += fadedGulliesZ * strength

            magnitude += strength

            // Update fadeTarget to include the new octave.
            fadeTarget = fadedGulliesX

            // Update the mask to include the new octave.
            val roundingForOctave = mix(rounding.y, rounding.x, clamp(phacelle.x + 0.5f)) * roundingMultiplier
            val newMask = easeOut(smoothStart(sloping * onset.y, roundingForOctave * onset.y))
            combiMask = powInv(combiMask, detail) * newMask

            // Update the ridgeMap fadeTarget and mask.
            ridgeMapFadeTarget = mix(ridgeMapFadeTarget, gulliesX, ridgeMapCombiMask)
            val newRidgeMapMask = easeOut(sloping * onset.w)
            ridgeMapCombiMask *= newRidgeMapMask

            // Prepare the next octave.
            strength *= falloff
            freq *= lacunarity
            roundingMultiplier *= rounding.w
        }

        val ridgeMap = ridgeMapFadeTarget * (1f - ridgeMapCombiMask)

        val deltaHeight = heightAndSlope.x - inputHeight
        val erosion = deltaHeight / magnitude

        // Offset according to the height offset parameter by multiplying it with the magnitude.
        val offset = mix(heightOffset.x, -fadeTarget, heightOffset.y) * magnitude
        var eroded = heightAndSlope.x + offset

        // Add trees to terrain.
        var trees = -1f
        if (enableTrees) {
            val dx = heightAndSlope.y
            val dy = heightAndSlope.z
            val normalY = 1f / sqrt(1f + sq(dx, dy))
            val treesAmount = calculateTreeCoverage(eroded, normalY, erosion + 0.5f, ridgeMap)
            trees = (1f - sq((noisedX(px + 0.5f, py + 0.5f) * 200.0f) * 0.5f + 0.5f) - 1f + treesAmount) * 1.5f
            if (trees > 0f) {
                eroded += trees / 300f
            }
        }

        JomlPools.vec3f.sub(1)
        JomlPools.vec4f.sub(1)

        return dst.set(
            eroded,
            clamp(erosion * 0.5f + 0.5f), // Erosion delta as [0, 1] value.
            clamp(ridgeMap * 0.5f + 0.5f), // Ridge map as [0, 1] value.
            clamp(trees * 0.5f + 0.5f), // Tree value as [0, 1] value.
        )
    }
}