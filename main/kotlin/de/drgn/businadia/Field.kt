package de.drgn.businadia

import com.fasterxml.jackson.annotation.JsonIgnore

data class Field(val region1: Region, val region2: Region, var rotation: Int, val availableBuildings: Set<Building>) {
    var building: Building? = null

    @get:JsonIgnore
    val edgeRegions: List<Region>
        get() {
            val rotations = List(3) { (rotation + it) % 6 }

            return List(6) {
                if (it in rotations)
                    region1
                else region2
            }
        }
}