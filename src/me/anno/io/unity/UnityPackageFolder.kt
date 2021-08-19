package me.anno.io.unity

import me.anno.io.files.FileReference
import me.anno.io.zip.InnerFolder

class UnityPackageFolder(root: FileReference) : InnerFolder(root) {

    val project = UnityProject(this)

    override fun listChildren(): List<FileReference> {
        synchronized(project) {
            // index all files
            for (guid in project.files.keys) {
                project.getGuidFolder(guid)
            }
            return super.listChildren() + project.files.values
        }
    }

    override fun getChild(name: String): FileReference {
        return synchronized(project) {
            super.getChild(name).nullIfUndefined()
                ?: project.getGuidFolder(name)
        }
    }

}
