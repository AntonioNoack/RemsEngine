package me.anno.io.json

import me.anno.io.xml.ComparableStringBuilder
import java.io.InputStream
import java.util.ArrayList

open class JsonScanner : JsonReader {
    constructor(data: String) : super(data)
    constructor(data: InputStream) : super(data)
    constructor(data: ByteArray) : super(data)

    val types = ArrayList<ComparableStringBuilder>()
    var typeI = 0

    init {
        types.add(type0)
    }

    fun scanObject(readProperty: (CharSequence) -> Unit) {
        assertEquals(skipSpace(), '{')
        var next = skipSpace()
        while (true) {
            when (next) {
                '}' -> return
                '"' -> {
                    if (types.size <= typeI) types.add(ComparableStringBuilder())
                    val name = readString(types[typeI++])
                    assertEquals(skipSpace(), ':')
                    readProperty(name)
                    typeI-- // done reading :)
                    next = skipSpace()
                }
                ',' -> next = skipSpace()
                else -> assert(next, '}', '"')
            }
        }
    }

    fun scanArray(readElement: () -> Unit) {
        assertEquals(skipSpace(), '[')
        var next = skipSpace()
        while (true) {
            when (next) {
                ']' -> return
                ',' -> {}
                else -> readElement()
            }
            next = skipSpace()
        }
    }
}