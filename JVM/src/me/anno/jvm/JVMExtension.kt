package me.anno.jvm

import me.anno.audio.openal.AudioManager
import me.anno.config.DefaultStyle
import me.anno.extensions.plugins.Plugin
import me.anno.fonts.signeddistfields.Contour
import me.anno.image.thumbs.Thumbs
import me.anno.io.MediaMetadata
import me.anno.io.utils.LinkCreator
import me.anno.io.utils.TrashManager
import me.anno.jvm.fonts.ContourImpl
import me.anno.jvm.fonts.FontManagerImpl
import me.anno.jvm.images.ImageWriterImpl
import me.anno.jvm.images.MetadataImpl
import me.anno.jvm.images.ThumbsImpl
import me.anno.language.spellcheck.Spellchecking
import me.anno.utils.SafeInit.initSafely
import me.anno.utils.types.Ints.toIntOrDefault
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.lang.management.ManagementFactory
import javax.sound.sampled.AudioSystem

// todo move all Java-exclusive things here as far as possible...
//  (things that will be unavailable on Android/Web/KotlinNative)
//  - ServerSockets (?)
//  - OpenAL (?)
//  - getReference()-Implementation?
//  - Extension discovery?? -> hard to do as an extension...

class JVMExtension : Plugin() {
    override fun onEnable() {
        super.onEnable()
        initSafely(::initUnsafely)
    }

    private fun initUnsafely(step: Int): Boolean { // I hope the generated code isn't too bad/slow :/
        when (step) { // components may be removed to reduce executable size
            0 -> getLogger().info("Process ID: ${getProcessID()}")
            1 -> ClipboardImpl.register()
            2 -> OpenFileExternallyImpl.register()
            3 -> MediaMetadata.registerSignatureHandler(100, "ImageIO", MetadataImpl::readImageIOMetadata)
            4 -> ImageWriterImpl.register()
            5 -> AWTRobot.register()
            6 -> FontManagerImpl.register()
            7 -> Contour.calculateContoursImpl = ContourImpl::calculateContours
            8 -> FileWatchImpl.register()
            9 -> Spellchecking.checkImpl = SpellcheckingImpl::check
            10 -> AudioManager.audioDeviceHash = { AudioSystem.getMixerInfo()?.size ?: -1 }
            11 -> TrashManager.moveToTrashImpl = TrashManagerImpl::moveToTrash
            12 -> LinkCreator.createLink = FileExplorerImpl::createLink
            13 -> DefaultStyle.initDefaults() // reload default font size
            14 -> Thumbs.registerSignatures("exe", ThumbsImpl::generateSystemIcon)
            else -> return true
        }
        return false
    }

    private fun getLogger(): Logger {
        return LogManager.getLogger(JVMExtension::class)
    }

    private fun getProcessID(): Int {
        return ManagementFactory.getRuntimeMXBean().name.split("@")[0].toIntOrDefault(-1)
    }
}