package me.anno.io.files.inner

import me.anno.utils.async.Callback
import me.anno.io.files.FileReference

typealias InnerFolderCallback = Callback<InnerFolder>
typealias InnerFolderReader = (FileReference, callback: InnerFolderCallback) -> Unit
