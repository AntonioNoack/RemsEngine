package me.anno.video

import me.anno.audio.AudioCache
import me.anno.extensions.plugins.Plugin
import me.anno.image.ImageReader
import me.anno.installer.Installer
import me.anno.io.files.FileFileRef
import me.anno.io.files.WebRef
import me.anno.utils.OS
import me.anno.video.FFMPEGMetadata.loadFFMPEG
import me.anno.video.ffmpeg.FFMPEG
import me.anno.video.ffmpeg.FFMPEG.ffmpegPath
import me.anno.video.ffmpeg.FFMPEG.ffprobePath
import me.anno.video.ffmpeg.FFMPEGStream
import me.anno.io.MediaMetadata

class VideoPlugin : Plugin() {
    override fun onEnable() {
        super.onEnable()
        VideoCache.generateVideoFrames = VideoCacheImpl::generateVideoFrames
        VideoCache.getProxyFile = VideoProxyCreator::getProxyFile
        VideoCache.getProxyFileDontUpdate = VideoProxyCreator::getProxyFileDontUpdate
        VideoStream.runVideoStreamWorker = VideoStreamWorker::runVideoStreamWorker
        ImageReader.tryFFMPEG = ImageReaderExt::tryFFMPEG
        AudioCache.getAudioSequence = { file, startTime, duration, sampleRate ->
            // why is it not possible to assign directly???
            FFMPEGStream.getAudioSequence(file, startTime, duration, sampleRate)
        }
        MediaMetadata.registerSignatureHandler(100, "video") { file, signature, dst ->
            // only load ffmpeg for ffmpeg files
            if (signature == "gif" || signature == "media" || signature == "dds") {
                if (!OS.isAndroid && (file is FileFileRef || file is WebRef)) {
                    dst.loadFFMPEG()
                }
                true
            } else false
        }
    }

    override fun onDisable() {
        super.onDisable()
        VideoCache.generateVideoFrames = null
        VideoCache.getProxyFile = null
        VideoStream.runVideoStreamWorker = null
        ImageReader.tryFFMPEG = null
        AudioCache.getAudioSequence = null
        MediaMetadata.unregister("video")
    }

    companion object {
        @JvmStatic
        @Suppress("unused")
        fun checkFFMPEGInstall() {
            if (!FFMPEG.isInstalled && OS.isWindows) {
                Installer.downloadMaybe("ffmpeg/bin/ffmpeg.exe", ffmpegPath)
                Installer.downloadMaybe("ffmpeg/bin/ffprobe.exe", ffprobePath)
            }
        }
    }
}