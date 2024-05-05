package me.anno.io.unity

import me.anno.io.files.FileReference
import me.anno.io.files.inner.InnerFolder

class UnityPackageFolder(root: FileReference) : InnerFolder(root) {

    val project = UnityProject(this)

    init {
        // index all files
        for (guid in project.files.keys) {
            project.getGuidFolder(guid)
        }
    }

    override fun listChildren(): List<FileReference> {
        return super.listChildren() + project.files.values
    }

    override fun getChild(name: String): FileReference {
        return super.getChild(name).nullIfUndefined()
                ?: project.getGuidFolder(name)
    }

}
