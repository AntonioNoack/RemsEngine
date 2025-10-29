package me.anno

// todo Anim-Model blending (top/bottom) around rest-pose by plane or spherical distance

// todo SDF Physics

// todo can we make everything exception-throwing-free? that would help porting (e.g. to languages like Rust, where exceptions are rare/impossible),
//  and we would be able to disable exceptions on WASM
//  -> get rid of all "!!"
//  -> get rid of all "throw"
//  -> get rid of all "as", but "as?" is fine
