package me.anno.engine.scene

// old file:

// todo in Unity, there is also the state "was changed, but is the same as the original"
//  - we should have that! how do we keep track of that? we need another property, I think


// entity
//      components; maybe inherited, maybe custom
//      children entities; maybe inherited, maybe custom

// todo load the scene from a tree of files
// todo for the shipped game, pack all scene files into a separate zip file,
// todo and preload all values, so we get faster access times


// todo load the basic values, json
// todo load all children: either json or a reference
// todo or just load all, and then replace the references? yes :)
