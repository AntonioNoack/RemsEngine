package me.anno.io.files.inner

import me.anno.ecs.prefab.Prefab
import me.anno.ecs.prefab.PrefabReadable
import me.anno.image.Image
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.io.files.Reference.appendPath
import me.anno.io.files.inner.lazy.InnerLazyByteFile
import me.anno.io.files.inner.lazy.InnerLazyImageFile
import me.anno.io.files.inner.lazy.InnerLazyPrefabFile
import me.anno.utils.algorithms.Recursion
import me.anno.utils.structures.lists.UnsafeArrayList
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer

/**
 * a file, which is inside another file,
 * e.g., inside a zip file, or inside a mesh
 * */
open class InnerFolder(
    absolutePath: String,
    relativePath: String,
    parent: FileReference
) : InnerFile(absolutePath, relativePath, true, parent) {

    constructor(root: FileReference) : this(root.absolutePath, "", root.getParent()) {
        alias = root
    }

    constructor(parent: InnerFolder, name: String) :
            this(appendPath(parent.absolutePath, name), appendPath(parent.relativePath, name), parent)

    var lookup: Map<String, InnerFile>? = null
    val children = HashMap<String, InnerFile>()
    val childrenList = UnsafeArrayList<InnerFile>()
    var alias: FileReference? = null
        private set

    operator fun contains(fileName: String) = fileName in children
    override fun listChildren(): List<FileReference> = childrenList

    override fun invalidate() {
        super.invalidate()
        for (child in children.values) {
            child.invalidate()
        }
    }

    override fun inputStreamSync(): InputStream {
        return alias?.inputStreamSync() ?: warnIsDirectory()
    }

    override fun readBytesSync(): ByteArray {
        return alias?.readBytesSync() ?: warnIsDirectory()
    }

    override fun readByteBufferSync(native: Boolean): ByteBuffer {
        return alias?.readByteBufferSync(native) ?: warnIsDirectory()
    }

    private fun warnIsDirectory(): Nothing {
        throw IOException("'$this' is directory") // could be thrown as well
    }

    override fun getChildImpl(name: String): FileReference {
        return synchronized(children) {
            val c0 = children.values.filter { it.name.equals(name, true) }
            c0.firstOrNull { it.name == name } ?: c0.firstOrNull() ?: InvalidRef
        }
    }

    override fun getLc(path: String): FileReference? {
        val index = path.indexOf('/')
        return if (index < 0) {
            children[path]
        } else {
            val parent = path.substring(0, index)
            val name = path.substring(index + 1)
            children[parent]?.getLc(name)
        }
    }

    fun getSubName(name: String): String {
        return if (relativePath.isEmpty()) name
        else "$relativePath/$name"
    }

    fun getOrPut(name: String, create: () -> InnerFile): InnerFile {
        return synchronized(children) {
            children.getOrPut(name) {
                val child = create()
                childrenList.add(child)
                child
            }
        }
    }

    fun createChild(name: String, relativePath: String = getSubName(name)): InnerFolder {
        val child = children[name]
        if (child != null) return child as InnerFolder
        val absolutePath = appendPath(absolutePath, name)
        return createChild2(absolutePath, relativePath)
    }

    private fun createChild2(absolutePath: String, relativePath: String): InnerFolder {
        val child = children[name]
        if (child != null) return child as InnerFolder
        return InnerFolder(absolutePath, relativePath, this)
    }

    fun createChild(
        name: String,
        registry: HashMap<String, InnerFile>? = null
    ): InnerFile = createAnyChild(name, { abs, rel -> createChild2(abs, rel) }, registry)

    fun createTextChild(
        name: String, content: String,
        registry: HashMap<String, InnerFile>? = null
    ): InnerFile = createAnyChild(name, { abs, rel -> InnerTextFile(abs, rel, this, content) }, registry)

    fun createPrefabChild(
        name: String, content: Prefab,
        registry: HashMap<String, InnerFile>? = null
    ): InnerFile = createAnyChild(name, { abs, rel -> InnerPrefabFile(abs, rel, this, content) }, registry)

    fun createLazyPrefabChild(
        name: String, content: Lazy<Prefab>,
        registry: HashMap<String, InnerFile>? = null
    ): InnerFile = createAnyChild(name, { abs, rel -> InnerLazyPrefabFile(abs, rel, this, content) }, registry)

    fun createLazyImageChild(
        name: String, content: Lazy<Image>,
        registry: HashMap<String, InnerFile>? = null
    ): InnerFile = createLazyImageChild(name, content, { content.value }, registry)

    fun createLazyImageChild(
        name: String, cpuImage: Lazy<Image>, gpuImage: () -> Image,
        registry: HashMap<String, InnerFile>? = null
    ) = createAnyChild(name, { abs, rel -> InnerLazyImageFile(abs, rel, this, cpuImage, gpuImage) }, registry)

    fun createByteChild(
        name: String, content: ByteArray,
        registry: HashMap<String, InnerFile>? = null
    ): InnerFile = createAnyChild(name, { abs, rel -> InnerByteFile(abs, rel, this, content) }, registry)

    @Suppress("unused")
    fun createByteChild(
        name: String, content: Lazy<ByteArray>,
        registry: HashMap<String, InnerFile>? = null
    ): InnerFile = createAnyChild(name, { abs, rel -> InnerLazyByteFile(abs, rel, this, content) }, registry)

    fun createImageChild(
        name: String, content: Image,
        registry: HashMap<String, InnerFile>? = null
    ): InnerFile = createAnyChild(name, { abs, rel -> InnerImageFile(abs, rel, this, content) }, registry)

    @Suppress("unused")
    fun createStreamChild(name: String, content: () -> InputStream, registry: HashMap<String, InnerFile>? = null)
            : InnerFile = createAnyChild(name, { abs, rel -> InnerStreamFile(abs, rel, this, content) }, registry)

    private inline fun createAnyChild(
        name: String, createFile: (String, String) -> InnerFile,
        registry: HashMap<String, InnerFile>? = null
    ): InnerFile {
        val child = children[name]
        if (child != null) return child
        val relativePath = getSubName(name)
        val childFromRegistry = registry?.get(relativePath)
        if (childFromRegistry != null) return childFromRegistry
        val newChild = createFile(appendPath(absolutePath, name), relativePath)
        registry?.put(relativePath, newChild)
        return newChild
    }

    fun sealPrefabs() {
        Recursion.processRecursive<FileReference>(this) { file, remaining ->
            when (file) {
                is InnerFolder -> remaining.addAll(file.listChildren())
                is PrefabReadable -> file.readPrefab().sealFromModifications()
            }
        }
    }
}