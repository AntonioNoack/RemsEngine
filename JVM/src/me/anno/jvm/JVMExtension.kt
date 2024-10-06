package me.anno.jvm

import me.anno.audio.openal.AudioManager
import me.anno.config.DefaultStyle
import me.anno.extensions.events.EventHandler
import me.anno.extensions.events.GameLoopStartEvent
import me.anno.extensions.plugins.Plugin
import me.anno.fonts.signeddistfields.Contour
import me.anno.gpu.GFX
import me.anno.image.thumbs.Thumbs
import me.anno.io.MediaMetadata
import me.anno.io.utils.LinkCreator
import me.anno.io.utils.TrashManager
import me.anno.jvm.fonts.ContourImpl
import me.anno.jvm.fonts.FontManagerImpl
import me.anno.jvm.images.ImageIOImpl
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
//  - OpenAL (?)
//  - getReference()-Implementation?
//  - java.io.File-usage
//  - java.nio.Files-usage
//  - Extension discovery?? -> hard to do as an extension (recursive issue)

class JVMExtension : Plugin() {
    override fun onEnable() {
        super.onEnable()
        registerListener(this)
        initSafely(::initUnsafely)
    }

    override fun onDisable() {
        super.onDisable()
        unregisterListener(this)
    }

    @EventHandler
    fun onGameLoopStart(event: GameLoopStartEvent) {
        GLFWController.pollControllers(GFX.someWindow)
    }

    private fun initUnsafely(step: Int): Boolean { // I hope the generated code isn't too bad/slow :/
        when (step) { // components may be removed to reduce executable size
            0 -> getLogger().info("Process ID: ${getProcessID()}")
            1 -> ClipboardImpl.register()
            2 -> OpenFileExternallyImpl.register()
            3 -> MediaMetadata.registerSignatureHandler(100, "ImageIO", MetadataImpl::readImageIOMetadata)
            4 -> ImageWriterImpl.register()
            5 -> ImageIOImpl.register() // if Image-plugin isn't available, we should still support the default formats
            6 -> AWTRobot.register()
            7 -> FontManagerImpl.register()
            8 -> Contour.calculateContoursImpl = ContourImpl::calculateContours
            9 -> FileWatchImpl.register()
            10 -> Spellchecking.checkImpl = SpellcheckingImpl::check
            11 -> AudioManager.audioDeviceHash = { AudioSystem.getMixerInfo()?.size ?: -1 }
            12 -> TrashManager.moveToTrashImpl = TrashManagerImpl::moveToTrash
            13 -> LinkCreator.createLink = FileExplorerImpl::createLink
            14 -> DefaultStyle.initDefaults() // reload default font size
            15 -> Thumbs.registerSignatures("exe", ThumbsImpl::generateSystemIcon)
            16 -> LoggerOverride.setup()
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