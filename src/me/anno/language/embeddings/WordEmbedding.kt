package me.anno.language.embeddings

import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

class WordEmbedding {

    var maxDimension = 0

    val values = HashMap<String, FloatArray>()
    val dictionary = HashMap<String, Int>()

    fun create() = FloatArray(maxDimension + 1)

    fun register(name: String, index: Int, value: Float = 1f) {
        maxDimension = max(maxDimension, index)
        dictionary[name] = index
        register(name, mapOf(index to value))
    }

    fun registerChain(names: List<String>, startIndex: Int, delta: Float = 0.3f) {
        for (index in names.indices) register(names[index], startIndex + index)
        for (index in 0 until names.size - 1) {
            similar(names[index], names[index + 1], delta)
        }
    }

    fun similar(name1: String, name2: String, delta: Float = 0.3f) {
        val index2 = dictionary[name2]
        if (index2 != null) add(name1, index2, delta)
        else add(name1, values[name2] ?: throw IndexOutOfBoundsException("$name2 is missing"), delta)
        val index1 = dictionary[name1]
        if (index1 != null) add(name1, index1, delta)
        else add(name2, values[name1] ?: throw IndexOutOfBoundsException("$name1 is missing"), delta)
    }

    fun add(name: String, index: Int, value: Float) {
        maxDimension = max(maxDimension, index)
        val oldValue = values[name]!!
        if (oldValue.size <= index) {
            val clone = FloatArray(index + 1)
            System.arraycopy(oldValue, 0, clone, 0, oldValue.size)
            clone[index] += value
            values[name] = clone
        } else {
            oldValue[index] += value
        }
    }

    fun add(name: String, index: FloatArray, scale: Float) {
        val oldValue = values[name]!!
        if (oldValue.size <= index.size) {
            val clone = FloatArray(index.size)
            System.arraycopy(oldValue, 0, clone, 0, oldValue.size)
            add(clone, index, scale)
            values[name] = clone
        } else {
            add(oldValue, index, scale)
        }
    }

    fun inverse(name1: String, name2: String, delta: Float = 1f) {
        similar(name1, name2, -delta)
    }

    fun register(name: String, embedding: Map<Int, Float>) {
        val embedded = FloatArray((embedding.keys.maxOrNull() ?: -1) + 1)
        for ((index, value) in embedding) {
            embedded[index] = value
        }
        register(name, embedded)
    }

    fun register2(name: String, embedding: Map<String, Float>) {
        val embedded = FloatArray((embedding.keys.maxOfOrNull { dictionary[it]!! } ?: -1) + 1)
        for ((index, value) in embedding) {
            embedded[dictionary[index]!!] = value
        }
        register(name, embedded)
    }

    fun synonym(name: String, embedding: String) {
        values[name] = values[embedding]!!
    }

    fun clone(name: String, embedding: String) {
        values[name] = copy(values[embedding]!!)
    }

    private fun copy(fa: FloatArray) = FloatArray(fa.size) { fa[it] }

    fun register(name: String, embedding: FloatArray) {
        values[name] = embedding
    }

    fun find(name: String): FloatArray? = values[name]

    fun find(toMatch: FloatArray, minMatch: Float = 0f): String? {
        var bestScore = minMatch
        var bestWord: String? = null
        for ((word, embedded) in values) {
            val score = dot(toMatch, embedded)
            if (score > bestScore) {
                bestScore = score
                bestWord = word
            }
        }
        return bestWord
    }

    fun find(values: List<FloatArray?>, toMatch: FloatArray, minMatch: Float = 0f): Int {
        var bestScore = minMatch
        var bestWord = -1
        for (index in values.indices) {
            val embedded = values[index] ?: continue
            val score = dot(toMatch, embedded)
            if (score > bestScore) {
                bestScore = score
                bestWord = index
            }
        }
        return bestWord
    }

    fun dot(a: FloatArray, b: FloatArray): Float {
        var dot = 0f
        for (i in 0 until min(a.size, b.size)) {
            dot += a[i] * b[i]
        }
        return dot
    }

    fun add(dst: FloatArray?, src: FloatArray, scale: Float = 1f): FloatArray {
        if (dst == null || dst.size < src.size) {
            val clone = FloatArray(src.size)
            if (dst != null) System.arraycopy(dst, 0, clone, 0, dst.size)
            return add(clone, src)
        }
        for (i in src.indices) {
            dst[i] += src[i] * scale
        }
        return dst
    }

    fun scale(dst: FloatArray, scale: Float) {
        for (i in dst.indices) {
            dst[i] *= scale
        }
    }

    fun normalize() {
        for ((_, value) in values) {
            scale(value, 1f / sqrt(dot(value, value)))
        }
    }

    // todo train and such...

}