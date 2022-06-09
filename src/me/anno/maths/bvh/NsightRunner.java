package me.anno.maths.bvh;

import me.anno.gpu.GFX;
import me.anno.maths.paths.PathFindingAccTestKt;

public class NsightRunner {
    public static void main(String[] args) {
        GFX.INSTANCE.setDisableRenderDoc(true);
        PathFindingAccTestKt.main();
    }
}
