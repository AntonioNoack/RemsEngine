package me.anno.io.files.inner

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import me.anno.cache.CacheData
import me.anno.cache.CacheSection
import me.anno.extensions.FileReaderRegistry
import me.anno.extensions.FileReaderRegistryImpl
import me.anno.gpu.GFX
import me.anno.image.ImageAsFolder
import me.anno.io.files.FileReference
import me.anno.io.files.FileWatch
import me.anno.io.files.Signature
import me.anno.io.files.SignatureCache
import me.anno.mesh.vox.VOXReader
import me.anno.utils.async.Callback
import me.anno.utils.async.Callback.Companion.USE_COROUTINES_INSTEAD
import me.anno.utils.async.suspendToCallback
import me.anno.utils.async.suspendToValue
import me.anno.utils.async.unpack
import java.io.IOException
import kotlin.math.max

object InnerFolderCache : CacheSection("InnerFolderCache"),
    FileReaderRegistry<InnerFolderReaderX> by FileReaderRegistryImpl() {

    val imageFormats = "png,jpg,bmp,pds,hdr,webp,tga,ico,dds,gif,exr,qoi"
    val imageFormats1 = imageFormats.split(',')

    init {
        // meshes
        registerSignatures("vox", VOXReader::readAsFolder)
        // images
        registerSignatures(imageFormats, ImageAsFolder::readAsFolder)
        registerSignatures("media", ImageAsFolder::readAsFolder) // correct for webp, not for videos
    }

    fun wasReadAsFolder(file: FileReference): InnerFolder? {
        val data = getEntryWithoutGenerator(file) as? CacheData<*>
        return data?.value as? InnerFolder
    }

    @Deprecated(USE_COROUTINES_INSTEAD)
    fun readAsFolder(file: FileReference, async: Boolean): InnerFile? {
        return readAsFolder(file, timeoutMillis, async)
    }

    @Deprecated(USE_COROUTINES_INSTEAD)
    fun readAsFolder(file: FileReference, callback: Callback<InnerFolder?>) {
        if (file is InnerFile && file.folder is InnerFolder) return callback.ok(file.folder as InnerFolder)
        return suspendToCallback({
            getFileEntryX(file, false, timeoutMillis) { file1, _ ->
                generate(file1)
            }.await()
        }, callback)
    }

    @Deprecated(USE_COROUTINES_INSTEAD)
    fun readAsFolder(file: FileReference, timeoutMillis: Long, async: Boolean): InnerFile? {
        if (file is InnerFile && file.folder != null) return file.folder
        return suspendToValue(async) {
            getFileEntryX(file, false, timeoutMillis) { file1, _ ->
                generate(file1)
            }.await()
        }
    }

    fun readAsFolderX(file: FileReference, timeoutMillis: Long = InnerFolderCache.timeoutMillis): Deferred<Result<InnerFile>> {
        if (file is InnerFile && file.folder != null) {
            return CompletableDeferred(Result.success(file.folder!!))
        }
        return getFileEntryX(file, false, timeoutMillis) { file1, _ ->
            generate(file1)
        }
    }

    private suspend fun generate(file1: FileReference): Result<InnerFolder> {
        if (GFX.glThread != null) {
            // todo can we get this working without introducing a dead-lock for tests?
            val signature = SignatureCache.getX(file1).await()
            return generate1(file1, signature.getOrNull())
        } else {
            val signature = SignatureCache[file1, false]
            return generate1(file1, signature)
        }
    }

    private suspend fun generate1(file1: FileReference, signature: Signature?): Result<InnerFolder> {
        val ext = file1.lcExtension
        if (signature?.name == "json" && ext == "json") {
            return Result.failure(IOException("Unsupported type: JSON"))
        } else {
            val readers = getReaders(signature, ext)
            return generate(file1, readers, 0)
        }
    }

    private suspend fun generate(
        file1: FileReference,
        generators: List<InnerFolderReaderX>, gi: Int
    ): Result<InnerFolder> {
        for (i in gi until generators.size) {
            val reader = generators[gi]
            val result = reader(file1)
            val (folder, err) = result.unpack()
            err?.printStackTrace()
            if (folder != null) {
                if (file1 is InnerFile) {
                    file1.folder = folder
                }
                // todo remove watch dog when unloading it?
                FileWatch.addWatchDog(file1)
                return result
            }
        }
        return Result.failure(IOException("No reader succeeded for readAsFolder('$file1')"))
    }

    fun splitParent(name: String): Pair<String, String> {
        var path = name.replace('\\', '/')
        while (path.endsWith('/')) path = path.substring(0, path.length - 1)
        val nameIndex = path.indexOfLast { it == '/' }
        val parent = path.substring(0, max(nameIndex, 0))
        return parent to path
    }

    var timeoutMillis = 60_000L

    /**
     * opening a packed stream again would be really expensive for large packages;
     * is there a better strategy than this? -> HeavyIterator reduces the number of required passes
     * */
    var sizeLimit = 500_000L
}