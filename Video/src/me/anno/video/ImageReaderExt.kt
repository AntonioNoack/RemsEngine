package me.anno.video

import me.anno.image.Image
import me.anno.image.raw.GPUFrameImage
import me.anno.io.MediaMetadata
import me.anno.io.files.FileFileRef
import me.anno.io.files.FileReference
import me.anno.utils.Sleep
import me.anno.utils.async.Callback
import me.anno.utils.async.Callback.Companion.then
import me.anno.video.ffmpeg.FFMPEGStream
import java.io.IOException
import kotlin.math.min

object ImageReaderExt {

    private fun frameIndex(meta: MediaMetadata): Int {
        return min(20, (meta.videoFrameCount - 1) / 3)
    }

    fun tryFFMPEG(file: FileReference, signature: String?, forGPU: Boolean, callback: Callback<Image>) {
        val file = file.resolved()
        if (file is FileFileRef) {
            tryFFMPEGImpl(file, signature, forGPU, callback)
        } else {
            // todo when we have native ffmpeg, don't copy the file
            val tmp = FileFileRef.createTempFile("4ffmpeg", file.extension)
            file.readBytes { bytes, e ->
                if (bytes != null) {
                    tmp.writeBytes(bytes)
                    tryFFMPEG(tmp, signature, forGPU, callback.then { _, _ -> tmp.delete() })
                } else callback.call(null, e)
            }
        }
    }

    private fun tryFFMPEGImpl(file: FileFileRef, signature: String?, forGPU: Boolean, callback: Callback<Image>) {
        MediaMetadata.getMeta(file).waitFor { meta ->
            if (meta == null || !meta.hasVideo || meta.videoFrameCount < 1) {
                callback.err(IOException("Meta for $file is missing video"))
            } else if (forGPU) {
                tryFFMPEGForGPU(file, signature, meta, callback)
            } else {
                tryFFMPEGForCPU(file, signature, meta, callback)
            }
        }
    }

    private fun tryFFMPEGForGPU(file: FileFileRef, signature: String?, meta: MediaMetadata, callback: Callback<Image>) {
        FFMPEGStream.getImageSequenceGPU(
            file, signature, meta.videoWidth, meta.videoHeight,
            frameIndex(meta), 1, meta.videoFPS,
            meta.videoWidth, meta.videoFPS, meta.videoFrameCount, {}, { frames ->
                val frame = frames.firstOrNull()
                if (frame != null) {
                    Sleep.waitUntil("ImageReaderExt:forGPU:created", true, {
                        frame.isCreated || frame.isDestroyed
                    }, {
                        val image = GPUFrameImage(frame)
                        image.flipY()
                        callback.call(image, null)
                    })
                } else callback.err(IOException("No frame was found in $file"))
            }
        )
    }

    private fun tryFFMPEGForCPU(file: FileFileRef, signature: String?, meta: MediaMetadata, callback: Callback<Image>) {
        FFMPEGStream.getImageSequenceCPU(
            file, signature, meta.videoWidth, meta.videoHeight,
            frameIndex(meta), 1, meta.videoFPS,
            meta.videoWidth, meta.videoFPS, meta.videoFrameCount, {}, { frames ->
                val frame = frames.firstOrNull()
                if (frame != null) callback.call(frame, null)
                else callback.err(IOException("No frame was found"))
            }
        )
    }
}