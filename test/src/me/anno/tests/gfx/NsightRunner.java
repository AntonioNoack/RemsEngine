package me.anno.tests.gfx;

import me.anno.Build;

import static me.anno.gpu.RenderDoc.disableRenderDoc;
import static me.anno.tests.engine.light.SpotLightKt.spotLightTest;

public class NsightRunner {
    public static void main(String[] args) {
        Build.setDebug(false);
        disableRenderDoc();
        // new RemsEngine().run(true);
        // runFileExplorerTest();
        spotLightTest();
    }
}
