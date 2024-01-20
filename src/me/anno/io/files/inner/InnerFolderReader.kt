package me.anno.io.files.inner

import me.anno.utils.structures.Callback
import me.anno.io.files.FileReference

typealias InnerFolderCallback = Callback<InnerFolder>
typealias InnerFolderReader = (FileReference, callback: InnerFolderCallback) -> Unit
