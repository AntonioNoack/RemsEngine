package me.anno.objects.modes

enum class EditorFPS(val value: Int){
    F1(1), F2(2), F3(3), F5(5), F10(10),
    F24(24), F30(30), F60(60), F90(90), F120(120),
    F144(144), F240(240), F300(300), F360(360);
    val displayName = value.toString()
    val dValue = value.toDouble()
}