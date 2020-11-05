package me.anno.utils

import me.anno.config.DefaultConfig
import java.io.File
import java.util.*

object FileHelper {

    fun Long.formatFileSize(): String {
        val endings = "kMGTPEZY"
        val divider = if(DefaultConfig["ui.file.showGiB", true]) 1024 else 1000
        val suffix = if(divider == 1024) "i" else ""
        val halfDivider = divider/2
        var v = this
        if(v < halfDivider) return "$v Bytes"
        for(prefix in endings){
            val vSaved = v
            v = (v + halfDivider)/divider
            if(v < divider){
                return "${when(v){
                    in 0 .. 9 -> "%.2f".format(Locale.ENGLISH, (vSaved.toFloat() / divider))
                    in 10 .. 99 -> "%.1f".format(Locale.ENGLISH, (vSaved.toFloat() / divider))
                    else -> v.toString()
                }} ${prefix}B$suffix"
            }
        }
        return "$v ${endings.last()}B$suffix"
    }

    fun File.listFiles2(includeHiddenFiles: Boolean = OS.isWindows) = listFiles()?.filter {
        !it.name.equals("desktop.ini", true) && (!name.startsWith('.') || !includeHiddenFiles) } ?: emptyList()

    fun File.openInExplorer(){
        if(!exists()){
            parentFile?.openInExplorer() ?: LOGGER.warn("Cannot open file $this, as it does not exist!")
        } else {
            when {
                OS.isWindows -> {// https://stackoverflow.com/questions/2829501/implement-open-containing-folder-and-highlight-file
                    OS.startProcess("explorer.exe", "/select,", absolutePath)
                }
                OS.isLinux -> {// https://askubuntu.com/questions/31069/how-to-open-a-file-manager-of-the-current-directory-in-the-terminal
                    OS.startProcess("xdg-open", absolutePath)
                }
            }
        }
    }

}