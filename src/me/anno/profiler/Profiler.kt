package me.anno.profiler

// todo it would be nice to find the peaks
// todo therefore we need a frame-aware profiler on the main thread, which tracks major changes in runtime of the most important functions
// todo all best profilers cost, or I am already using them (VisualJVM)
// todo therefore, it would be nice to have our own profiler

// todo goals:
// todo inject code into a single function
// todo track function: which functions are called from that?
// todo measure time
// todo efficient statistics: via hash-codes for functions, and a huge array? todo or better: every function gets an index,
// todo and the stats are a structure of arrays
// todo then we only need time measurements

// todo we need a debug window
// todo can we connect remotely to a running JVM? that would be awesome
// todo access to threads
// todo list all threads
object Profiler {
}