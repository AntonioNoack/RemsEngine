package me.anno.export

import me.anno.ecs.annotations.Docs
import me.anno.io.NamedSaveable
import me.anno.io.base.BaseWriter
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.io.json.saveable.JsonStringReader
import me.anno.io.json.saveable.JsonStringWriter

class ExportSettings : NamedSaveable() {

    // todo get the minimum viable export running :3

    // todo:
    //  - which platforms to support (LWJGL libraries) -> Linux, Windows, MacOS, x86/arm
    //  - exclude classes from ECSRegistry via
    //      a) reflections (as their implementation -> fail save),
    //      b) renaming them inside the .class file?,
    //      c) rewriting the file using the Kotlin compiler
    //      d) replacing their .class files with stubs? (still registered, but without any content) <- might be the cleanest solution

    // todo TreeView for FilesToInclude/Exclude: check boxes on every level, and then state gets saved

    var gameTitle = ""
    var configName = ""
    var versionNumber = 1

    val modulesToInclude = HashSet<String>()

    var firstSceneRef: FileReference = InvalidRef

    @Docs("Collection of files/folders; external stuff is always exported, when referenced by an internal asset")
    val assetsToInclude = HashSet<FileReference>()

    // idk...
    val excludedClasses = HashSet<String>()

    var dstFile: FileReference = InvalidRef
    var iconOverride: FileReference = InvalidRef

    var lastUsed = 0L

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeStringArray("modulesToInclude", modulesToInclude.toTypedArray())
        writer.writeFileArray("assetsToInclude", assetsToInclude.toTypedArray())
        writer.writeStringArray("excludedClasses", excludedClasses.toTypedArray())
        saveSerializableProperties(writer)
    }

    override fun setProperty(name: String, value: Any?) {
        if (readSerializableProperty(name, value)) return
        when (name) {
            "modulesToInclude" -> loadStringArray(modulesToInclude, value)
            "assetsToInclude" -> loadFileArray(assetsToInclude, value)
            "excludedClasses" -> loadStringArray(excludedClasses, value)
            else -> super.setProperty(name, value)
        }
    }

    private fun loadFileArray(dst: MutableCollection<FileReference>, value: Any?) {
        if (value is Array<*>) {
            dst.clear()
            dst.addAll(value.filterIsInstance<FileReference>())
        }
    }

    private fun loadStringArray(dst: MutableCollection<String>, value: Any?) {
        if (value is Array<*>) {
            dst.clear()
            dst.addAll(value.filterIsInstance<String>())
        }
    }

    fun clone(): ExportSettings {
        return JsonStringReader.readFirst(JsonStringWriter.toText(this, InvalidRef), InvalidRef)
    }
}