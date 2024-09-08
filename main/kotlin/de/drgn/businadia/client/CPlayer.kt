package de.drgn.businadia.client

import de.drgn.businadia.Region
import de.drgn.businadia.Result
import de.drgn.businadia.gridSize

class CPlayer(val name: String) {

	var newResult = Result(null, Region.entries.associateWith { 1 }, 0)
	var realResult = Result(null, Region.entries.associateWith { 1 }, 0)

	var grid = List(gridSize) { x ->
		List(gridSize) { y ->
			Tile(x, y)
		}
	}

	companion object {
		var players = listOf<String>()
	}
}