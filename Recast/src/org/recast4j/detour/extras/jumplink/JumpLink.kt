package org.recast4j.detour.extras.jumplink

class JumpLink {
    val numSpines = MAX_SPINE
    val spine0 = FloatArray(MAX_SPINE * 3)
    val spine1 = FloatArray(MAX_SPINE * 3)

    lateinit var startSamples: Array<GroundSample>
    lateinit var endSamples: Array<GroundSample>

    var start: GroundSegment? = null
    var end: GroundSegment? = null
    var trajectory: Trajectory? = null

    companion object {
        const val MAX_SPINE = 8
    }
}