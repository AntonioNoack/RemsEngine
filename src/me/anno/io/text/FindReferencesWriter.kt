package me.anno.io.text

import me.anno.io.ISaveable
import me.anno.io.find.PartialWriter

class FindReferencesWriter(canSkipDefaultValues: Boolean) : PartialWriter(canSkipDefaultValues) {

    val usedPointers = HashSet<ISaveable>()

    override fun writePointer(name: String?, className: String, ptr: Int, value: ISaveable) {
        usedPointers += value
    }

}