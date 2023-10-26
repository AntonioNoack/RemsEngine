package me.anno.engine.ui

import me.anno.cache.instances.LastModifiedCache
import me.anno.ecs.prefab.Prefab
import me.anno.ecs.prefab.PrefabCache
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.io.files.Signature
import me.anno.io.files.thumbs.Thumbs
import me.anno.io.text.TextWriter
import me.anno.studio.StudioBase
import me.anno.ui.editor.files.FileExplorer
import me.anno.ui.editor.files.toAllowedFilename
import me.anno.utils.OS
import me.anno.utils.files.Files
import org.apache.logging.log4j.LogManager

object AssetImport {

    private val LOGGER = LogManager.getLogger(AssetImport::class)

    fun interface PrefabCopier {
        fun copy(srcFile: FileReference, dstFolder: FileReference, srcPrefab: Prefab, depth: Int): Prefab
    }

    fun shallowCopyImport(current: FileReference, files: List<FileReference>, fe: FileExplorer?) {
        for (src in files) shallowCopyImport(current, src)
        onCopyFinished(current, fe)
    }

    fun deepCopyImport(current: FileReference, files: List<FileReference>, fe: FileExplorer?) {
        for (src in files) deepCopyImport(current, src)
        onCopyFinished(current, fe)
    }

    fun onCopyFinished(current: FileReference, fe: FileExplorer?) {
        fe?.invalidate()
        LastModifiedCache.invalidate(current)
        // update icon of current folder... hopefully this works
        Thumbs.invalidate(current)
        Thumbs.invalidate(current.getParent())
    }

    fun shallowCopyImport(current: FileReference, src: FileReference) {
        // first create the file mapping, then replace all references
        val mapping = HashMap<FileReference, Prefab>()
        shallowCopyAssets(src, current, mapping)
        replaceShallowReferences(mapping)
    }

    private fun replaceShallowReferences(mapping: HashMap<FileReference, Prefab>) {
        // replace all local references, so we can change the properties of everything:
        for ((_, prefab) in mapping) {
            val original = PrefabCache[prefab.prefab]!!
            // not strictly required
            val replacement0 = mapping[original.prefab]
            if (replacement0 != null) {
                original.prefab = replacement0.source
            }
            // not strictly required
            for (add in original.adds) {
                val replacement = mapping[add.prefab]
                if (replacement != null) {
                    add.prefab = replacement.source
                }
            }
            original.sets.forEach { k1, k2, v ->
                when {
                    // todo maps, pairs, triples and ISaveables need to be investigated as well
                    //  e.g. AnimationState
                    v is FileReference && v != InvalidRef -> {
                        val replacement = mapping[v]
                        if (replacement != null) {
                            prefab[k1, k2] = replacement.source
                        }
                    }
                    v is List<*> && v.any { it is FileReference && it != InvalidRef } -> {
                        prefab[k1, k2] = v.map { mapping[it]?.source ?: it }
                    }
                }
            }
            prefab.source.writeText(TextWriter.toText(prefab, StudioBase.workspace))
        }
    }

    fun deepCopyImport(current: FileReference, src: FileReference) {
        // first create the file mapping, then replace all references
        val mapping = HashMap<FileReference, Prefab>()
        deepCopyAssets(src, current, mapping)
    }

    fun isPureFile(file: FileReference): Boolean {
        return when (if (file.isDirectory) null else Signature.findNameSync(file)) {
            // todo add all pure-copy extensions
            "jpg", "png", "bmp", "svg", "gif", "qoi", "media", "hdr", "exr", "dds", "webp", "txt",
            "woff", "woff2", "pdf", "gimp" -> true
            // maybe list the negatives?
            else -> false
        }
    }

    private fun generalCopyAssets(
        srcFolder: FileReference, dstFolder: FileReference, isMainFolder: Boolean,
        prefabMapping: HashMap<FileReference, Prefab>, depth: Int,
        copier: PrefabCopier
    ) {
        // if asset is pure (png,jpg,...), just copy or link it
        if (isPureFile(srcFolder)) {
            val dstFile = if (dstFolder.isDirectory) {
                dstFolder.getChild(srcFolder.name)
            } else dstFolder
            dstFile.writeFile(srcFolder) {}
        } else {
            // todo what if this is directly an asset? mesh, material, ...
            for (srcFile in srcFolder.listChildren() ?: return) {
                if (srcFile.isDirectory) {
                    val dstFolder2 = dstFolder.getChild(srcFile.name)
                    if (!dstFolder2.exists) dstFolder2.tryMkdirs()
                    if (dstFolder2.isDirectory) {
                        generalCopyAssets(srcFile, dstFolder2, false, prefabMapping, depth + 1, copier)
                    } else LOGGER.warn("Could not create directory $dstFolder2 for $srcFile/${srcFile.name}")
                } else {

                    // read as inner folder
                    // rename its inner "Scene.json" into src.nameWithoutExtension + .json
                    // import all other inner files into their respective files
                    // if content is identical, we could merge them

                    // it may not be necessarily a prefab, it could be a saveable
                    val prefab = loadPrefab(srcFile)
                    if (prefab != null) {
                        val name = findName(srcFile, prefab, isMainFolder)
                        val newPrefab = copier.copy(srcFile, dstFolder, prefab, depth)
                        val dstFile = savePrefab(dstFolder, name, newPrefab)
                        newPrefab.source = dstFile
                        prefabMapping[srcFile] = newPrefab
                    } else LOGGER.warn("Skipped $srcFile, because it was not a prefab")
                }
            }
        }
    }

    private fun copyPrefab(
        srcFile: FileReference, dstFolder: FileReference, isMainFolder: Boolean,
        cache: HashMap<FileReference, Prefab>, depth: Int, copier: PrefabCopier
    ): FileReference {
        if (srcFile == InvalidRef) return InvalidRef
        val cached = cache[srcFile]
        if (cached != null) return cached.source
        val prefab = loadPrefab(srcFile)
        return if (prefab != null) {
            val name = findName(srcFile, prefab, isMainFolder)
            val newPrefab = copier.copy(srcFile, dstFolder, prefab, depth + 1)
            val dstFile = savePrefab(newPrefab.source.ifUndefined(dstFolder), name, newPrefab)
            newPrefab.source = dstFile
            cache[srcFile] = newPrefab
            dstFile
        } else {
            LOGGER.warn("Skipped $srcFile, because it was not a prefab")
            InvalidRef
        }
    }

    private fun shallowCopyAssets(
        srcFolder: FileReference,
        dstFolder: FileReference,
        prefabMapping: HashMap<FileReference, Prefab>
    ) {
        generalCopyAssets(srcFolder, dstFolder, true, prefabMapping, 0) { srcFile, _, prefab, _ ->
            Prefab(prefab.clazzName, srcFile)
        }
    }

    private fun deepCopyAssets(
        srcFolder: FileReference,
        dstFolder0: FileReference,
        prefabMapping: HashMap<FileReference, Prefab>
    ) {
        // todo deep copy is unable to copy reference loops...
        val copier = object : PrefabCopier {
            override fun copy(srcFile: FileReference, dstFolder: FileReference, srcPrefab: Prefab, depth: Int): Prefab {
                // find all referenced files, and copy them, too
                val dstPrefab = Prefab(srcPrefab.clazzName)
                var dstFolder1 = dstFolder
                var isMainFolder = dstFolder0 === dstFolder
                when (srcPrefab.clazzName) {
                    "Mesh" -> dstFolder1 = dstFolder0.getChild("meshes")
                    "Material" -> dstFolder1 = dstFolder0.getChild("materials")
                    "Animation" -> dstFolder1 = dstFolder0.getChild("animations")
                    "Skeleton" -> dstFolder1 = dstFolder0.getChild("skeletons")
                }
                if (dstFolder1 !== dstFolder) {
                    dstFolder1.tryMkdirs()
                    isMainFolder = false
                }
                dstPrefab.source = dstFolder1
                if (depth < 10) {
                    dstPrefab.prefab = copyPrefab(
                        srcPrefab.prefab, dstFolder1, isMainFolder,
                        prefabMapping, depth, this
                    )
                    for (add in srcPrefab.adds) {
                        val clone = add.clone()
                        clone.prefab = copyPrefab(
                            add.prefab, dstFolder0, isMainFolder,
                            prefabMapping, depth, this
                        )
                        dstPrefab.add(clone, -1)
                    }
                    srcPrefab.sets.forEach { k1, k2, value0 ->
                        var value = value0
                        // todo we need to also inspect AnimationState, and structures like it
                        if (value is FileReference) {
                            value = copyPrefab(
                                value, dstFolder0, isMainFolder,
                                prefabMapping, depth, this
                            )
                        } else if (value is List<*> && value.any { it is FileReference }) {
                            value = value.map {
                                if (it is FileReference) copyPrefab(
                                    it, dstFolder0, isMainFolder,
                                    prefabMapping, depth, this
                                )
                                else it
                            }
                        }
                        dstPrefab.sets[k1, k2] = value
                    }
                } else LOGGER.warn("Cannot resolve deep-links in $srcFile because of recursion")
                return dstPrefab
            }
        }
        generalCopyAssets(srcFolder, dstFolder0, true, prefabMapping, 0, copier)
    }

    private fun loadPrefab(srcFile: FileReference): Prefab? {
        return try {
            PrefabCache[srcFile]
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun findName(srcFile: FileReference, prefab: Prefab, isMainFolder: Boolean): String {
        val prefabName = prefab.instanceName?.toAllowedFilename()
        val fileName = srcFile.nameWithoutExtension.toAllowedFilename()
        var name = fileName ?: srcFile.getParent()?.nameWithoutExtension ?: prefab.instanceName ?: "Scene"
        if (name.toIntOrNull() != null) {
            name = prefabName ?: "Scene"
        }
        if (isMainFolder && name == "Scene") {
            // rename to file name
            name = srcFile.getParent()!!.nameWithoutExtension
        }
        return name
    }

    private val copyCheckedTypes = listOf("Mesh", "Material", "Animation", "Skeleton")

    private fun savePrefab(dstFolder: FileReference, name: String, newPrefab: Prefab): FileReference {
        var dstFile = dstFolder.getChild("$name.json")
        val data = TextWriter.toText(newPrefab, StudioBase.workspace)
        if (dstFile.exists && newPrefab.clazzName in copyCheckedTypes) {
            // compare the contents: if identical, we can use it
            val data0 = try {
                dstFile.readTextSync()
            } catch (e: Exception) {
                null
            }
            // todo this comparison can only be done when everything has been renamed... [ShallowCopy]
            // todo we also have the issue, that we should compare with all siblings
            if (data0 != data) {
                println("Files differ [$name]: ${data0?.hashCode()}[${data0?.length}] vs ${data.hashCode()}[${data.length}]")
                dstFile = Files.findNextFile(dstFolder, name, "json", 3, '-', 1)
            }
        }
        dstFile.getParent()?.tryMkdirs()
        dstFile.writeText(data)
        return dstFile
    }

    fun createLink(original: FileReference, linkLocation: FileReference) {
        val data = if (OS.isWindows) {
            // create .url file
            // are they supported for static files, too???
            if (original.absolutePath.contains("://")) {
                "[InternetShortcut]\n" +
                        "URL=$original\n"
            } else {
                "[InternetShortcut]\n" +
                        "URL=file://$original\n"
            }
        } else {
            // create .desktop file
            // sample data by https://help.ubuntu.com/community/UnityLaunchersAndDesktopFiles:
            "[Desktop Entry]\n" +
                    "Version=1.0\n" +
                    "Name=${original.name}\n" +
                    "Exec=${original.absolutePath}\n" +
                    "Icon=${original.absolutePath}\n" +
                    "Type=Link\n"
        }
        linkLocation.writeText(data)
    }
}