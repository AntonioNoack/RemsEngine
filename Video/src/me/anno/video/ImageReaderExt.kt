package me.anno.video

import me.anno.image.ImageCallback
import me.anno.image.ImageReader
import me.anno.image.raw.GPUFrameImage
import me.anno.io.files.FileFileRef
import me.anno.io.files.FileReference
import me.anno.utils.Sleep
import me.anno.video.ffmpeg.FFMPEGStream
import me.anno.io.MediaMetadata
import me.anno.io.files.Reference.getReference
import java.io.IOException

object ImageReaderExt {
    fun tryFFMPEG(file: FileReference, signature: String?, forGPU: Boolean, callback: ImageCallback) {
        if (file is FileFileRef) {
            val meta = MediaMetadata.getMeta(file, false)
            if (meta == null || !meta.hasVideo || meta.videoFrameCount < 1) {
                callback(null, IOException("Meta for $file is missing video"))
            } else if (forGPU) {
                FFMPEGStream.getImageSequenceGPU(
                    file, signature, meta.videoWidth, meta.videoHeight,
                    ImageReader.frameIndex(meta), 1, meta.videoFPS,
                    meta.videoWidth, meta.videoFPS, meta.videoFrameCount, {}, { frames ->
                        val frame = frames.firstOrNull()
                        if (frame != null) {
                            Sleep.waitForGFXThread(true) { frame.isCreated || frame.isDestroyed }
                            callback(GPUFrameImage(frame), null)
                        } else callback(null, IOException("No frame was found"))
                    }
                )
            } else {
                FFMPEGStream.getImageSequenceCPU(
                    file, signature, meta.videoWidth, meta.videoHeight,
                    ImageReader.frameIndex(meta), 1, meta.videoFPS,
                    meta.videoWidth, meta.videoFPS, meta.videoFrameCount, {}, { frames ->
                        val frame = frames.firstOrNull()
                        if (frame != null) callback(frame, null)
                        else callback(null, IOException("No frame was found"))
                    }
                )
            }
        } else {
            // todo when we have native ffmpeg, don't copy the file
            val tmp = FileFileRef.createTempFile("4ffmpeg", file.extension)
            file.readBytes { bytes, e ->
                if (bytes != null) {
                    tmp.writeBytes(bytes)
                    tryFFMPEG(getReference(tmp), signature, forGPU, callback)
                    tmp.delete()
                } else callback(null, e)
            }
        }
    }
}