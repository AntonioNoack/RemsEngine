package me.anno.io.xml.saveable

import me.anno.io.xml.generic.XMLReader

// todo read our XML format...
class XMLStringReader(val data: CharSequence) {
    val data1 = XMLReader().read(data.toString().byteInputStream())
}