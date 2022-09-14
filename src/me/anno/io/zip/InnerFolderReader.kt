package me.anno.io.zip

import me.anno.io.files.FileReference

typealias InnerFolderCallback = (InnerFolder?, Exception?) -> Unit
typealias InnerFolderReader = (FileReference, callback: InnerFolderCallback) -> Unit
