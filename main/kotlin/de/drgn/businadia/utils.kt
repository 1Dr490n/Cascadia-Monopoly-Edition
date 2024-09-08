package de.drgn.businadia

import com.fasterxml.jackson.annotation.JsonTypeInfo
import de.drgn.businadia.client.Tile
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.image.BufferedImage
import java.io.FileNotFoundException
import java.io.InputStream
import javax.imageio.ImageIO
import javax.swing.JButton
import javax.swing.JTextField
import kotlin.math.min

const val MAIN_TITLE = "CASCADIA"
const val EDITION_TITLE = "Monopoly Edition"
const val GAME_TITLE = "$MAIN_TITLE: $EDITION_TITLE"

fun loadImage(path: String): BufferedImage = try {
	ImageIO.read(loadFile("images/$path"))
} catch (e: IllegalArgumentException) {
	throw FileNotFoundException(path)
}
fun loadFile(path: String): InputStream = try {
	object {}.javaClass.getResourceAsStream("/files/${path.lowercase()}")
} catch (e: IllegalArgumentException) {
	throw FileNotFoundException(path)
}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
sealed class SMessage {
	data class Field(val x: Int, val y: Int, val field: de.drgn.businadia.Field) : SMessage()
	data class Available(val fields: List<de.drgn.businadia.Field>?, val buildings: List<Building>) : SMessage()
	data class Turn(val player: String, val isYou: Boolean) : SMessage()
	data class Init(val players: List<String>, val rounds: Int) : SMessage()
	data class ViewPlayer(
		val name: String,
		val fields: List<Triple<Int, Int, de.drgn.businadia.Field>>,
		val money: Int
	) : SMessage()

	data class OthersTurn(
		val player: String,
		val field: CMessage.Turn.Field,
		val building: CMessage.Turn.Building?,
		val money: Int
	) : SMessage()

	data object End : SMessage()
	data class LoginError(val message: String) : SMessage() {
		class Exception(message: String) : kotlin.Exception(message)
	}
	data class Loaded(val fields: List<Triple<Int, Int, de.drgn.businadia.Field>>, val money: Int, val round: Int) : SMessage()
	data object DiversityImproved : SMessage()
}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
sealed class CMessage {
	data class Turn(val field: Field, val building: Building?, val money: Int) : CMessage() {
		data class Field(val x: Int, val y: Int, val rotation: Int, val index: Int)
		data class Building(val x: Int, val y: Int, val index: Int)
	}

	data class ViewPlayer(val player: String) : CMessage()
	data object Overpopulation : CMessage()
}

const val gridSize = 9

enum class Region(val a1: Building, val a2: Building, val b: Building) {
	Park(Building.Restaurant, Building.ShoppingCenter, Building.TrainStation),
	Downtown(Building.ShoppingCenter, Building.Office, Building.Restaurant),
	Industry(Building.Factory, Building.TrainStation, Building.Office),
	Commercial(Building.Office, Building.Factory, Building.TrainStation),
	Neighborhood(Building.TrainStation, Building.Restaurant, Building.Factory);

	val image by lazy {
		loadImage("regions/$name.jpg")
	}
}

data class TestBuilding(val building: Building, val allowed: Boolean = true)

enum class Building(val color: Color, val points: List<Int>, val hasMaximum: Boolean, val explanation: String, val explanation2: String? = null) {
	Restaurant(Color(85, 36, 4), listOf(2, 4, 7, 11, 15, 18, 22), false, "Restaurant chain length") {
		override val testFields by lazy {
			mapOf(
				(2 to 3) to TestBuilding(Restaurant),
				(3 to 4) to TestBuilding(Restaurant),
				(4 to 4) to TestBuilding(Restaurant),
				(4 to 5) to TestBuilding(Restaurant),

				(2 to 2) to TestBuilding(Restaurant, false),
				(3 to 2) to TestBuilding(Restaurant, false),
				(1 to 3) to TestBuilding(Restaurant, false),
				(3 to 3) to TestBuilding(Restaurant, false),
				(4 to 3) to TestBuilding(Restaurant, false),
				(2 to 4) to TestBuilding(Restaurant, false),
				(5 to 4) to TestBuilding(Restaurant, false),
				(2 to 5) to TestBuilding(Restaurant, false),
				(3 to 5) to TestBuilding(Restaurant, false),
				(5 to 5) to TestBuilding(Restaurant, false),
				(4 to 6) to TestBuilding(Restaurant, false),
				(5 to 6) to TestBuilding(Restaurant, false),

				)
		}
	},
	ShoppingCenter(Color.white, listOf(2, 3, 5, 8, 12), true, "Neighboring building types") {
		override val testFields by lazy {
			mapOf(
				(4 to 4) to TestBuilding(ShoppingCenter),
				(3 to 4) to TestBuilding(Restaurant),
				(3 to 3) to TestBuilding(Restaurant),
				(4 to 3) to TestBuilding(Factory),
				(5 to 4) to TestBuilding(ShoppingCenter),
				(4 to 5) to TestBuilding(TrainStation),
				(3 to 5) to TestBuilding(Office),
			)
		}
	},
	Factory(Color(0x881805), listOf(2, 4, 7, 10, 13), false, "Factory cluster size") {
		override val testFields by lazy {
			mapOf(
				(3 to 3) to TestBuilding(Factory),
				(4 to 3) to TestBuilding(Factory),
				(3 to 4) to TestBuilding(Factory),
				(4 to 4) to TestBuilding(Factory),
				(4 to 5) to TestBuilding(Factory),
			)
		}
	},
	Office(Color(0x55B4B7), listOf(2, 4, 7, 10, 14), false, "Neighboring restaurants") {
		override val testFields by lazy {
			mapOf(
				(4 to 4) to TestBuilding(Office),
				(3 to 3) to TestBuilding(Restaurant),
				(3 to 4) to TestBuilding(Restaurant),
				(3 to 5) to TestBuilding(Restaurant),
				(4 to 5) to TestBuilding(Restaurant),
			)
		}
	},
	TrainStation(Color(40, 40, 40), emptyList(), false, "3 / line", "Straight lines between train stations") {
		override val testFields by lazy {
			mapOf(
				(4 to 3) to TestBuilding(TrainStation),
				(3 to 5) to TestBuilding(TrainStation),
				(5 to 5) to TestBuilding(TrainStation),
				(4 to 4) to TestBuilding(TrainStation, false),
				(5 to 4) to TestBuilding(TrainStation, false),
				(4 to 5) to TestBuilding(TrainStation, false),

				(2 to 2) to TestBuilding(TrainStation),
				(2 to 3) to TestBuilding(TrainStation, false),
				(3 to 4) to TestBuilding(TrainStation, false),
			)
		}
	};

	abstract val testFields: Map<Pair<Int, Int>, TestBuilding>

	val image by lazy {
		loadImage("buildings/$name.png")
	}
}

operator fun <T> List<T>.times(n: Int) = List(n) { this }.flatten()

fun Polygon.subPoly(rotation: Int, n: Int): Polygon {
	val res = Polygon()
	for (i in rotation..<rotation + n) {
		res.addPoint(xpoints[i % npoints], ypoints[i % npoints])
	}
	return res
}

fun <T> List<List<T>>.forEachPositioned(action: (Pair<Int, Int>, T) -> Unit) {
	forEachIndexed { x, ts ->
		ts.forEachIndexed { y, t ->
			action(x to y, t)
		}
	}
}

fun Int.toSignedString() = (if (this > 0) "+" else "") + this

typealias BuildingResult = Pair<Map<Building, Int>, Map<Building, List<List<Tile>>>>

fun evaluateBuildings(grid: List<List<Tile>>): BuildingResult {
	val buildings = mutableMapOf<Building, Int>()

	val points = mutableMapOf<Building, List<List<Tile>>>()

	buildings[Building.Restaurant] = run {
		var score = 0

		val handledTiles = mutableListOf<Tile>()
		val newPoints = mutableListOf<List<Tile>>()

		grid.forEachPositioned { _, tile ->

			val chain = mutableListOf<Tile>()

			fun handle(tile: Tile) {

				val neighbors = tile.neighbors(grid).filterNotNull()
					.filter { it.field?.building == Building.Restaurant && it !in handledTiles }

				if (neighbors.size > 1) {
					return
				}

				handledTiles += tile
				chain += tile

				if (neighbors.isEmpty()) {
					score += Building.Restaurant.points[min(Building.Restaurant.points.size - 1, chain.size - 1)]
					newPoints += chain
					return
				}

				handle(neighbors.single())
			}

			if (tile.field?.building == Building.Restaurant && tile !in handledTiles) {
				handle(tile)
			}
		}

		points[Building.Restaurant] = newPoints
		score
	}

	buildings[Building.ShoppingCenter] = run {

		var score = 0
		val newPoints = mutableListOf<List<Tile>>()

		grid.forEachPositioned { _, tile ->
			if (tile.field?.building == Building.ShoppingCenter) {
				val neighbors = tile.neighbors(grid).distinctBy { it?.field?.building }.filter { it?.field?.building != null }

				if (neighbors.isNotEmpty()) {
					score += Building.ShoppingCenter.points[neighbors.size - 1]
					newPoints += neighbors.map {
						listOf(tile, it!!)
					}
				}
			}
		}

		points[Building.ShoppingCenter] = newPoints
		score
	}

	buildings[Building.Factory] = run {
		var score = 0

		val handledTiles = mutableSetOf<Tile>()
		val newPoints = mutableListOf<List<Tile>>()

		grid.forEachPositioned { _, tile ->

			var cluster = 0

			fun handle(tile: Tile) {

				if (tile !in handledTiles)
					cluster++
				else return

				val neighbors = tile.neighbors(grid).filterNotNull()
					.filter { it.field?.building == Building.Factory && it !in handledTiles }

				neighbors.forEach {
					newPoints += listOf(tile, it)
				}

				handledTiles += tile

				neighbors.forEach {
					handle(it)
				}
			}

			if (tile.field?.building == Building.Factory && tile !in handledTiles) {
				handle(tile)

				score += Building.Factory.points[min(Building.Factory.points.size - 1, cluster - 1)]
			}
		}

		points[Building.Factory] = newPoints
		score
	}

	buildings[Building.Office] = run {

		var score = 0
		val newPoints = mutableListOf<List<Tile>>()

		grid.forEachPositioned { (x, y), tile ->
			if (tile.field?.building == Building.Office) {
				val neighbors = grid[x][y].neighbors(grid).filter { it?.field?.building == Building.Restaurant }
				neighbors.forEach {
					newPoints += listOf(tile, it!!)
				}
				if (neighbors.isNotEmpty())
					score += Building.Office.points[min(Building.Office.points.size - 1, neighbors.size - 1)]
			}
		}

		points[Building.Office] = newPoints

		score
	}

	buildings[Building.TrainStation] = run {
		var lines = 0

		val handled = mutableListOf<Pair<Tile, Tile>>()

		grid.forEachPositioned { _, tile ->
			if (tile.field?.building == Building.TrainStation) {
				for (i in 0..<6) {
					var hex = tile
					var depth = 0
					while (true) {
						hex = hex.neighbors(grid)[i] ?: break

						if (hex.field?.building == Building.TrainStation) {

							if (depth > 0 && handled.none { tile in it.toList() && hex in it.toList() }) {
								lines++

								handled += tile to hex
							}

							break
						}

						depth++
					}
				}
			}
		}

		points[Building.TrainStation] = handled.map {
			it.toList()
		}

		lines * 3
	}

	return buildings to points
}

fun evaluateRegions(grid: List<List<Tile>>) = Region.entries.associateWith { region ->
	val clusters = mutableListOf<Int>()

	val handledTiles = mutableSetOf<Tile>()

	grid.forEachPositioned { _, tile ->

		var cluster = 0

		fun handle(tile: Tile) {

			if (tile in handledTiles)
				return

			cluster++

			val edges = tile.field!!.edgeRegions

			handledTiles += tile

			tile.neighbors(grid).forEachIndexed { i, neighbor ->
				if (
					neighbor?.field?.edgeRegions?.get((i + 3) % 6) == region
					&& neighbor !in handledTiles
					&& edges[i] == region
				) {
					handle(neighbor)
				}
			}
		}

		if ((tile.field?.region1 == region || tile.field?.region2 == region) && tile !in handledTiles) {
			handle(tile)

			clusters += cluster
		}
	}

	clusters.max()
}

data class Result(var buildings: BuildingResult?, var regions: Map<Region, Int>?, var money: Int)

class TextField(private val hint: String) : JTextField() {
	init {
		foreground = Color.white
		isOpaque = false
		background = Color(0, 0, 0, 0)
		caretColor = Color.white
		selectionColor = Color.white
	}

	override fun paintBorder(g: Graphics?) {

	}

	override fun paint(g: Graphics) {
		g.color = Color(0, 0, 0, 150)
		g.fillRect(0, 0, width, height)

		super.paint(g as Graphics2D)
		if (text.isEmpty()) {
			val h = height
			g.setRenderingHint(
				RenderingHints.KEY_TEXT_ANTIALIASING,
				RenderingHints.VALUE_TEXT_ANTIALIAS_ON
			)
			g.color = Color.lightGray
			g.drawString(hint, insets.left, h / 2 + g.fontMetrics.ascent / 2 - 2)
		}
	}
}

class Button(text: String, onClick: (Button.() -> Unit)? = null) : JButton(text) {

	var isRollover = false

	init {
		onClick?.let {
			addActionListener {
				onClick()
			}
		}
	    isBorderPainted = false

		addMouseListener(object : MouseAdapter() {
			override fun mouseEntered(e: MouseEvent?) {
				super.mouseEntered(e)

				isRollover = true
				repaint()
			}

			override fun mouseExited(e: MouseEvent?) {
				super.mouseExited(e)

				isRollover = false
				repaint()
			}
		})
	}

	override fun paintComponent(g: Graphics) {

		g.color = when {
			model.isPressed -> Color(0, 0, 0, 205)
			isRollover -> Color(0, 0, 0, 170)
			else -> Color(0, 0, 0, 200)
		}
		g.fillRect(0, 0, width, height)

		val stringWidth = g.fontMetrics.stringWidth(text)

		g.color = Color.white
		g.drawString(text, width / 2 - stringWidth / 2, height / 2 - g.fontMetrics.height / 2 + g.fontMetrics.ascent)

		if(isFocusOwner) {
			g.color = Color.lightGray
			g.drawRect(width / 2 - stringWidth / 2 - 2, height / 2 - g.fontMetrics.height / 2, stringWidth + 4, g.fontMetrics.height)
		}
	}
}

operator fun<T> Map<Point, T>.get(x: Int, y: Int) = get(Point(x, y))
operator fun<T> MutableMap<Point, T>.set(x: Int, y: Int, t: T) = set(Point(x, y), t)