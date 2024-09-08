package de.drgn.businadia.client.gui

import de.drgn.businadia.*
import de.drgn.businadia.client.Tile
import java.awt.Color
import java.awt.Graphics2D

object Test {

	private lateinit var result: BuildingResult

	var testBuilding: Building? = null

	private var grid = List(gridSize) { x ->
		List(gridSize) { y ->
			Tile(x, y)
		}
	}

	fun testBuilding(building: Building) {
		grid.forEach {
			it.forEach {
				it.field = null
			}
		}

		building.testFields.forEach { (x, y), b ->
			grid[x][y].field = Field(Region.Park, Region.Park, if(b.allowed) 0 else 1, emptySet())
			grid[x][y].field!!.building = b.building
		}

		result = evaluateBuildings(grid.map { it.map { if(it.field?.rotation?.let { it % 2 == 0 } != true) Tile(it.x, it.y) else it } })

		testBuilding = building
	}

	fun drawTest(g: Graphics2D) {

		grid.flatten().forEach { hex ->
			drawTile(g, hex, hex.field != null
					|| hex.neighbors(grid).any {
				it?.field != null && GamePanel.placedField?.first != it
			}, test = true
			)
		}

		drawDebugOverlay(g, result.second)

		val pointsY = GamePanel.height - (Building.entries.size + 4 + testBuilding!!.points.size) * 25
		g.color = Color.white
		testBuilding!!.explanation2?.let {
			g.drawString(it, 15, pointsY - 30)
		}
		g.drawString(testBuilding!!.explanation, 15, pointsY - 5)
		testBuilding!!.points.forEachIndexed { i, it ->
			drawPoints(g, if(i == testBuilding!!.points.size - 1 && !testBuilding!!.hasMaximum) "${i + 1}+" else (i + 1).toString(), it, null, 25, pointsY, i)
		}

		drawPointsTable(
			g,
			Building.entries,
			result.first,
			null,
			25,
			GamePanel.height - (Building.entries.size + 2) * 25,
			0
		)
	}
}