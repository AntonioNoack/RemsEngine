package me.anno.jvm

import com.sun.jna.platform.FileUtils
import me.anno.audio.openal.AudioManager
import me.anno.config.DefaultStyle
import me.anno.extensions.plugins.Plugin
import me.anno.jvm.fonts.ContourImpl
import me.anno.jvm.fonts.FontManagerImpl
import me.anno.fonts.signeddistfields.Contour
import me.anno.gpu.framebuffer.Screenshots
import me.anno.jvm.images.ImageImpl
import me.anno.jvm.images.MetadataImpl
import me.anno.jvm.images.ThumbsImpl
import me.anno.io.MediaMetadata
import me.anno.io.files.FileFileRef
import me.anno.io.files.FileReference
import me.anno.image.thumbs.Thumbs
import me.anno.io.utils.LinkCreator
import me.anno.io.utils.TrashManager
import me.anno.language.spellcheck.Spellchecking
import me.anno.ui.editor.files.FileExplorer
import me.anno.utils.types.Ints.toIntOrDefault
import org.apache.logging.log4j.LogManager
import java.io.IOException
import java.lang.management.ManagementFactory
import javax.sound.sampled.AudioSystem

// todo move all Java-exclusive things here as far as possible...
//  (things that will be unavailable on Android/Web/KotlinNative)
//  - ServerSockets (?)
//  - OpenAL (?)
//  - getReference()-Implementation?
//  - Extension discovery?? -> hard to do as an extension...

class JVMExtension : Plugin() {
    companion object {
        private val LOGGER = LogManager.getLogger(JVMExtension::class)
    }

    override fun onEnable() {
        super.onEnable()
        LOGGER.info("Process ID: ${getProcessID()}")
        ClipboardImpl.register()
        Thumbs.registerSignature("exe", ThumbsImpl::generateSystemIcon)
        OpenFileExternallyImpl.register()
        MediaMetadata.registerSignatureHandler(100, "ImageIO", MetadataImpl::readImageIOMetadata)
        Screenshots.takeSystemScreenshotImpl = AWTRobot::takeScreenshot
        Contour.calculateContoursImpl = ContourImpl::calculateContours
        ImageImpl.register()
        AWTRobot.register()
        FontManagerImpl.register()
        FileWatchImpl.register()
        Spellchecking.checkImpl = SpellcheckingImpl::check
        AudioManager.audioDeviceHash = { AudioSystem.getMixerInfo()?.size ?: -1 }
        TrashManager.moveToTrashImpl = this::moveToTrash
        LinkCreator.createLink = FileExplorerImpl::createLink
        DefaultStyle.initDefaults() // reload default font size
    }

    private fun getProcessID(): Int {
        return ManagementFactory.getRuntimeMXBean().name.split("@")[0].toIntOrDefault(-1)
    }

    private fun moveToTrash(files: List<FileReference>): Boolean {
        val fileUtils: FileUtils = FileUtils.getInstance()
        return if (fileUtils.hasTrash()) {
            val fileArray = files
                .filterIsInstance<FileFileRef>()
                .map { it.file }.toTypedArray()
            try {
                fileUtils.moveToTrash(*fileArray)
                true
            } catch (e: IOException) {
                e.printStackTrace()
                false
            }
        } else {
            LOGGER.warn("Trash is not available")
            false
        }
    }
}