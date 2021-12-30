package me.anno.audio.effects

import me.anno.io.Saveable
import me.anno.io.text.TextReader
import me.anno.objects.Audio
import me.anno.objects.Camera
import me.anno.objects.inspectable.Inspectable

abstract class SoundEffect(val inputDomain: Domain, val outputDomain: Domain) : Saveable(),
    Inspectable {

    // for the inspector
    lateinit var audio: Audio

    abstract fun apply(
        getDataSrc: (Int) -> FloatArray,
        dataDst: FloatArray,
        source: Audio,
        destination: Camera,
        time0: Time, time1: Time,
    )

    abstract fun getStateAsImmutableKey(
        source: Audio,
        destination: Camera,
        time0: Time, time1: Time
    ): Any

    abstract val displayName: String
    abstract val description: String

    open fun clone() = TextReader.read(toString(), true).first() as SoundEffect

    override val approxSize = 10
    override fun isDefaultValue() = false

    companion object {
        fun copy(src: FloatArray, dst: FloatArray){
            System.arraycopy(src, 0, dst, 0, src.size)
        }
    }

}