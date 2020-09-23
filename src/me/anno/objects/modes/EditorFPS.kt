package me.anno.objects.modes

enum class EditorFPS(val value: Int){
    F5(5), F10(10), F24(24), F30(30), F60(60), F(90), F120(120);
    val displayName = value.toString()
    val dValue = value.toDouble()
}