package audacity.soundtouch

import me.anno.utils.structures.arrays.FloatArrayList

class FloatPtrArrList(val array: FloatArrayList, val offset: Int): FloatPtr {
    override operator fun plus(deltaOffset: Int) = FloatPtrArrList(array, offset + deltaOffset)
    override operator fun get(index: Int) = array[index + offset]
    override operator fun set(index: Int, value: Float){
        array[index + offset] = value
    }
}