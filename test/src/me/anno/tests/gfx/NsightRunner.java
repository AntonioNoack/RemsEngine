package me.anno.tests.gfx;

import me.anno.Build;
import me.anno.gpu.GFXBase;

import static me.anno.tests.ui.FileExplorerKt.runFileExplorerTest;

public class NsightRunner {
    public static void main(String[] args) {
        Build.setDebug(false);
        GFXBase.disableRenderDoc();
        // new RemsEngine().run(true);
        runFileExplorerTest();
    }
}
