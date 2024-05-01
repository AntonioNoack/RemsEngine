package me.anno.io

import me.anno.config.DefaultConfig
import me.anno.ecs.prefab.PrefabCache
import me.anno.extensions.plugins.Plugin
import me.anno.image.thumbs.AssetThumbnails
import me.anno.io.files.Reference.getReference
import me.anno.io.files.inner.InnerFolder
import me.anno.io.files.inner.InnerFolderCache
import me.anno.image.thumbs.Thumbs
import me.anno.image.thumbs.AssetThumbHelper
import me.anno.io.links.LNKReader
import me.anno.io.links.URLReader
import me.anno.io.links.WindowsShortcut
import me.anno.io.unity.UnityReader
import me.anno.io.zip.ExeSkipper
import me.anno.io.zip.Inner7zFile
import me.anno.io.zip.InnerRarFile
import me.anno.io.zip.InnerTarFile
import me.anno.io.zip.InnerZipFile
import java.io.IOException

/**
 * registers all available readers into InnerFolderCache
 * */
class UnpackPlugin : Plugin() {

    override fun onEnable() {
        super.onEnable()

        DefaultConfig.addImportMappings("Asset", AssetThumbHelper.unityExtensions)
        PrefabCache.unityReader = UnityReader::loadUnityFile

        // compressed folders
        InnerFolderCache.registerSignatures("zip,bz2,lz4,xar,oar", InnerZipFile.Companion::createZipRegistryV2)
        InnerFolderCache.registerSignatures("exe", ExeSkipper::readAsFolder)
        InnerFolderCache.registerSignatures("7z") { src, callback ->
            val file = Inner7zFile.createZipRegistry7z(src) {
                Inner7zFile.fileFromStream7z(src)
            }
            callback.ok(file)
        }
        InnerFolderCache.registerSignatures("rar") { src, callback ->
            val file = InnerRarFile.createZipRegistryRar(src) {
                InnerRarFile.fileFromStreamRar(src)
            }
            callback.ok(file)
        }
        InnerFolderCache.registerSignatures("gzip", InnerTarFile.Companion::readAsGZip)
        InnerFolderCache.registerSignatures("tar", InnerTarFile.Companion::readAsGZip)

        // Windows and Linux links
        InnerFolderCache.registerSignatures("lnk", LNKReader::readLNKAsFolder)
        InnerFolderCache.registerSignatures("url", URLReader::readURLAsFolder)

        Thumbs.registerFileExtensions("lnk") { srcFile, dstFile, size, callback ->
            WindowsShortcut.get(srcFile) { link, exc ->
                if (link != null) {
                    val iconFile = link.iconPath ?: link.absolutePath
                    Thumbs.generate(getReference(iconFile), dstFile, size, callback)
                } else callback.err(exc)
            }
        }

        // try as an asset
        Thumbs.registerFileExtensions(AssetThumbHelper.unityExtensions, AssetThumbnails::generateAssetFrame)

        // register yaml generally for unity files?
        InnerFolderCache.registerFileExtensions(AssetThumbHelper.unityExtensions) { it, c ->
            val f = UnityReader.readAsFolder(it) as? InnerFolder
            c.call(f, if (f == null) IOException("$it cannot be read as Unity project") else null)
        }
    }

    override fun onDisable() {
        super.onDisable()
        InnerFolderCache.unregisterSignatures("zip,bz2,lz4,xar,oar,exe,7z,rar,gzip,tar,lnk,url")
        InnerFolderCache.unregisterFileExtensions(AssetThumbHelper.unityExtensions)
        // unregister more?
        PrefabCache.unityReader = null
    }
}