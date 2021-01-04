package me.anno.io.text

import me.anno.io.find.PartialWriter

class FindReferencesWriter: PartialWriter(true) {

    val usedPointers = HashSet<Int>()

    override fun writePointer(name: String?, className: String, ptr: Int) {
        usedPointers += ptr
    }

}