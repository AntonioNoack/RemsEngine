# User Interface

This is the custom UI library of Rem's Engine.
It's inspired by Android's UI system, just I've named the classes "Panel" instead of "View" in most cases.

The most interesting classes for beginners are in the "base", and "base/groups" sub directories.

Each panel has a minimum size, which it wants to preserve, and then layout options when more space is available.
Each panel can have customized rendering login in the onDraw() method.
Generally, the library should be highly customizable.

It is also used by the Engine's editor, and Rem's Studio.
Panels are also a mandatory component for any window.

To test your own UI elements, use the methods testUI(title, panel) or testUI3(title, panel).
The latter ensures that your element uses the whole available space, not just the minimum.