package de.drgn.businadia.client.gui

import de.drgn.businadia.*
import de.drgn.businadia.client.CPlayer
import de.drgn.businadia.client.Client
import de.drgn.businadia.client.Client.availableBuildings
import de.drgn.businadia.client.Client.availableBuildingsBounds
import de.drgn.businadia.client.Client.availableFields
import de.drgn.businadia.client.Client.currentPlayer
import de.drgn.businadia.client.Client.currentPlayersTurn
import de.drgn.businadia.client.Client.notification
import de.drgn.businadia.client.Client.player
import de.drgn.businadia.client.Client.round
import de.drgn.businadia.client.Tile
import java.awt.*
import java.awt.event.*
import javax.swing.JPanel

object GamePanel : JPanel() {
    var selectedField: Field? = null
    var selectedHexagonIndex: Int? = null

    var selectedBuildingIndex: Int? = null

    var hovered: Tile? = null
    var hoveredRegion: Region? = null
    var hoveredBuilding: Building? = null

    var placedField: Pair<Tile, Int>? = null
    var placedBuilding: Pair<Tile, Int>? = null

    var debugOverlay = false

    init {
        background = Color(5, 5, 5)

        addMouseListener(object : MouseAdapter() {
            override fun mouseReleased(e: MouseEvent) {
                super.mouseReleased(e)

                if (currentPlayersTurn != null || currentPlayer !== player)
                    return

                if (e.x > width - 325) {
                    selectedHexagonIndex =
                        availableFields.indexOfFirst { it.polygon.contains(e.x, e.y) }.takeUnless { it < 0 }?.also {
                            if (placedField != null)
                                notification("You have already placed a tile.")
                        }

                    selectedBuildingIndex =
                        availableBuildingsBounds.indexOfFirst { (_, it) ->
                            it.contains(
                                e.x.toDouble(),
                                e.y.toDouble()
                            )
                        }.also {
                            if (it != -1) {
                                if (placedField == null)
                                    notification("Place a tile first.")
                                if (placedBuilding != null)
                                    notification("You have already placed a building.")
                            }
                        }.takeUnless {
                            it < 0 || placedField == null || placedBuilding != null || (it != placedField?.second && player.realResult.money <= 0).also {
                                if (it && placedField != null && placedBuilding == null) notification("You don't have enough money.")
                            }
                        }

                    if (placedField == null) {
                        selectedHexagonIndex?.let {
                            selectedField = availableFields[it].field!!.copy()
                        } ?: run {
                            selectedField = null
                        }
                    } else if (placedBuilding == null) {
                        val minus =
                            if (placedField?.second != selectedBuildingIndex && selectedBuildingIndex != null) -1 else 0
                        player.newResult.money = player.realResult.money + minus
                    }
                    return
                }

                val hexagon = player.grid.flatten().find { it.polygon.contains(e.x, e.y) } ?: return
                if (
                    selectedField != null
                    && hexagon.field == null
                    && hexagon.neighbors.any {
                        it?.field != null && placedField?.first != it
                    }
                ) {
                    hexagon.field = selectedField
                    selectedField = null
                    availableFields[selectedHexagonIndex!!].field = null

                    placedField = hexagon to selectedHexagonIndex!!

                    selectedHexagonIndex = null

                    player.newResult.regions = evaluateRegions(player.grid)

                    return
                }
                if (
                    selectedBuildingIndex != null
                    && hexagon.field != null
                    && hexagon.field?.building == null
                    && availableBuildings[selectedBuildingIndex!!] in hexagon.field!!.availableBuildings
                ) {

                    hexagon.field?.building = availableBuildings[selectedBuildingIndex!!]
                    availableBuildings[selectedBuildingIndex!!] = null

                    placedBuilding = hexagon to selectedBuildingIndex!!

                    selectedBuildingIndex = null

                    val minus =
                        if (placedField?.second != placedBuilding?.second && placedBuilding?.second != null) -1 else 0
                    val plus = if (placedBuilding?.first?.field?.let { it.region1 == it.region2 } == true) 1 else 0

                    player.newResult.money = player.realResult.money + minus + plus
                    player.newResult.buildings = evaluateBuildings(player.grid)

                    return
                }
            }
        })

        addMouseMotionListener(object : MouseMotionAdapter() {
            override fun mouseMoved(e: MouseEvent) {
                super.mouseMoved(e)

                hovered = currentPlayer.grid.flatten().find { it.polygon.contains(e.x, e.y) }
                    ?: availableFields.find { it.polygon.contains(e.x, e.y) }

                val hoveredTile = hovered

                hoveredTile?.field?.let { f ->
                    val polygon = hoveredTile.polygon.subPoly(f.rotation, 4)

                    hoveredRegion = if (polygon.contains(e.x, e.y))
                        f.region1
                    else f.region2

                    hoveredBuilding = f.building
                } ?: run {

                    hoveredBuilding =
                        availableBuildingsBounds.indexOfFirst { it.second.contains(e.x.toDouble(), e.y.toDouble()) }
                            .takeUnless {
                                it < 0
                            }?.let {
                                availableBuildings[it]
                            }

                    hoveredRegion = null
                }
            }
        })

        addKeyListener(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                super.keyPressed(e)

                when (e.keyCode) {
                    KeyEvent.VK_ESCAPE, KeyEvent.VK_1 -> {
                        currentPlayer = player
                        Test.testBuilding = null
                    }

                    in KeyEvent.VK_2..KeyEvent.VK_9 -> {
                        Test.testBuilding = null
                        val player = CPlayer.players.getOrNull(e.keyCode - KeyEvent.VK_2)
                        if (player != null) {
                            Client.send(CMessage.ViewPlayer(player))
                        }
                    }

                    KeyEvent.VK_C -> Test.testBuilding(Building.Restaurant)
                    KeyEvent.VK_V -> Test.testBuilding(Building.ShoppingCenter)
                    KeyEvent.VK_B -> Test.testBuilding(Building.Factory)
                    KeyEvent.VK_N -> Test.testBuilding(Building.Office)
                    KeyEvent.VK_M -> Test.testBuilding(Building.TrainStation)

                    KeyEvent.VK_P -> debugOverlay = !debugOverlay
                }

                if (currentPlayersTurn != null || currentPlayer !== player)
                    return

                when (e.keyCode) {
                    KeyEvent.VK_A -> {
                        selectedField?.let {
                            it.rotation--
                            if (it.rotation < 0)
                                it.rotation = 5
                        }
                    }

                    KeyEvent.VK_D -> {
                        selectedField?.let {
                            it.rotation++
                            it.rotation %= 6
                        }
                    }

                    /*KeyEvent.VK_LEFT -> {
                        if(placedBuilding == null && placedField == null && currentPlayer.grid[0].none { it.field != null }) {
                            for (x in 0..<gridSize - 1) {
                                for (y in 0..<gridSize) {
                                    currentPlayer.grid[x][y].field = currentPlayer.grid[x + 1][y].field
                                }
                            }
                            currentPlayer.grid.last().forEach {
                                it.field = null
                            }
                        }
                    }
                    KeyEvent.VK_RIGHT -> {
                        if(placedBuilding == null && placedField == null && currentPlayer.grid.last().none { it.field != null }) {
                            for (x in gridSize - 1 downTo 1) {
                                for (y in 0..<gridSize) {
                                    currentPlayer.grid[x][y].field = currentPlayer.grid[x - 1][y].field
                                }
                            }
                            currentPlayer.grid[0].forEach {
                                it.field = null
                            }
                        }
                    }*/

                    KeyEvent.VK_ENTER -> {
                        if (placedField != null) {

                            if (placedBuilding != null && placedBuilding?.second != placedField?.second)
                                player.realResult.money--

                            if (placedBuilding?.first?.field?.let { it.region1 == it.region2 } == true)
                                player.realResult.money++

                            Client.send(
                                CMessage.Turn(
                                    placedField!!.let { (hex, i) ->
                                        CMessage.Turn.Field(
                                            hex.x,
                                            hex.y,
                                            hex.field!!.rotation,
                                            i
                                        )
                                    },
                                    placedBuilding?.let { (hex, i) -> CMessage.Turn.Building(hex.x, hex.y, i) },
                                    player.realResult.money
                                )
                            )

                            round++

                            if (placedBuilding != null)
                                player.realResult.buildings = evaluateBuildings(player.grid)

                            player.realResult.regions = evaluateRegions(player.grid)

                            placedField = null
                            placedBuilding = null
                            selectedBuildingIndex = null
                        }
                    }

                    KeyEvent.VK_Z -> {
                        placedBuilding?.let { (hex, i) ->
                            availableBuildings[i] = hex.field?.building
                            hex.field?.building = null


                            placedBuilding = null

                            selectedBuildingIndex = i

                            player.newResult.buildings = evaluateBuildings(player.grid)

                            val minus =
                                if (placedField?.second != selectedBuildingIndex && selectedBuildingIndex != null) -1 else 0
                            player.newResult.money = player.realResult.money + minus

                        } ?: placedField?.let { (hex, i) ->
                            availableFields[i].field = hex.field
                            hex.field = null
                            placedField = null

                            selectedHexagonIndex = i
                            selectedField = availableFields[i].field?.copy()

                            selectedBuildingIndex = null

                            player.newResult.regions = evaluateRegions(player.grid)
                        }
                    }

                    KeyEvent.VK_J -> Client.send(CMessage.Overpopulation)

                }
            }
        })

        isFocusable = true
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g as Graphics2D)

        if (width < 2)
            return

        g.font = Font("arial", Font.PLAIN, 20)

        g.color = Color(20, 20, 20)
        g.fillRect(0, 0, 325, height)
        g.fillRect(width - 325, 0, 325, height)

        //g.clip = Rectangle(0, 0, width, Hexagon.radius * 4)
        //g.drawImage(skyline, 0, 0, width, (width * skyline.height) / skyline.width, null)

        var textY = 0

        fun drawString(string: String) {
            g.drawString(string, 50, 50 + textY++ * 25)
        }

        g.color = Color.white

        if (currentPlayer !== player)
            drawString("Watching ${currentPlayer.name}")

        drawString("Round $round / ${Client.rounds}")

        if (currentPlayersTurn == ".")
            drawString("THE END")
        else
            drawString("${currentPlayersTurn?.plus("'s") ?: "Your"} turn")

        drawString("$${currentPlayer.realResult.money}")

        drawString("")
        drawString("C: Restaurant")
        drawString("V: ShoppingCenter")
        drawString("B: Factory")
        drawString("N: Office")
        drawString("M: TrainStation")

        drawString("")
        drawString("1: You")
        CPlayer.players.forEachIndexed { i, p ->
            drawString("${i + 2}: $p")
        }

        if (Test.testBuilding != null) {
            Test.drawTest(g)
            return
        }

        if (currentPlayersTurn == null) {
            for (building in Building.entries) {
                if (availableBuildings.count { it == building } == 3) {
                    g.color = Color.white
                    drawString("Improve diversity [J]")
                    break
                }
            }
        }

        drawString("")
        hoveredRegion?.let {
            drawString("Region: ${it.name}")
        }
        hoveredBuilding?.let {
            drawString("Building: ${it.name}")
        }
        hovered?.field?.let {
            drawString("Allowed buildings:")
            it.availableBuildings.forEach { building ->
                drawString(" - ${building.name}")
            }
        }

        drawResult(
            g,
            currentPlayer.realResult,
            currentPlayer.newResult.takeIf { currentPlayer === player },
            25,
            height - (Building.entries.size + Region.entries.size + 5) * 25
        )

        currentPlayer.grid.flatten().forEach { hex ->
            drawTile(g, hex, hex.field != null
                    || hex.neighbors.any {
                it?.field != null && placedField?.first != it
            })
        }

        hovered?.let { hovered ->
            if (
                selectedField != null
                && hovered.field == null
                && hovered.neighbors.any {
                    it?.field != null && placedField?.first != it
                }
            ) {
                drawTile(g, Tile(hovered.x, hovered.y, field = selectedField), true)
            } else if (selectedBuildingIndex != null
                && hovered.field != null
                && hovered.field?.building == null
                && availableBuildings[selectedBuildingIndex!!] in hovered.field!!.availableBuildings
            ) {
                drawBuilding(
                    g,
                    availableBuildings[selectedBuildingIndex!!]!!,
                    hovered.rx, hovered.ry
                )
            }
            g.color = Color(255, 255, 255, 50)
            g.fillPolygon(hovered.polygon)
        }

        availableFields.forEach {
            drawTile(g, it, true)
        }
        availableBuildings.forEachIndexed { i, building ->
            val (bounds, _) = availableBuildingsBounds[i]

            if (building == null) {
                g.color = Color(30, 30, 30)
                g.fillOval(bounds.x, bounds.y, bounds.width, bounds.height)
                g.color = Color.black
                g.drawOval(bounds.x, bounds.y, bounds.width, bounds.height)
            } else drawBuilding(g, building, bounds.x + Tile.RADIUS / 2, bounds.y + Tile.RADIUS / 2)
        }

        if (selectedField != null) {
            g.color = Color.green
            g.stroke = BasicStroke(4f)
            g.drawPolygon(availableFields[selectedHexagonIndex!!].polygon)
            g.stroke = BasicStroke(1f)
        }
        if (selectedBuildingIndex != null) {
            val (bounds, _) = availableBuildingsBounds[selectedBuildingIndex!!]
            g.color = Color.green
            g.stroke = BasicStroke(3f)
            g.drawOval(bounds.x, bounds.y, bounds.width, bounds.height)
            g.stroke = BasicStroke(1f)
        }

        if (debugOverlay)
            drawDebugOverlay(g, currentPlayer.newResult.buildings?.second)

        drawNotifications(g, this)
    }
}