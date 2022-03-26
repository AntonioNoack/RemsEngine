package me.anno.ecs.annotations

/**
 * hides the variable, that is annotated, from the inspector;
 * if multiple annotations of this class are present, the results are or-red
 * @param hideIfVariableIsTrue empty: always hide; non-empty: if local member variable with that name is true, hide it
 * */
annotation class HideInInspector(val hideIfVariableIsTrue: String = "")
