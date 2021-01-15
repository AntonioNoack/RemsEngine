package audacity.soundtouch

interface FloatPtr {
    operator fun plus(deltaOffset: Int): FloatPtr
    operator fun get(index: Int): Float
    operator fun set(index: Int, value: Float)
}