package me.anno.mesh.gltf.reader

/**
 * Combines timestamps with a sequence of output values and defines an interpolation algorithm.
 * */
class AnimSampler {
    var input: FloatArray? = null // times
    var output: FloatArray? = null // values
    var interpolation = "LINEAR"
}