package me.anno.audio

import me.anno.audio.openal.SoundBuffer
import me.anno.cache.AsyncCacheData
import me.anno.cache.CacheSection
import me.anno.io.files.FileReference

object AudioCache : CacheSection<AudioSliceKey, SoundBuffer>("Audio") {
    const val playbackSampleRate = 48000
    var getAudioSequence: ((
        file: FileReference, startTime: Double, duration: Double,
        sampleRate: Int, result: AsyncCacheData<SoundBuffer>
    ) -> Unit)? = null
}