package audacity.soundtouch

class FloatPtrArr(val array: FloatArray, val offset: Int): FloatPtr {
    override operator fun plus(deltaOffset: Int) = FloatPtrArr(array, offset + deltaOffset)
    override operator fun get(index: Int) = array[index + offset]
    override operator fun set(index: Int, value: Float){
        array[index + offset] = value
    }
}