package me.anno.objects.cache

import me.anno.gpu.GFX
import me.anno.video.FFMPEGStream
import me.anno.video.Frame
import java.io.File
import kotlin.math.abs

class VideoCache(val file: File){

    // todo add audio component...

    var startTime = 0f
    var endTime = 1e9f

    var fps = 24f

    val duration get() = endTime - startTime

    val framesPerContainer = 16
    val frameContainers = HashMap<Int, FFMPEGStream>()

    val bufferTimeout = 1_500_000_000 // 1.5s

    fun getFrame(frameIndex: Int): Frame? {
        val timeForExpiration = GFX.lastTime
        if(frameIndex >= framesPerContainer) getFrame(timeForExpiration, frameIndex - framesPerContainer, true)
        getFrame(timeForExpiration, frameIndex + framesPerContainer, true)
        val result = getFrame(timeForExpiration, frameIndex, true)
        if(Math.random() < 0.1){
            val toRemove = frameContainers.filter { (_, stream) -> abs(stream.lastUsedTime - timeForExpiration) > bufferTimeout }.toList()
            for(entry in toRemove){
                frameContainers.remove(entry.first)
                entry.second.destroy()
            }
        }
        return result
    }

    fun getFrame(time: Long, frameIndex: Int, loadIfMissing: Boolean): Frame? {
        val containerIndex = frameIndex / framesPerContainer
        val container = frameContainers[containerIndex] ?: if(loadIfMissing) requestContainer(containerIndex) else return null
        container.lastUsedTime = time
        val frames = container.frames
        val localFrameIndex = frameIndex % framesPerContainer
        return frames.getOrNull(localFrameIndex) ?:
            if(localFrameIndex > 0)
                frames.getOrNull(localFrameIndex-1)
            else
                getFrame(time, frameIndex-1, false)
    }

    fun requestContainer(index: Int): FFMPEGStream {
        val minFrame = index * framesPerContainer
        // val maxFrame = minFrame + framesPerContainer
        val startTime = minFrame / fps
        val stream = FFMPEGStream.getImageSequence(file, startTime, framesPerContainer, fps)
        frameContainers[index] = stream
        return stream
    }

}