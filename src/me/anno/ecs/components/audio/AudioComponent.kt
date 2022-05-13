package me.anno.ecs.components.audio

import me.anno.ecs.Component
import me.anno.ecs.annotations.Docs
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef

class AudioComponent : AudioComponentBase() {

    var source: FileReference = InvalidRef

    // most tracks are short, so keep them in memory by default
    @Docs("Keeps the track in memory, so it can be started without delay")
    var keepInMemory = true

    override fun clone(): Component {
        val clone = AudioComponent()
        copy(clone)
        return clone
    }

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as AudioComponent
        clone.source = source
        clone.keepInMemory = keepInMemory
    }

    private fun keepInMemory() {
        // todo calculate number of buffers
        val duration = 10

        var numBuffers = 10
        for(i in 0 until numBuffers){
            // todo keep buffer in memory
        }
    }

    fun isFullyLoaded(){
        // todo check whether all buffers are in memory
    }

    override fun onUpdate(): Int {
        var ret = 30
        if (keepInMemory) {
            keepInMemory()
            ret = 5
        }
        return ret
    }

    override val className = "AudioComponent"

}