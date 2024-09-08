package de.drgn.businadia.client

import de.drgn.businadia.Field
import de.drgn.businadia.client.gui.GamePanel
import de.drgn.businadia.gridSize
import java.awt.Polygon
import javax.swing.JPanel
import kotlin.math.sqrt

class Tile(
	val x: Int,
	val y: Int,
	_rx: Int? = null,
	_ry: Int? = null,
	var field: Field? = null,
	val panel: JPanel = GamePanel
) {

	private val points = listOf(
		width / -2 to height / 4,
		width / -2 to height / -4,
		0 to height / -2,
		width / 2 to height / -4,
		width / 2 to height / 4,
		0 to height / 2
	)

	val polygon by lazy {
		val polygon = Polygon()
		points.forEach {
			polygon.addPoint(it.first + rx, it.second + ry)
		}
		polygon
	}

	val rx by lazy {
		_rx ?: ((panel.width / 2 + ((x - gridSize / 2) + y % 2 / 2.0) * width).toInt() - RADIUS / 2)
	}
	val ry by lazy {
		_ry ?: (panel.height / 2 + ((y - gridSize / 2)) * height * 3 / 4)
	}

	companion object {
		const val RADIUS = 64

		val neighborsEven = listOf(-1 to 0, -1 to -1, 0 to -1, 1 to 0, 0 to 1, -1 to 1)
		val neighborsOdd = listOf(-1 to 0, 0 to -1, 1 to -1, 1 to 0, 1 to 1, 0 to 1)

		val width = (sqrt(3.0) * RADIUS).toInt()
		val height = 2 * RADIUS
	}

	val neighbors by lazy {
		neighbors(Client.currentPlayer.grid)
	}

	fun neighbors(grid: List<List<Tile>>): List<Tile?> {
		return (if (y % 2 == 0) neighborsEven else neighborsOdd).map { (x, y) ->
			grid.getOrNull(this.x + x)?.getOrNull(this.y + y)
		}
	}
}