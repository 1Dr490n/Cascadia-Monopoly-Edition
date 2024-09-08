package de.drgn.businadia.server

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import de.drgn.businadia.*
import java.io.File
import java.net.ServerSocket
import java.net.SocketTimeoutException
import kotlin.concurrent.thread
import kotlin.system.exitProcess

object Server {
    private lateinit var serverSocket: ServerSocket

    var playerIndex = 0

    private fun generateFields(): List<Field> {

        val keystones = Region.entries.map { r ->
            listOf(r.a1, r.a1, r.a2, r.a2, r.b).map { b ->
                Field(r, r, 0, setOf(b))
            }
        }.flatten()

        val other = listOf(
            (Region.Park to Region.Commercial) to listOf(
                setOf(Building.Restaurant, Building.Factory, Building.Office),
                setOf(Building.Restaurant, Building.Factory),
                setOf(Building.Restaurant, Building.TrainStation),
                setOf(Building.ShoppingCenter, Building.Office),
                setOf(Building.ShoppingCenter, Building.Factory, Building.TrainStation),
                setOf(Building.Office, Building.TrainStation)
            ),
            (Region.Commercial to Region.Neighborhood) to listOf(
                setOf(Building.ShoppingCenter, Building.Factory, Building.Office),
                setOf(Building.ShoppingCenter, Building.TrainStation),
                setOf(Building.Office, Building.TrainStation),
                setOf(Building.Restaurant, Building.Office),
                setOf(Building.Restaurant, Building.ShoppingCenter, Building.Factory),
                setOf(Building.Restaurant, Building.Factory)
            ),
            (Region.Downtown to Region.Commercial) to listOf(
                setOf(Building.ShoppingCenter, Building.Factory, Building.Office),
                setOf(Building.ShoppingCenter, Building.Office),
                setOf(Building.Office, Building.TrainStation),
                setOf(Building.Factory, Building.TrainStation),
                setOf(Building.Factory, Building.Office, Building.TrainStation),
                setOf(Building.ShoppingCenter, Building.Factory)
            ),
            (Region.Industry to Region.Commercial) to listOf(
                setOf(Building.Restaurant, Building.Factory, Building.TrainStation),
                setOf(Building.Factory, Building.TrainStation),
                setOf(Building.Factory, Building.Office),
                setOf(Building.Restaurant, Building.TrainStation),
                setOf(Building.Office, Building.TrainStation),
                setOf(Building.Restaurant, Building.Factory)
            ),
            (Region.Park to Region.Neighborhood) to listOf(
                setOf(Building.Restaurant, Building.ShoppingCenter, Building.Office),
                setOf(Building.Restaurant, Building.TrainStation),
                setOf(Building.Restaurant, Building.Office),
                setOf(Building.ShoppingCenter, Building.Office),
                setOf(Building.Restaurant, Building.ShoppingCenter, Building.TrainStation),
                setOf(Building.ShoppingCenter, Building.TrainStation)
            ),
            (Region.Park to Region.Downtown) to listOf(
                setOf(Building.ShoppingCenter, Building.TrainStation),
                setOf(Building.Restaurant, Building.Factory),
                setOf(Building.Restaurant, Building.Office),
                setOf(Building.Office, Building.TrainStation),
                setOf(Building.Restaurant, Building.Office, Building.TrainStation),
                setOf(Building.ShoppingCenter, Building.Factory)
            ),
            (Region.Park to Region.Industry) to listOf(
                setOf(Building.Restaurant, Building.Factory, Building.TrainStation),
                setOf(Building.Restaurant, Building.Factory),
                setOf(Building.Restaurant, Building.TrainStation),
                setOf(Building.Factory, Building.TrainStation),
                setOf(Building.Restaurant, Building.ShoppingCenter),
                setOf(Building.ShoppingCenter, Building.TrainStation)
            ),
            (Region.Downtown to Region.Neighborhood) to listOf(
                setOf(Building.Restaurant, Building.ShoppingCenter, Building.Office),
                setOf(Building.Factory, Building.TrainStation),
                setOf(Building.Factory, Building.Office),
                setOf(Building.Restaurant, Building.ShoppingCenter),
                setOf(Building.Restaurant, Building.Office),
                setOf(Building.ShoppingCenter, Building.Office)
            ),
            (Region.Industry to Region.Neighborhood) to listOf(
                setOf(Building.ShoppingCenter, Building.Factory, Building.TrainStation),
                setOf(Building.ShoppingCenter, Building.Factory),
                setOf(Building.ShoppingCenter, Building.TrainStation),
                setOf(Building.Office, Building.TrainStation),
                setOf(Building.Restaurant, Building.ShoppingCenter, Building.Factory),
                setOf(Building.Restaurant, Building.Office)
            ),
            (Region.Downtown to Region.Industry) to listOf(
                setOf(Building.ShoppingCenter, Building.Office, Building.TrainStation),
                setOf(Building.ShoppingCenter, Building.Factory),
                setOf(Building.Restaurant, Building.Factory),
                setOf(Building.Restaurant, Building.ShoppingCenter),
                setOf(Building.Factory, Building.Office),
                setOf(Building.ShoppingCenter, Building.Office)
            )
        )

        val duals = other.map { (regions, buildings) ->
            buildings.map {
                Field(regions.first, regions.second, 0, it)
            }
        }.flatten()

        return (keystones + duals)
    }

    private var rounds = 0

    private lateinit var _availableFields: Array<Field>
    val remainingFields by lazy {
        generateFields().shuffled().take(SPlayer.players.size * rounds + 3).toMutableList().also {
            _availableFields = it.take(4).toTypedArray()
            it.removeAll(_availableFields.toSet())
        }
    }

    val remainingBuildings = Building.entries.times(20).shuffled().toMutableList()

    val availableFields by lazy {
        remainingFields
        _availableFields
    }

    val availableBuildings = remainingBuildings.take(4).also {
        remainingBuildings.subList(0, 4).clear()
    }.toTypedArray()

    fun start(port: Int?, rounds: Int, save: GameState?): Int {
        this.rounds = rounds

        serverSocket = ServerSocket(port ?: 0)

        serverSocket.soTimeout = 500

        var isOpen = true

        println("Listening on port ${serverSocket.localPort}")


        if(save != null) {
            remainingFields.clear()
            remainingFields += save.remainingFields
            remainingBuildings.clear()
            remainingBuildings += save.remainingBuildings
            _availableFields = save.availableFields.toTypedArray()
            save.availableBuildings.forEachIndexed { i, it ->
                availableBuildings[i] = it
            }

            playerIndex = save.playerIndex
        }

        thread {
            readln()
            isOpen = false
        }

        thread {

            val availableNames = save?.players?.map { it.name }
            while (isOpen && SPlayer.players.size < 4) {
                try {
                    SPlayer(serverSocket.accept(), availableNames)
                } catch (_: SocketTimeoutException) {
                }
            }

            if(save != null && save.players.size != SPlayer.players.size) {
                println("The following players are missing:")
                save.players.forEach {
                    if(SPlayer.players.none { p -> p.name == it.name }) {
                        println(" - ${it.name}")
                    }
                }
                exitProcess(1)
            }

            if(save != null)
                SPlayer.players.sortBy { save.players.indexOfFirst { p -> p.name == it.name } }

            println("\nStarting...")

            val numbers = MutableList(5) { it }
            numbers.shuffle()

            SPlayer.players.forEachIndexed { i, player ->

                if(save != null) {
                    save.players.find { it.name == player.name }!!.let { (_, fields, money) ->
                        fields.forEach { (x, y, f) ->
                            player.fields[x, y] = f
                        }
                        player.send(SMessage.Loaded(fields, money, rounds - remainingFields.size / SPlayer.players.size + if(remainingFields.size % SPlayer.players.size < i) 1 else 0))
                    }
                } else {

                    val number = numbers.removeFirst()

                    player.apply {
                        setField(
                            gridSize / 2, gridSize / 2,
                            Field(Region.entries[number], Region.entries[number], 0, setOf(Building.entries[number]))
                        ) // top

                        setField(
                            gridSize / 2 - 1, gridSize / 2 + 1, Field(
                                Region.entries[(number + 1) % Region.entries.size],
                                Region.entries[(number + 2) % Region.entries.size],
                                5,
                                setOf(
                                    Building.entries[number],
                                    Building.entries[(number + 1) % Building.entries.size],
                                    Building.entries[(number + 2) % Building.entries.size],
                                )
                            )
                        ) // left

                        setField(
                            gridSize / 2, gridSize / 2 + 1, Field(
                                Region.entries[(number + 3) % Region.entries.size],
                                Region.entries[(number + 4) % Region.entries.size],
                                0,
                                setOf(
                                    Building.entries[(number + 3) % Building.entries.size],
                                    Building.entries[(number + 4) % Building.entries.size],
                                )
                            )
                        ) // right
                    }
                }

                player.send(SMessage.Init(SPlayer.players.filter { it !== player }.map { it.name }, rounds))

                thread {
                    try {
                        player.handleInputs()
                    } catch (_: Exception) {
                        saveGame()
                        exitProcess(1)
                    }
                }

            }
            nextPlayer()
        }

        return serverSocket.localPort
    }

    fun nextPlayer() {
        playerIndex++
        playerIndex %= SPlayer.players.size

        val player = SPlayer.players[playerIndex]

        println("${player.name}'s turn")

        SPlayer.players.forEach {
            it.send(SMessage.Turn(player.name, player === it))
            it.send(SMessage.Available(availableFields.toList(), availableBuildings.toList()))
        }
    }

    data class GameState(
        val players: List<SPlayer.Serializable>,

        val availableFields: List<Field>,
        val availableBuildings: List<Building>,

        val remainingFields: List<Field>,
        val remainingBuildings: List<Building>,

        val playerIndex: Int
    )

    private fun saveGame() {
        jacksonObjectMapper().writerWithDefaultPrettyPrinter().writeValue(
            File("save.json"), GameState(
                SPlayer.players.map {
                    SPlayer.Serializable(it.name, it.fields.map { (p, f) ->
                        Triple(p.x, p.y, f)
                    }, it.money)
                },

                availableFields.toList(),
                availableBuildings.toList(),

                remainingFields,
                remainingBuildings,

                playerIndex
            )
        )
    }
}