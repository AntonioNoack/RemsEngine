package me.anno

import me.anno.extensions.plugins.Plugin
import me.anno.fonts.*
import me.anno.fonts.signeddistfields.Contour
import me.anno.gpu.framebuffer.Screenshots
import me.anno.images.ImageImpl
import me.anno.images.MetadataImpl
import me.anno.images.ThumbsImpl
import me.anno.input.Clipboard
import me.anno.io.MediaMetadata
import me.anno.io.files.thumbs.Thumbs
import me.anno.utils.files.OpenFileExternally
import java.awt.font.FontRenderContext
import java.awt.font.TextLayout

// todo move all Java-exclusive things here as far as possible...
//  (things that will be unavailable on Android/Web/KotlinNative)

class JVMPlugin : Plugin() {
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
    }
}