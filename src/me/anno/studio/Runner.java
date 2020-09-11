package me.anno.studio;

// todo currently most important topics:
// todo file manager
// todo project management
// todo timeline cutting tool
// todo general settings?
// todo merging cameras?
// todo particle systems
// done move entries in tree

import me.anno.config.DefaultConfig;
import me.anno.installer.Installer;

public class Runner {
    public static void main(String[] args){
        DefaultConfig.INSTANCE.getName();
        Installer.INSTANCE.checkInstall();
        RemsStudio.INSTANCE.run();
    }
}
