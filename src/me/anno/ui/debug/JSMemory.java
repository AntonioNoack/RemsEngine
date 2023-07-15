package me.anno.ui.debug;

public class JSMemory {
    /**
     * can we query how much memory JS is using?
     * <a href="https://stackoverflow.com/questions/2530228/jquery-or-javascript-to-find-memory-usage-of-page">stackoverflow-article</a>
     * window.performance.memory.usedJSHeapSize ; might be Chrome-only -> edge works; doesn't work in Firefox
     * */
    public static native long jsUsedMemory();
}
