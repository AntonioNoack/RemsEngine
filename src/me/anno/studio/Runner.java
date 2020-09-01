package me.anno.studio;

// todo currently most important topics:
// todo file manager
// todo project management
// todo timeline cutting tool
// todo general settings?
// todo merging cameras?
// todo particle systems
// done move entries in tree

import me.anno.installer.Installer;

public class Runner {
    public static void main(String[] args){
        Installer.INSTANCE.checkInstall();
        RemsStudio.INSTANCE.run();
    }
}
