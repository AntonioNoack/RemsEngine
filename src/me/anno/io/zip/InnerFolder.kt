package me.anno.io.zip

import me.anno.ecs.prefab.Prefab
import me.anno.ecs.prefab.PrefabReadable
import me.anno.image.Image
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import java.io.IOException
import java.io.InputStream

/**
 * a file, which is inside another file,
 * e.g. inside a zip file, or inside a mesh
 * */
open class InnerFolder(
    absolutePath: String,
    relativePath: String,
    _parent: FileReference
) : InnerFile(absolutePath, relativePath, true, _parent) {

    constructor(root: FileReference) : this(root.absolutePath, "", root.getParent() ?: InvalidRef)

    constructor(parent: InnerFolder, name: String) :
            this(appendPath(parent.absolutePath, name), appendPath(parent.relativePath, name), parent)

    val children = HashMap<String, InnerFile>()
    val childrenList = ArrayList<InnerFile>()

    operator fun contains(fileName: String) = fileName in children

    override fun listChildren(): List<FileReference> = childrenList

    override fun invalidate() {
        super.invalidate()
        for (child in children.values) child.invalidate()
    }

    override fun getInputStream(): InputStream {
        throw IOException("File is directory")
    }

    override fun getChild(name: String): FileReference {
        return if ('\\' in name || '/' in name) {
            getReference(this, name)
        } else {
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
        return children.getOrPut(name) {
            val child = create()
            childrenList.add(child)
            child
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