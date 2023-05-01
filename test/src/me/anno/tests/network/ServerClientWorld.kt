package me.anno.tests.network

import me.anno.Engine
import me.anno.config.DefaultConfig
import me.anno.config.DefaultConfig.style
import me.anno.ecs.prefab.change.Path
import me.anno.gpu.GFXBase
import me.anno.language.translation.NameDesc
import me.anno.network.*
import me.anno.network.packets.PingPacket
import me.anno.studio.StudioBase
import me.anno.ui.Panel
import me.anno.ui.base.buttons.TextButton
import me.anno.ui.base.groups.PanelList2D
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.base.menu.Menu.askName
import me.anno.ui.base.scrolling.ScrollPanelY
import me.anno.ui.base.text.UpdatingSimpleTextPanel
import me.anno.ui.debug.TestStudio.Companion.testUI3
import org.apache.logging.log4j.LogManager
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.InetAddress
import java.net.Socket
import kotlin.concurrent.thread

val port1 = 65113
val protocol = Protocol("TEST", NetworkProtocol.TCP).apply {
    register { EnterPacket() }
    register { PingPacket() }
    register { MessagePacket() }
    register { ExitPacket() }
}

fun main() {
    GFXBase.disableRenderDoc()
    testUI3 {
        StudioBase.instance?.enableVSync = true
        DefaultConfig["debug.ui.showRedraws"] = true
        // todo scrolling breaks when any client joins... why???
        val list = PanelList2D(style)
        val master = PanelListY(style)
        master.add(TextButton("Join All", false, style)
            .addLeftClickListener {
                list.forAllPanels {
                    if (it is TextButton && it.text == "Join") it.click()
                }
            })
        master.add(TextButton("Leave All", false, style)
            .addLeftClickListener {
                list.forAllPanels {
                    if (it is TextButton && it.text == "Leave") it.click()
                }
            })
        master.add(TextButton("Test Button", false, style)
            .addLeftClickListener {
            })
        list.add(master)
        list.childWidth = 120
        list.childHeight = 90
        for (i in 0 until 100) {
            list.add(createClient(i))
        }
        ScrollPanelY(list, style)
    }
}

fun createClient(i: Int): Panel {

    val clientName = "Instance $i"

    val client = Instance(clientName)
    val controls = PanelListY(style)

    controls.add(UpdatingSimpleTextPanel(0, style) {
        "$i" +
                (if (client.server != null) "s" else "") +
                (if (client.client?.isRunning == true) "c" else "") +
                ": ${client.players.size}p ${client.messageLength}m"
    })

    controls.add(TextButton("SendMsg", false, style)
        .addLeftClickListener {
            askName(controls.windowStack,
                NameDesc("Enter Messsage"), "",
                NameDesc("Send"), { -1 }, {
                    if (it.isNotBlank()) {
                        client.client?.sendTCP(MessagePacket().apply {
                            this.uuid = clientName
                            message = it.trim()
                        })
                    }
                })
        })

    val leaveJoinButton = TextButton("Join", false, style)
    controls.add(leaveJoinButton
        .addLeftClickListener {
            if (client.client != null) {
                client.stop()
                leaveJoinButton.text = "Join"
            } else {
                client.start()
                leaveJoinButton.text = "Leave"
            }
        })

    return controls
}

fun connect(): Socket {
    return TCPClient.createSocket(InetAddress.getByName("localhost"), port1, protocol)
}

class LocalClient(val instance: Instance) : TCPClient(connect(), protocol, instance.name, instance.uuid)

class LocalServer : Server() {
    override fun onClientConnected(client: TCPClient) {
        super.onClientConnected(client)
        broadcast(EnterPacket(client.uuid))
        forAllClients {
            client.sendTCP(EnterPacket(it.uuid))
        }
    }

    override fun onClientDisconnected(client: TCPClient) {
        super.onClientDisconnected(client)
        broadcast(ExitPacket(client.uuid))
    }
}

class Instance(val name: String) {

    var currentId = 0
    var messageLength = 0
    var client: LocalClient? = null
    var server: LocalServer? = null
    val players = HashSet<String>()
    val uuid = Path.generateRandomId()
    val logger = LogManager.getLogger(name)

    fun start() {
        val id = ++currentId
        thread(name = "tcp-$name") {
            while (id == currentId && !Engine.shutdown) {
                try {
                    val server = LocalServer()
                    server.register(protocol)
                    this.server = server
                    server.start(port1, -1)
                } catch (e: Exception) {
                    server?.close()
                    server = null
                }
                try {
                    val client = LocalClient(this)
                    this.client = client
                    client.startClientSide()
                } catch (e: Exception) {
                    client?.close()
                    client = null
                    logger.info("$e")
                }
                // we're no longer connected, so remove all
                players.clear()
                messageLength = 0
                Thread.sleep(5)
            }
        }
    }

    fun stop() {
        currentId++
        client?.close()
        server?.close()
        client = null
        server = null
    }

}

class EnterPacket(var uuid: String = "") : Packet("JOIN") {

    override fun writeData(server: Server?, client: TCPClient, dos: DataOutputStream) {
        super.writeData(server, client, dos)
        dos.writeUTF(uuid)
    }

    override fun readData(server: Server?, client: TCPClient, dis: DataInputStream, size: Int) {
        super.readData(server, client, dis, size)
        uuid = dis.readUTF()
    }

    override fun onReceive(server: Server?, client: TCPClient) {
        if (server == null) {
            client as LocalClient
            client.instance.players.add(uuid)
        }
    }

}

class ExitPacket(var uuid: String = "") : Packet("LEFT") {

    override fun writeData(server: Server?, client: TCPClient, dos: DataOutputStream) {
        super.writeData(server, client, dos)
        dos.writeUTF(uuid)
    }

    override fun readData(server: Server?, client: TCPClient, dis: DataInputStream, size: Int) {
        super.readData(server, client, dis, size)
        uuid = dis.readUTF()
    }

    override fun onReceive(server: Server?, client: TCPClient) {
        if (server == null) {
            client as LocalClient
            client.instance.players.remove(uuid)
        }
    }
}

class MessagePacket : Packet("MSG") {

    var uuid = ""
    var message = ""

    override fun readData(server: Server?, client: TCPClient, dis: DataInputStream, size: Int) {
        super.readData(server, client, dis, size)
        uuid = dis.readUTF()
        message = dis.readUTF()
    }

    override fun writeData(server: Server?, client: TCPClient, dos: DataOutputStream) {
        super.writeData(server, client, dos)
        dos.writeUTF(uuid)
        dos.writeUTF(message)
    }

    override fun onReceive(server: Server?, client: TCPClient) {
        if (server != null) server.broadcast(this)
        else {
            client as LocalClient
            client.instance.messageLength += message.length
        }
    }
}
