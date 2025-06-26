package me.anno.engine.ui

import me.anno.io.files.FileReference
import me.anno.io.files.ImportType.MESH
import me.anno.io.files.ImportType.METADATA
import me.anno.io.files.SignatureCache
import me.anno.io.files.inner.InnerFolder

object AssetImportType {

    private fun getImportType(file: FileReference): String? {
        var file = file
        while (file is InnerFolder) {
            file = file.alias ?: break
        }
        return SignatureCache[file].waitFor()?.importType
    }

    fun getPureTypeOrNull(file: FileReference): String? {
        val importType = getImportType(file) ?: return null
        return if (importType == MESH || importType == METADATA) null
        else importType
    }

}