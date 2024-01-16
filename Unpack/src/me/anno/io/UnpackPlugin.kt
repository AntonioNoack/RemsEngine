package me.anno.io

import me.anno.config.DefaultConfig
import me.anno.ecs.prefab.PrefabCache
import me.anno.extensions.plugins.Plugin
import me.anno.io.files.inner.InnerFolder
import me.anno.io.files.inner.InnerFolderCache
import me.anno.io.files.thumbs.ThumbsExt
import me.anno.io.links.URLReader
import me.anno.io.unity.UnityReader
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

        DefaultConfig.addImportMappings("Asset", *ThumbsExt.unityExtensions.toTypedArray())
        PrefabCache.unityReader = UnityReader::loadUnityFile

        // compressed folders
        InnerFolderCache.register(
            listOf("zip", "bz2", "lz4", "xar", "oar"),
            InnerZipFile.Companion::createZipRegistryV2
        )
        InnerFolderCache.register("7z") { src, callback ->
            val file = Inner7zFile.createZipRegistry7z(src) {
                Inner7zFile.fileFromStream7z(src)
            }
            callback(file, null)
        }
        InnerFolderCache.register("rar") { src, callback ->
            val file = InnerRarFile.createZipRegistryRar(src) {
                InnerRarFile.fileFromStreamRar(src)
            }
            callback(file, null)
        }
        InnerFolderCache.register("gzip", InnerTarFile.Companion::readAsGZip)
        InnerFolderCache.register("tar", InnerTarFile.Companion::readAsGZip)

        // todo register windows lnk
        //  then remove all windows-lnk specific code
        //  then check whether the stuff still works

        // register windows url
        InnerFolderCache.register("url", URLReader::readURLAsFolder)

        // register yaml generally for unity files?
        InnerFolderCache.registerFileExtension(ThumbsExt.unityExtensions) { it, c ->
            val f = UnityReader.readAsFolder(it) as? InnerFolder
            c(f, if (f == null) IOException("$it cannot be read as Unity project") else null)
        }
    }

    override fun onDisable() {
        super.onDisable()
        for (sig in listOf("zip", "bz2", "lz4", "xar", "oar", "7z", "rar", "gzip", "tar", "url")) {
            InnerFolderCache.unregister(sig)
        }
        // unregister more?
        PrefabCache.unityReader = null
    }
}