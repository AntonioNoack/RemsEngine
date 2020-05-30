package me.anno.input

import me.anno.io.utils.StringMap
import me.anno.ui.base.Panel

object ActionManager {

    val localActions = HashMap<Pair<String, KeyCombination>, Action>()

    val globalActions = HashMap<KeyCombination, Action>()

    fun init(){

        val defaultValue = StringMap()
        defaultValue[""]

    }

    fun press(keyComb: KeyCombination, inFocus: Panel?){
        if(inFocus != null){
            val local = localActions[inFocus.getClassName() to keyComb] ?: return press(keyComb)
            executeLocally(inFocus, local)
        } else press(keyComb)
    }

    fun press(keyComb: KeyCombination){
        val global = globalActions[keyComb] ?: return
        executeGlobally(global)
    }

    fun executeLocally(panel: Panel, action: Action){

    }

    fun executeGlobally(action: Action){
        // todo execute globally

    }

}