package me.anno.utils

fun incrementName(name: String): String {
    val lastHash = name.lastIndexOf('#')
    if(lastHash > -1){
        val value = name.substring(lastHash+1).trim().toIntOrNull()
        if(value != null){
            return "${name.substring(0, lastHash)}#${value+1}"
        }
    }
    return "${name.trim()} #2"
}