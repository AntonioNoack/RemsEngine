package me.anno.studio

import me.anno.installer.Installer

// todo currently most important topics:
// todo file manager
// todo move entries in tree
// todo project management
// todo timeline cutting tool
// todo general settings?
// todo merging cameras?
// todo particle systems

fun main(){
    Installer.checkInstall()
    RemsStudio.run()
}