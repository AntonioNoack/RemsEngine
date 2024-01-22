package me.anno

import com.sun.jna.platform.FileUtils
import me.anno.extensions.plugins.Plugin
import me.anno.fonts.AWTFont
import me.anno.fonts.ContourImpl
import me.anno.fonts.FontManager
import me.anno.fonts.FontManagerImpl
import me.anno.fonts.FontStats
import me.anno.fonts.signeddistfields.Contour
import me.anno.gpu.framebuffer.Screenshots
import me.anno.images.ImageImpl
import me.anno.images.MetadataImpl
import me.anno.images.ThumbsImpl
import me.anno.input.Clipboard
import me.anno.io.MediaMetadata
import me.anno.io.files.FileFileRef
import me.anno.io.files.thumbs.Thumbs
import me.anno.io.utils.TrashManager
import me.anno.utils.files.OpenFileExternally
import org.apache.logging.log4j.LogManager
import java.awt.font.FontRenderContext
import java.awt.font.TextLayout
import java.io.IOException

// todo move all Java-exclusive things here as far as possible...
//  (things that will be unavailable on Android/Web/KotlinNative)

class JVMPlugin : Plugin() {
    companion object {
        private val LOGGER = LogManager.getLogger(JVMPlugin::class)
    }

    override fun onEnable() {
        super.onEnable()
        Clipboard.setClipboardContentImpl = ClipboardImpl::setClipboardContent
        Clipboard.getClipboardContentImpl = ClipboardImpl::getClipboardContent
        Clipboard.copyFilesImpl = ClipboardImpl::copyFiles
        Thumbs.registerSignature("exe", ThumbsImpl::generateSystemIcon)
        OpenFileExternally.openInBrowserImpl = OpenFileExternallyImpl::openInBrowser
        OpenFileExternally.openInStandardProgramImpl = OpenFileExternallyImpl::openInStandardProgram
        OpenFileExternally.editInStandardProgramImpl = OpenFileExternallyImpl::editInStandardProgram
        OpenFileExternally.openInExplorerImpl = OpenFileExternallyImpl::openInExplorer
        MediaMetadata.registerSignatureHandler(100, "ImageIO", MetadataImpl::readImageIOMetadata)
        Screenshots.takeSystemScreenshotImpl = AWTRobot::takeScreenshot
        Contour.calculateContoursImpl = ContourImpl::calculateContours
        ImageImpl.register()
        AWTRobot.register()
        FontStats.getTextGeneratorImpl = FontManagerImpl::getTextGenerator
        FontStats.queryInstalledFontsImpl = FontManagerImpl::getInstalledFonts
        FontStats.getTextLengthImpl = { font, text ->
            val awtFont = (FontManager.getFont(font) as AWTFont).awtFont
            val ctx = FontRenderContext(null, true, true)
            TextLayout(text, awtFont, ctx).bounds.maxX
        }
        FontStats.getFontHeightImpl = { font ->
            val ctx = FontRenderContext(null, true, true)
            val layout = TextLayout(".", (FontManager.getFont(font) as AWTFont).awtFont, ctx)
            (layout.ascent + layout.descent).toDouble()
        }
        TrashManager.moveToTrashImpl = { files ->
            val fileUtils: FileUtils = FileUtils.getInstance()
            if (fileUtils.hasTrash()) {
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
}