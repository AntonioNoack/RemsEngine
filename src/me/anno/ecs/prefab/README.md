# Prefabs

Prefabs are an editor-/save-system inspired by Unity:

Each prefab is a file with:
- root: what class it is itself (e.g., Entity, Component, ...), parent prefab (inheritance)
- children: instances/tree additions (plus parent prefabs for further inheritance)
- properties: property changes (e.g., position, name, ...)

This allows for very modular world building, but brings the cost of eventually pretty deep hierarchies.
Future implementations hopefully will get rid of their runtime cost.

Removing children is impossible with the current model,
but I'm not sure whether I should add it or not.
The current way of "deleting" something is to disable it.