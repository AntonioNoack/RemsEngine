package me.anno.io.files.inner

import me.anno.io.files.FileReference
import me.anno.utils.async.Callback
import me.anno.utils.async.Callback.Companion.USE_COROUTINES_INSTEAD

@Deprecated(USE_COROUTINES_INSTEAD)
typealias InnerFolderCallback = Callback<InnerFolder>

@Deprecated(USE_COROUTINES_INSTEAD)
typealias InnerFolderReader = (FileReference, callback: InnerFolderCallback) -> Unit

typealias InnerFolderReaderX = suspend (FileReference) -> Result<InnerFolder>
