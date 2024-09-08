package de.drgn.businadia.client

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import de.drgn.businadia.*
import de.drgn.businadia.client.gui.ClientFrame
import de.drgn.businadia.client.gui.GamePanel
import de.drgn.businadia.client.gui.LoginPanel
import java.awt.Rectangle
import java.awt.geom.Ellipse2D
import java.io.BufferedReader
import java.io.BufferedWriter
import java.net.Socket
import javax.sound.sampled.AudioSystem
import kotlin.concurrent.thread
import kotlin.system.exitProcess

object Client {

    fun send(message: CMessage) {
        writer.write(jacksonObjectMapper().writeValueAsString(message))
        writer.newLine()
        writer.flush()
    }

    lateinit var currentPlayer: CPlayer

    lateinit var player: CPlayer

    private lateinit var socket: Socket
    private lateinit var reader: BufferedReader
    private lateinit var writer: BufferedWriter

    var currentPlayersTurn: String? = ""

    val availableFields by lazy {
        List(4) {
            Tile(
                0,
                0,
                GamePanel.width - 325 / 2 + Tile.RADIUS,
                (GamePanel.height / 2 - 4.5 * Tile.RADIUS + it * 3 * Tile.RADIUS).toInt()
            )
        }
    }

    val availableBuildingsBounds by lazy {
        List(4) { i ->
            val x = GamePanel.width - 325 / 2 - 1 * Tile.RADIUS
            val y = (GamePanel.height / 2 - 4.5 * Tile.RADIUS + i * 3 * Tile.RADIUS - Tile.RADIUS / 2).toInt()

            Rectangle(x, y, Tile.RADIUS, Tile.RADIUS) to Ellipse2D.Double(
                x.toDouble(),
                y.toDouble(),
                Tile.RADIUS.toDouble(),
                Tile.RADIUS.toDouble()
            )
        }
    }

    val availableBuildings = Array<Building?>(4) { null }

    var round = 1
    var rounds = 0

    val notifications = mutableListOf<Pair<String, Long>>()
    fun notification(str: String, sound: Boolean = false) {
        if (sound) {
            val clip = AudioSystem.getClip()
            val notificationSound = AudioSystem.getAudioInputStream(loadFile("notification.wav"))
            clip.open(notificationSound)
            clip.start()
        }
        notifications += str to System.currentTimeMillis()
    }

    fun start(host: String, port: Int, name: String): Nothing {
        currentPlayer = CPlayer(name)

        player = currentPlayer

        socket = Socket(host, port)

        LoginPanel.playButton.text = "Waiting for host to start the game..."

        reader = socket.getInputStream().bufferedReader()
        writer = socket.getOutputStream().bufferedWriter()

        writer.write(player.name)
        writer.newLine()
        writer.flush()

        thread {
            while (socket.isConnected) {
                GamePanel.repaint()
            }
        }

        while (true) {
            val message = try {
                jacksonObjectMapper().readValue<SMessage>(reader.readLine())
            } catch (e: Exception) {
                exitProcess(1)
            }
            println(message.javaClass.name)
            when (message) {
                is SMessage.Field -> player.grid[message.x][message.y].field = message.field
                is SMessage.Available -> {
                    println(message.fields)
                    message.fields?.forEachIndexed { i, it -> availableFields[i].field = it }
                    message.buildings.forEachIndexed { i, it -> availableBuildings[i] = it }

                    GamePanel.placedField = null
                    GamePanel.placedBuilding = null
                }

                is SMessage.Turn -> {
                    currentPlayersTurn = message.player.takeUnless { message.isYou }
                    notification("It's ${currentPlayersTurn?.plus("'s") ?: "your"} turn!", currentPlayersTurn == null)

                    if (currentPlayersTurn == null) {
                        for (building in Building.entries) {
                            if (availableBuildings.count { it == building } == 3) {
                                notification("You can improve the diversity.")
                                break
                            }
                        }
                    }
                }

                is SMessage.Init -> {
                    CPlayer.players = message.players
                    rounds = message.rounds

                    ClientFrame.remove(LoginPanel)
                    ClientFrame.add(GamePanel)
                    ClientFrame.revalidate()
                    ClientFrame.repaint()

                    GamePanel.requestFocus()
                }

                is SMessage.ViewPlayer -> {
                    currentPlayer = CPlayer(message.name)
                    message.fields.forEach { (x, y, f) ->
                        currentPlayer.grid[x][y].field = f
                    }
                    currentPlayer.realResult.money = message.money
                    currentPlayer.realResult.buildings = evaluateBuildings(currentPlayer.grid)
                    currentPlayer.realResult.regions = evaluateRegions(currentPlayer.grid)
                }

                is SMessage.OthersTurn -> {
                    if (currentPlayer.name == message.player) {
                        currentPlayer.grid[message.field.x][message.field.y].field =
                            availableFields[message.field.index].field

                        message.building?.let {
                            currentPlayer.grid[it.x][it.y].field?.building = availableBuildings[it.index]
                        }
                        currentPlayer.grid[message.field.x][message.field.y].field!!.rotation = message.field.rotation

                        currentPlayer.realResult.money = message.money
                        currentPlayer.realResult.buildings = evaluateBuildings(currentPlayer.grid)
                        currentPlayer.realResult.regions = evaluateRegions(currentPlayer.grid)
                    }
                }

                SMessage.End -> {
                    currentPlayersTurn = "."
                    notification("The game has ended!", true)
                }

                is SMessage.LoginError -> {
                    socket.close()
                    throw SMessage.LoginError.Exception(message.message)
                }

                is SMessage.Loaded -> {
                    message.fields.forEach { (x, y, f) ->
                        player.grid[x][y].field = f
                    }
                    player.realResult.money = message.money

                    player.realResult.buildings = evaluateBuildings(player.grid)
                    player.realResult.regions = evaluateRegions(player.grid)
                    round = message.round
                }
                is SMessage.DiversityImproved -> {
                    notification("The diversity has been improved.")
                }
            }
        }
    }
}
