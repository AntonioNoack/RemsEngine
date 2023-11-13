package me.anno.io.json.generic

import java.io.InputStream

open class JsonScanner : JsonReader {
    constructor(data: String) : super(data)
    constructor(data: InputStream) : super(data)
    constructor(data: ByteArray) : super(data)

    fun scanObject(readProperty: (CharSequence) -> Unit) {
        assertEquals(skipSpace(), '{')
        var next = skipSpace()
        while (true) {
            when (next) {
                '}' -> return
                '"' -> {
                    val name = readString(type0, false)
                    assertEquals(skipSpace(), ':')
                    readProperty(name)
                    next = skipSpace()
                }
                ',' -> next = skipSpace()
                else -> assert(next, '}', '"') // fail
            }
        }
    }

    fun scanArray(readElement: () -> Unit) {
        assertEquals(skipSpace(), '[')
        while (true) {
            when (val next = skipSpace()) {
                ']' -> return
                ',' -> {}
                else -> {
                    putBack(next)
                    readElement()
                }
            }
        }
    }
}