package me.anno.video

import me.anno.audio.AudioCache
import me.anno.extensions.plugins.Plugin
import me.anno.image.ImageAsFolder
import me.anno.installer.Installer
import me.anno.io.MediaMetadata
import me.anno.io.files.FileFileRef
import me.anno.io.files.WebRef
import me.anno.utils.OS
import me.anno.utils.OSFeatures
import me.anno.video.FFMPEGMetadata.loadFFMPEG
import me.anno.video.ffmpeg.FFMPEG
import me.anno.video.ffmpeg.FFMPEG.ffmpegPath
import me.anno.video.ffmpeg.FFMPEG.ffprobePath
import me.anno.video.ffmpeg.FFMPEGStream

class VideoPlugin : Plugin() {
    override fun onEnable() {
        super.onEnable()
        registerProxyCreator()
        registerVideoStream()
        registerImageReader()
        registerAudioStream()
        registerMediaMetadata()
    }

    private fun registerProxyCreator() {
        VideoCache.generateVideoFrames = VideoCacheImpl::generateVideoFrames
        VideoCache.getProxyFile = VideoProxyCreator::getProxyFile
        VideoCache.getProxyFileDontUpdate = VideoProxyCreator::getProxyFileDontUpdate
    }

    private fun registerVideoStream() {
        VideoStream.runVideoStreamWorker = VideoStreamWorker.Companion::runVideoStreamWorker
    }

    private fun registerAudioStream() {
        AudioCache.getAudioSequence = { file, startTime, duration, sampleRate, result ->
            FFMPEGStream.getAudioSequence(file, startTime, duration, sampleRate, result)
        }
    }

    private fun registerImageReader() {
        ImageAsFolder.tryFFMPEG = ImageReaderExt::tryFFMPEG
    }

    private fun registerMediaMetadata() {
        MediaMetadata.registerSignatureHandler(100, "video") { file, signature, dst, _ ->
            // only use ffmpeg for ffmpeg files
            if (signature == "gif" || signature == "media" || signature == "dds") {
                if (OSFeatures.supportsFFMPEG) {
                    val file = file.resolved()
                    if (file is FileFileRef || file is WebRef) {
                        dst.loadFFMPEG()
                    }
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
        ImageAsFolder.tryFFMPEG = null
        AudioCache.getAudioSequence = null
        MediaMetadata.unregister("video")
    }

    companion object {
        @JvmStatic
        @Suppress("unused")
        fun checkFFMPEGInstall() {
            if (!FFMPEG.isInstalled && OS.isWindows) {
                // todo update FFMPEG download, if possible
                Installer.downloadMaybe("ffmpeg/bin/ffmpeg.exe", ffmpegPath)
                Installer.downloadMaybe("ffmpeg/bin/ffprobe.exe", ffprobePath)
            }
        }
    }
}