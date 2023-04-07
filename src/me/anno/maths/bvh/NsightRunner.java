package me.anno.maths.bvh;

import me.anno.engine.RemsEngine;
import me.anno.gpu.GFXBase;

public class NsightRunner {
    public static void main(String[] args) {
        GFXBase.disableRenderDoc();
        RemsEngine.main(args);
    }
}
