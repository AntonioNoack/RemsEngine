package me.anno.utils

/**
 * annotates classes and function that should not be used in applications, only in the engine;
 * sometimes, I have to make fields public even if I don't want to: for portability, and modularity
 * */
annotation class InternalAPI