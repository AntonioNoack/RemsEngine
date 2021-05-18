package me.anno.utils.strings

object StringHelper {

    fun String.titlecase(): String {
        if(isEmpty()) return this
        return if(first().isLowerCase()){
            first().uppercase() + substring(1)
        } else this
    }

}