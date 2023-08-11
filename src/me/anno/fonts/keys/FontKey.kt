package me.anno.fonts.keys

class FontKey(var name: String, var sizeIndex: Int, var bold: Boolean, var italic: Boolean) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FontKey) return false
        return name == other.name && sizeIndex == other.sizeIndex &&
                bold == other.bold && italic == other.italic
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + sizeIndex
        result = 31 * result + bold.hashCode()
        result = 31 * result + italic.hashCode()
        return result
    }
}