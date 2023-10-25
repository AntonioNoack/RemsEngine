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

const val tcpPort = 65113
const val udpPort = 65112
val tcpProtocol = Protocol("TEST", NetworkProtocol.TCP).apply {
    register { JoinPacket() }
    register { PingPacket() }
    register { MessagePacket() }
    register { ExitPacket() }
}

val udpProtocol = Protocol("UDP", NetworkProtocol.UDP)

fun main() {
    GFXBase.disableRenderDoc()
    testUI3("ServerClientWorld") {
        StudioBase.instance?.enableVSync = true
        DefaultConfig["debug.ui.showRedraws"] = true
        val list = PanelList2D(style)
        val master = PanelListY(style)
        master.add(TextButton("Join All", style)
            .addLeftClickListener {
                list.forAllPanels {
                    if (it is TextButton && it.text == "Join") it.click()
                }
            })
        master.add(TextButton("Leave All", style)
            .addLeftClickListener {
                list.forAllPanels {
                    if (it is TextButton && it.text == "Leave") it.click()
                }
            })
        master.add(TextButton("Test Button", style)
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

    controls.add(TextButton("SendMsg", style)
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

    val leaveJoinButton = TextButton("Join", style)
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
    return TCPClient.createSocket(InetAddress.getByName("localhost"), tcpPort, tcpProtocol)
}

class LocalClient(val instance: Instance) : TCPClient(connect(), tcpProtocol, instance.name, instance.uuid)

class LocalServer : Server() {
    override fun onClientConnected(client: TCPClient) {
        super.onClientConnected(client)
        broadcast(JoinPacket(client.uuid, client.name))
        forAllClients {
            client.sendTCP(JoinPacket(it.uuid, it.name))
        }
    }

    override fun onClientDisconnected(client: TCPClient) {
        super.onClientDisconnected(client)
        broadcast(ExitPacket(client.uuid))
    }
}

class Player(val uuid: String, val name: String) {
    var color = 0
    var px = 0f
    var py = 0f
    var pz = 0f
}

open class Instance(val name: String) {

    var currentId = 0
    var messageLength = 0
    var client: LocalClient? = null
    var server: LocalServer? = null
    val players = HashMap<String, Player>()
    val uuid = Path.generateRandomId()
    val logger = LogManager.getLogger(name)

    open fun onPlayerJoin(player: Player) {
    }

    open fun onPlayerExit(player: Player) {
    }

    fun start() {
        val id = ++currentId
        thread(name = "tcp-$name") {
            while (id == currentId && !Engine.shutdown) {
                try {
                    val server = LocalServer()
                    server.register(tcpProtocol)
                    server.register(udpProtocol)
                    this.server = server
                    server.start(tcpPort, udpPort)
                } catch (e: Exception) {
                    server?.close()
                    server = null
                }
                try {
                    val client = LocalClient(this)
                    client.udpPort = udpPort
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

class JoinPacket(var uuid: String, var name: String) : Packet("JOIN") {

    constructor() : this("", "")

    override fun writeData(server: Server?, client: TCPClient, dos: DataOutputStream) {
        super.writeData(server, client, dos)
        dos.writeUTF(uuid)
        dos.writeUTF(name)
    }

    override fun readData(server: Server?, client: TCPClient, dis: DataInputStream, size: Int) {
        super.readData(server, client, dis, size)
        uuid = dis.readUTF()
        name = dis.readUTF()
    }

    override fun onReceive(server: Server?, client: TCPClient) {
        if (server == null) {
            client as LocalClient
            val player = Player(uuid, name)
            client.instance.players[uuid] = player
            client.instance.onPlayerJoin(player)
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
            val player = client.instance.players.remove(uuid)
            if (player != null) client.instance.onPlayerExit(player)
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

    override val className: String
        get() = "MessagePacket"
}
