package de.drgn.businadia.server

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import de.drgn.businadia.*
import java.awt.Point
import java.net.Socket

class SPlayer(socket: Socket, availableNames: List<String>?) {
    private val reader = socket.getInputStream().bufferedReader()
    private val writer = socket.getOutputStream().bufferedWriter()
    val name: String = reader.readLine().also { name ->
        when {
            players.any { it.name == name } -> {
                send(SMessage.LoginError("This name has already been taken."))
            }
            availableNames != null && !availableNames.contains(name) -> {
                send(SMessage.LoginError("This player does not exist."))
            }
            else -> {
                players += this
                println("\r\u001B[2A$name")
                print("Press [Enter] to start. (${players.size} / 4)")
            }
        }
    }

    val fields = HashMap<Point, Field>()
    var money = 0

    fun send(message: SMessage) {
        writer.write(jacksonObjectMapper().writeValueAsString(message))
        writer.newLine()
        writer.flush()
    }

    fun setField(x: Int, y: Int, field: Field) {
        fields[x, y] = field

        send(SMessage.Field(x, y, field))
    }

    companion object {
        val players = mutableListOf<SPlayer>()
    }

    fun handleInputs(): Nothing {
        while (true) {
            val message = jacksonObjectMapper().readValue<CMessage>(reader.readLine())

            when (message) {
                is CMessage.Turn -> {

                    if (players[Server.playerIndex] !== this || Server.remainingFields.isEmpty())
                        continue

                    fields[message.field.x, message.field.y] = Server.availableFields[message.field.index]

                    fields[message.field.x, message.field.y]!!.rotation = message.field.rotation

                    message.building?.let {
                        fields[message.building.x, message.building.y]!!.building =
                            Server.availableBuildings[it.index]
                    } ?: run {
                        Server.remainingBuildings.add(
                            Server.remainingBuildings.indices.random(),
                            Server.availableBuildings[message.field.index]
                        )
                    }

                    money = message.money

                    players.forEach {
                        if (it !== this)
                            it.send(SMessage.OthersTurn(name, message.field, message.building, message.money))
                    }

                    Server.availableFields[message.field.index] = Server.remainingFields.removeFirst()

                    if (Server.remainingFields.isEmpty()) {
                        players.forEach {
                            it.send(SMessage.End)
                        }
                        continue
                    }

                    Server.availableBuildings[message.building?.index ?: message.field.index] =
                        Server.remainingBuildings.removeFirst()

                    diversityCheck()

                    Server.nextPlayer()
                }

                CMessage.Overpopulation -> {

                    if (players[Server.playerIndex] !== this)
                        continue

                    for (building in Building.entries) {
                        if (Server.availableBuildings.count { it == building } == 3) {
                            for (i in Server.availableBuildings.indices) {
                                if (Server.availableBuildings[i] == building) {
                                    Server.availableBuildings[i] = Server.remainingBuildings.removeFirst()
                                }
                            }
                            repeat(3) {
                                Server.remainingBuildings.add(
                                    Server.remainingBuildings.indices.random(),
                                    building
                                )
                            }
                            break
                        }
                    }

                    diversityCheck()

                    players.forEach {
                        it.send(SMessage.DiversityImproved)
                        it.send(SMessage.Available(null, Server.availableBuildings.toList()))
                    }
                }

                is CMessage.ViewPlayer -> {
                    val p = players.find { it.name == message.player } ?: continue
                    send(
                        SMessage.ViewPlayer(
                            p.name,
                            p.fields.map { Triple(it.key.x, it.key.y, it.value) },
                            p.money
                        )
                    )
                }
            }
        }
    }

    private fun diversityCheck() {
        while (Server.availableBuildings.drop(1).all { it == Server.availableBuildings[0] }) {
            val building = Server.availableBuildings[0]

            for (i in Server.availableBuildings.indices)
                Server.availableBuildings[i] = Server.remainingBuildings.removeFirst()

            repeat(4) {
                Server.remainingBuildings.add(
                    Server.remainingBuildings.indices.random(),
                    building
                )
            }
            players.forEach {
                it.send(SMessage.DiversityImproved)
            }
        }
    }

    data class Serializable(val name: String, val fields: List<Triple<Int, Int, Field>>, val money: Int)
}