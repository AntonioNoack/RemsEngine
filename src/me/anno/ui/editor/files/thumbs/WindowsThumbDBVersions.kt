package me.anno.ui.editor.files.thumbs

object WindowsThumbDBVersions {

    val vista = 0x14
    val win7 = 0x15
    val win8 = 0x1a
    val win8v2 = 0x1c
    val win8v3 = 0x1e
    val win8_1 = 0x1f
    val win10 = 0x20

    val version = mapOf(
        vista to "Vista",
        win7 to "Win7",
        win8 to "Win8", win8v2 to "Win8", win8v3 to "Win8",
        win8_1 to "Win8.1",
        win10 to "Win10"
    )

}