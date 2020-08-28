package me.anno.studio

import me.anno.installer.Installer

// todo currently most important topics:
// todo file manager
// todo project management
// todo timeline cutting tool
// todo general settings?
// todo merging cameras?
// todo particle systems
// done move entries in tree

fun main(){
    Installer.checkInstall()
    RemsStudio.run()
}