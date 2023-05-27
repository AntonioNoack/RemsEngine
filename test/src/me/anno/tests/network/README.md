# Networking Related Tests

## [HTTP Servlet Test](HttpServerTest.kt)

This test shows how Rem's Engine's server-client-protocol description can be adapted to handle HTTP GET requests.
Since it's its own protocol, you can run a webserver on the same port as your game server. (some game server hosters want you to pay extra for that)

## [Server Client World](ServerClientWorld.kt)

Shows a self-managing server-client relation, where the first to open the port becomes the new host;
this is a play example, as this won't work distributed onto multiple computers.

Currently, there is also a UI bug, where you have to resize the window to make the buttons update 😅.

## [UDP Test](UDPTest.kt)

This is a test for Java's UDP, and then our implementation based on that.

## [Jamming Attack](JammingAttack.kt)

There once was a security flaw. I kind of forgot how it worked, but networking seems a little weird at the moment anyway 😅
