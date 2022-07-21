package me.anno.maths.bvh;

import me.anno.engine.RemsEngine;
import me.anno.gpu.GFX;

public class NsightRunner {
    public static void main(String[] args) {
        GFX.INSTANCE.disableRenderDoc();
        RemsEngine.main(args);
    }
}
