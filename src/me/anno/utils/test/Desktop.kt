package me.anno.utils.test

import me.anno.utils.OS
import java.awt.Desktop

fun main(){

    // Cannot run program "start": CreateProcess error=2, The system cannot find the file specified
    /*Runtime.getRuntime()
        .exec("start explorer.exe /d ${OS.desktop.absolutePath}")*/

    /*if(Desktop.isDesktopSupported()){
        val desktop = Desktop.getDesktop()
        desktop.open(OS.desktop)
    } else println("Not supported")*/

}