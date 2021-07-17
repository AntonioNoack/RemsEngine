package me.anno.ecs.prefab

enum class ChangeType0(val id: Int) {
    SET_VALUE(0),
    // remove element? we wouldn't really have an easy way for the ui to bring it back -> no, just disable it
    ADD_ELEMENT(1),
    REMOVE_ELEMENT(2) // cannot really be easily overridden

    ;

    companion object {
        val values = values()
    }
}