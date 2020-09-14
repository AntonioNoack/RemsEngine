package me.anno.ui.editor.files.thumbs

import me.anno.ui.editor.files.thumbs.WindowsThumbDBVersions.vista
import me.anno.ui.editor.files.thumbs.WindowsThumbDBVersions.win10
import me.anno.ui.editor.files.thumbs.WindowsThumbDBVersions.win7
import me.anno.ui.editor.files.thumbs.WindowsThumbDBVersions.win8
import me.anno.ui.editor.files.thumbs.WindowsThumbDBVersions.win8_1
import me.anno.ui.editor.files.thumbs.WindowsThumbDBVersions.win8v2
import me.anno.ui.editor.files.thumbs.WindowsThumbDBVersions.win8v3

// not really used
object Sizes {

    val sizesVista7Map = mapOf(
        0 to 32,
        1 to 96,
        2 to 256,
        3 to 1024,
        4 to "sr"
    )

    val sizesWin8Map = mapOf(
        0 to 16,
        1 to 32,
        2 to 48,
        3 to 96,
        4 to 256,
        5 to 1024,
        6 to "sr",
        7 to "wide",
        8 to "exif"
    )

    val sizesWin81Map = mapOf(
        0 to 16,
        1 to 32,
        2 to 48,
        3 to 96,
        4 to 256,
        5 to 1024,
        6 to 1600,
        7 to "sr",
        8 to "wide",
        9 to "exif",
        0xa to "wide_alternate"
    )

    val sizesWin10Map = mapOf(
        0 to 16,
        1 to 32,
        2 to 48,
        3 to 96,
        4 to 256,
        5 to 768,
        6 to 1280,
        7 to 1920,
        8 to 2560,
        9 to "sr",
        0xA to "wide",
        0xB to "exif",
        0xC to "wide_alternate",
        0xD to "custom_stream"
    )

    val sizesMap = mapOf(
        vista to sizesVista7Map,
        win7 to sizesVista7Map,
        win10 to sizesWin10Map,
        win8_1 to sizesWin81Map,
        win8 to sizesWin8Map,
        win8v2 to sizesWin8Map,
        win8v3 to sizesWin8Map
    )

}