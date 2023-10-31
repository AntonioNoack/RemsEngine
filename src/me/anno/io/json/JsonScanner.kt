package me.anno.io.json

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