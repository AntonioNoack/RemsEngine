package me.anno.io.files.inner

import me.anno.io.files.FileReference

typealias InnerFolderCallback = (InnerFolder?, Exception?) -> Unit
typealias InnerFolderReader = (FileReference, callback: InnerFolderCallback) -> Unit
