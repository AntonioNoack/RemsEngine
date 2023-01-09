package me.anno.build

// run engine in web browser
// ideas: JVM to WASM/JavaScript: https://github.com/appcypher/awesome-wasm-langs#java
// TeaVM - https://github.com/konsoletyper/teavm, idk -> doesn't work :/, neither is able to produce JS nor WASM code
// JWebAssembly - https://github.com/i-net-software/JWebAssembly, sounds ok, Apache 2 license; no GC? 🤨
// Bytecoder - https://github.com/mirkosertic/Bytecoder, looks good :), Apache 2 license -> doesn't work, why ever...
// CheerpJ - costs money
// JSweet - https://www.jsweet.org/, Java -> JS; so it needs all code to be Java -> we need a good decompiler
// JVM2WASM - lots of future work, need to get it working...