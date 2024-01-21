package me.anno

import me.anno.extensions.plugins.Plugin
import me.anno.input.Clipboard
import me.anno.io.MediaMetadata
import me.anno.io.files.thumbs.Thumbs
import me.anno.utils.files.OpenFileExternally

// todo move all Java-exclusive things here as far as possible...
//  (things that will be unavailable on Android/Web/KotlinNative)

// todo targets: java.awt, BufferedImage

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
        ImageImpl.register()
    }
}