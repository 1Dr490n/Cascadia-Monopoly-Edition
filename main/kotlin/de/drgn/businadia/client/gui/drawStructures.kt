package de.drgn.businadia.client.gui

import de.drgn.businadia.*
import de.drgn.businadia.client.Client.notifications
import de.drgn.businadia.client.Tile
import java.awt.*
import javax.swing.JPanel
import kotlin.math.ceil
import kotlin.math.sqrt

fun drawTile(g: Graphics2D, tile: Tile, border: Boolean, test: Boolean = false) {
    if (tile.field === null) {
        g.color = if (border) Color(30, 30, 30) else Color.black
        g.fillPolygon(tile.polygon)

        g.color = Color(15, 15, 15)
        if (!border)
            g.stroke = BasicStroke(2f)
        g.drawPolygon(tile.polygon)
        g.stroke = BasicStroke(1f)
    } else {
        if (test) {
            g.clip = tile.polygon
            g.color = tile.field!!.building!!.color
            g.fillRect(
                tile.rx - Tile.RADIUS,
                tile.ry - Tile.RADIUS,
                Tile.RADIUS * 2,
                Tile.RADIUS * 2,
            )
            g.clip = null
        } else {
            g.clip = tile.polygon.subPoly(tile.field!!.rotation, 4)
            g.drawImage(
                tile.field!!.region1.image,
                tile.rx - Tile.RADIUS,
                tile.ry - Tile.RADIUS,
                Tile.RADIUS * 2,
                Tile.RADIUS * 2,
                null
            )

            g.clip = tile.polygon.subPoly(tile.field!!.rotation + 3, 4)
            g.drawImage(
                tile.field!!.region2.image,
                tile.rx - Tile.RADIUS,
                tile.ry - Tile.RADIUS,
                Tile.RADIUS * 2,
                Tile.RADIUS * 2,
                null
            )
            g.clip = null
        }

        tile.field!!.building?.let {
            drawBuilding(g, it, tile.rx, tile.ry)
        } ?: tile.field!!.availableBuildings.sorted().forEachIndexed { i, building ->
            g.color = building.color

            g.fillRect(
                tile.rx - Tile.RADIUS / 4 + i * Tile.RADIUS / 2 / tile.field!!.availableBuildings.size,
                tile.ry - Tile.RADIUS / 4,
                ceil(Tile.RADIUS / 2.0 / tile.field!!.availableBuildings.size).toInt(),
                Tile.RADIUS / 2
            )
        }

        if (test) {
            if (tile.field!!.rotation % 2 != 0) {
                g.color = Color(0, 0, 0, 100)
                g.fillPolygon(tile.polygon)
            }
            g.color = Color.black
            g.drawPolygon(tile.polygon)
        }
    }
}

fun drawBuilding(g: Graphics2D, building: Building, x: Int, y: Int) {
    g.color = building.color
    g.fillOval(x - Tile.RADIUS / 2, y - Tile.RADIUS / 2, Tile.RADIUS, Tile.RADIUS)

    val s = (Tile.RADIUS / 2 * sqrt(2.0)).toInt()

    g.drawImage(
        building.image,
        x - s / 2,
        y - s / 2,
        s,
        s,
        null
    )
}

fun drawPoints(g: Graphics, name: String?, real: Int, new: Int?, x: Int, y: Int, i: Int): Int {
    if (name != null) {
        if (i % 2 == 0) {
            g.color = Color(75, 75, 75)
            g.fillRect(x, y + i * 25, 275, 25)
        }
        g.color = Color.white
        g.drawString(name, x + 5, y + 20 + i * 25)
    } else {
        g.color = Color(100, 100, 100)
        g.fillRect(x + 195, y + i * 25, 80, 25)
    }

    g.color = Color.white
    g.drawString(real.toString(), x + 200, y + 20 + i * 25)

    if (new != null) {
        val difference = new - real
        if (difference != 0) {
            g.color = if (difference > 0) Color.green else Color.red
            g.drawString(difference.toSignedString(), x + 235, y + 20 + i * 25)
        }
    }
    return i + 1
}

fun <T> drawPointsTable(
    g: Graphics,
    names: List<T>,
    real: Map<T, Int>?,
    new: Map<T, Int>?,
    x: Int,
    y: Int,
    i: Int
): Int {
    var i = i
    names.forEach { b ->
        i = drawPoints(
            g,
            b.toString(),
            real?.get(b) ?: 0,
            new?.get(b),
            x, y,
            i
        )
    }

    return drawPoints(
        g,
        null,
        real?.values?.sum() ?: 0,
        new?.values?.sum(),
        x, y,
        i
    )
}

fun drawResult(g: Graphics, realResult: Result?, newResult: Result?, x: Int, y: Int) {
    var i = drawPointsTable(g, Building.entries, realResult?.buildings?.first, newResult?.buildings?.first, x, y, 0)
    i = drawPointsTable(g, Region.entries, realResult?.regions, newResult?.regions, x, y, i)

    i = drawPoints(
        g,
        "Money",
        realResult?.money ?: 0,
        newResult?.money,
        x, y,
        i
    )

    drawPoints(
        g,
        null,
        realResult?.let { (it.buildings?.first?.values?.sum() ?: 0) + (it.regions?.values?.sum() ?: 0) + it.money }
            ?: 0,
        newResult?.let { (it.buildings?.first?.values?.sum() ?: 0) + (it.regions?.values?.sum() ?: 0) + it.money },
        x, y,
        i
    )
}

fun drawDebugOverlay(g: Graphics2D, buildings: Map<Building, List<List<Tile>>>?) {
    g.stroke = BasicStroke(3f)
    buildings?.forEach { (b, points) ->
        g.color = b.color.darker()
        points.forEach {
            g.drawPolyline(it.map { it.rx }.toIntArray(), it.map { it.ry }.toIntArray(), it.size)
        }
    }
    g.stroke = BasicStroke(1f)
}

fun drawNotifications(g: Graphics2D, panel: JPanel) {
    val notificationHeight = 30
    val notificationY = notifications.firstOrNull()?.let { (str, time) ->
        val difference = time + 1500 - System.currentTimeMillis()
        if (difference < -200) {
            notifications.removeIf {
                it.first == str && it.second == time
            }
            0
        } else if (difference < 0)
            (difference * notificationHeight / 200).toInt()
        else 0
    } ?: 0

    notifications.forEachIndexed { i, (str, _) ->

        val strWidth = g.fontMetrics.stringWidth(str)

        g.color = Color(0, 0, 0, 150)
        g.fillRect(
            panel.width / 2 - strWidth / 2 - 2,
            i * notificationHeight + notificationY,
            strWidth + 4,
            notificationHeight
        )

        g.color = Color.white
        g.drawString(
            str,
            panel.width / 2 - g.fontMetrics.stringWidth(str) / 2,
            i * notificationHeight + notificationY + notificationHeight / 2 - g.fontMetrics.height / 2 + g.fontMetrics.ascent
        )
    }
}

fun generateGradient(startX: Float, startY: Float, endX: Float, endY: Float, steps: Int, color: (Float) -> Color): LinearGradientPaint {
    val fractions = FloatArray(steps) { it.toFloat() / steps }
    val colors = Array(steps) {
        color(it / steps.toFloat())
    }

    return LinearGradientPaint(
        startX,
        startY,
        endX,
        endY,
        fractions,
        colors
    )
}