package me.anno.io.zip

import me.anno.ecs.prefab.Prefab
import me.anno.ecs.prefab.PrefabReadable
import me.anno.image.Image
import me.anno.image.raw.GPUImage
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.utils.structures.lists.UnsafeArrayList
import java.io.IOException
import java.io.InputStream

/**
 * a file, which is inside another file,
 * e.g., inside a zip file, or inside a mesh
 * */
open class InnerFolder(
    absolutePath: String,
    relativePath: String,
    parent: FileReference
) : InnerFile(absolutePath, relativePath, true, parent) {

    constructor(root: FileReference) : this(root.absolutePath, "", root.getParent() ?: InvalidRef)

    constructor(parent: InnerFolder, name: String) :
            this(appendPath(parent.absolutePath, name), appendPath(parent.relativePath, name), parent)

    var lookup: Map<String, InnerFile>? = null
    val children = HashMap<String, InnerFile>()
    val childrenList = UnsafeArrayList<InnerFile>()

    operator fun contains(fileName: String) = fileName in children
    override fun listChildren(): List<FileReference> = childrenList

    override fun invalidate() {
        super.invalidate()
        for (child in children.values) child.invalidate()
    }

    override fun getInputStream(callback: (InputStream?, Exception?) -> Unit) {
        callback(null, IOException("File is directory")) // could be thrown as well
    }

    override fun getChild(name: String): FileReference {
        return if ('\\' in name || '/' in name) {
            getReference(this, name)
        } else {
            synchronized(children) {
                val c0 = children.values.filter { it.name.equals(name, true) }
                c0.firstOrNull { it.name == name } ?: c0.firstOrNull() ?: InvalidRef
            }
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
        val absolutePath = "$absolutePath/$name"
        return InnerFolder(absolutePath, relativePath, this)
    }

    fun createChild(name: String, registry: HashMap<String, InnerFile>? = null): InnerFile {
        val child = children[name]
        if (child != null) return child
        val relativePath = "$relativePath/$name"
        return registry?.getOrPut(relativePath) {
            createChild(name, relativePath)
        } ?: createChild(name, relativePath)
    }

    fun createTextChild(name: String, content: String, registry: HashMap<String, InnerFile>? = null): InnerFile {
        val child = children[name]
        if (child != null) return child
        val relativePath = getSubName(name)
        return registry?.getOrPut(relativePath) { createTextChild(name, content, null) }
            ?: InnerTextFile("$absolutePath/$name", relativePath, this, content)
    }

    fun createPrefabChild(name: String, content: Prefab, registry: HashMap<String, InnerFile>? = null): InnerFile {
        val child = children[name]
        if (child != null) return child
        val relativePath = getSubName(name)
        return registry?.getOrPut(relativePath) { createPrefabChild(name, content, null) }
            ?: InnerPrefabFile("$absolutePath/$name", relativePath, this, content)
    }

    fun createLazyPrefabChild(name: String, content: Lazy<Prefab>, registry: HashMap<String, InnerFile>? = null): InnerFile {
        val child = children[name]
        if (child != null) return child
        val relativePath = getSubName(name)
        return registry?.getOrPut(relativePath) { createLazyPrefabChild(name, content, null) }
            ?: InnerLazyPrefabFile("$absolutePath/$name", relativePath, this, content)
    }

    fun createLazyImageChild(name: String, content: Lazy<Image>, registry: HashMap<String, InnerFile>? = null): InnerFile {
        return createLazyImageChild(name, content, content, registry)
    }

    fun createLazyImageChild(name: String, cpuImage: Lazy<Image>, gpuImage: Lazy<Image>, registry: HashMap<String, InnerFile>? = null): InnerFile {
        val child = children[name]
        if (child != null) return child
        val relativePath = getSubName(name)
        return registry?.getOrPut(relativePath) { createLazyImageChild(name, cpuImage, gpuImage, null) }
            ?: InnerLazyImageFile("$absolutePath/$name", relativePath, this, cpuImage, gpuImage)
    }

    fun createByteChild(name: String, content: ByteArray, registry: HashMap<String, InnerFile>? = null): InnerFile {
        val child = children[name]
        if (child != null) return child
        val relativePath = getSubName(name)
        return registry?.getOrPut(relativePath) { createByteChild(name, content, null) }
            ?: InnerByteFile("$absolutePath/$name", relativePath, this, content)
    }

    @Suppress("unused")
    fun createByteChild(
        name: String,
        content: Lazy<ByteArray>,
        registry: HashMap<String, InnerFile>? = null
    ): InnerFile {
        val child = children[name]
        if (child != null) return child
        val relativePath = getSubName(name)
        return registry?.getOrPut(relativePath) { createByteChild(name, content, null) }
            ?: InnerLazyByteFile("$absolutePath/$name", relativePath, this, content)
    }

    fun createImageChild(
        name: String,
        content: Image,
        registry: HashMap<String, InnerFile>? = null
    ): InnerFile {
        val child = children[name]
        if (child != null) return child
        val relativePath = getSubName(name)
        return registry?.getOrPut(relativePath) { createImageChild(name, content, null) }
            ?: InnerImageFile("$absolutePath/$name", relativePath, this, content)
    }

    fun createStreamChild(
        name: String,
        content: () -> InputStream,
        registry: HashMap<String, InnerFile>? = null
    ): InnerFile {
        val child = children[name]
        if (child != null) return child
        val relativePath = getSubName(name)
        return registry?.getOrPut(relativePath) { createStreamChild(name, content, null) }
            ?: InnerStreamFile("$absolutePath/$name", relativePath, this, content)
    }

    fun sealPrefabs() {
        for (child in listChildren()) {
            if (child is PrefabReadable) {
                child.readPrefab().sealFromModifications()
            }
            if (child is InnerFolder) {
                child.sealPrefabs()
            }
        }
    }

}