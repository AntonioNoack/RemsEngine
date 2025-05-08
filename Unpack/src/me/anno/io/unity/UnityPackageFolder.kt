package me.anno.io.unity

import me.anno.io.files.FileReference
import me.anno.io.files.inner.InnerFolder
import me.anno.utils.async.Callback
import me.anno.utils.async.Callback.Companion.map

class UnityPackageFolder(root: FileReference) : InnerFolder(root) {

    val project = UnityProject(this)

    init {
        // index all files
        for (guid in project.files.keys) {
            project.getGuidFolder(guid)
        }
    }

    override fun listChildren(callback: Callback<List<FileReference>>) {
        super.listChildren(callback.map { list ->
            list + project.files.values
        })
    }

    override fun getChildImpl(name: String): FileReference {
        return super.getChildImpl(name).nullIfUndefined()
            ?: project.getGuidFolder(name)
    }
}
