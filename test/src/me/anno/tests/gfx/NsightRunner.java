package me.anno.tests.gfx;

import me.anno.Build;
import me.anno.engine.RemsEngine;

import static me.anno.gpu.RenderDoc.disableRenderDoc;
import static me.anno.tests.engine.light.SpotLightKt.spotLightTest;
import static me.anno.tests.ui.FileExplorerKt.runFileExplorerTest;

public class NsightRunner {
    public static void main(String[] args) {
        Build.setDebug(false);
        disableRenderDoc();
        // new RemsEngine().run(true);
        // runFileExplorerTest();
        spotLightTest();
    }
}
