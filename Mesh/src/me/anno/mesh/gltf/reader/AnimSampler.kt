package me.anno.mesh.gltf.reader

/**
 * Combines timestamps with a sequence of output values and defines an interpolation algorithm.
 * */
class AnimSampler {
    var times: FloatArray? = null // times
    var values: FloatArray? = null // values
    var interpolation = "LINEAR"
}