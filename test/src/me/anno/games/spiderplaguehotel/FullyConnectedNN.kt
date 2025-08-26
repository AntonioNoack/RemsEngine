package me.anno.games.spiderplaguehotel

import me.anno.maths.noise.RandomUtils.addGaussianNoise
import me.anno.utils.assertions.assertEquals
import kotlin.math.exp
import kotlin.random.Random

// use a library from us for that?
//  or do we just rewrite it? it's fast, so let's do it :D
/**
 * simple, fully connected neural network with sigmoid activation function
 * */
class FullyConnectedNN(private val layerSizes: IntArray) {

    val numNodes = layerSizes.sum()

    fun initRandomly(random: Random) {
        val weights = FloatArray(numWeights(layerSizes))
        random.addGaussianNoise(weights, 1f)
        this.weights = weights
    }

    lateinit var weights: FloatArray

    fun copyInto(dst: FullyConnectedNN) {
        weights.copyInto(dst.weights)
    }

    fun mutate(rate: Float, random: Random) {
        random.addGaussianNoise(weights, rate)
    }

    fun predict(values: FloatArray) {
        assertEquals(numNodes, values.size)
        var vi = 0
        var wi = 0
        val sizes = layerSizes
        for (layerIndex in 1 until sizes.size) {
            val prevSize = sizes[layerIndex - 1]
            val currSize = sizes[layerIndex]
            vi += prevSize
            predict(values, wi, vi, prevSize, currSize)
            wi += (prevSize + 1) * currSize
        }
        assertEquals(weights.size, wi)
    }

    private fun predict(
        values: FloatArray,
        wi0: Int, vi0: Int,
        prevSize: Int, currSize: Int,
    ) {
        var wi = wi0
        var vi = vi0
        val weights = weights
        repeat(currSize) {
            var sum = weights[wi++]
            for (srcIndex in 0 until prevSize) {
                sum += weights[wi++] * values[srcIndex]
            }
            values[vi++] = sigmoid(sum)
        }
    }

    companion object {

        fun numWeights(sizes: IntArray): Int {
            var numWeights = 0
            for (i in 1 until sizes.size) {
                numWeights += (sizes[i - 1] + 1) * sizes[i]
            }
            return numWeights
        }

        fun sigmoid(x: Float): Float {
            return 1f / (1f + exp(-x))
        }
    }
}