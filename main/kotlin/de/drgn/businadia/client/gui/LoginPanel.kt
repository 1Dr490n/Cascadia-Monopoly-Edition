package de.drgn.businadia.client.gui

import de.drgn.businadia.*
import de.drgn.businadia.Button
import de.drgn.businadia.TextField
import de.drgn.businadia.client.Tile
import de.drgn.businadia.client.Client
import java.awt.*
import java.net.ConnectException
import java.net.UnknownHostException
import javax.swing.JComponent
import javax.swing.JPanel
import kotlin.concurrent.thread
import kotlin.math.roundToInt

object LoginPanel : JPanel() {

    private var waiting = false

    val nameField = TextField("Name")
    val hostField = TextField("Host")
    val portField = TextField("Port")
    val playButton = Button("Play") {
        if (waiting)
            return@Button

        waiting = true

        text = "Connecting..."
        thread {
            try {
                nameField.text = nameField.text.trim()
                if (nameField.text.trim().length < 2)
                    Client.notification("Your name must be at least two characters long.")
                else
                    Client.start(hostField.text, portField.text.toInt(), nameField.text)
            } catch (e: NumberFormatException) {
                Client.notification("Cannot parse port.")
            } catch (e: UnknownHostException) {
                Client.notification("Unknown host.")
            } catch (e: IllegalArgumentException) {
                Client.notification("Invalid port.")
            } catch (e: SMessage.LoginError.Exception) {
                Client.notification(e.message!!)
            } catch (e: ConnectException) {
                Client.notification("Connection failed.")
            } finally {
                text = "Play"
                waiting = false
            }
        }
    }

    private val components = listOf<JComponent>(nameField, hostField, portField, playButton)

    private val grid by lazy {
        val w = width / Tile.width + 2
        val h = height / Tile.height * 2
        List(w) { x ->
            List(h) { y ->
                val region = Region.entries.random()
                Tile(x - w / 4, y - 2, field = Field(region, region, 0, emptySet()), panel = LoginPanel)
            }
        }
    }

    init {
        background = Color.black

        components.forEach(::add)

        thread {
            while (!GamePanel.isAncestorOf(ClientFrame))
                repaint()
        }
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g as Graphics2D)

        if (width < 2)
            return

        grid.forEachPositioned { _, tile ->
            drawTile(g, tile, false)
        }

        g.color = Color(0, 0, 0, 30)
        g.fillRect(0, 0, width, height)

        components.forEachIndexed { i, it ->
            it.bounds = Rectangle(width / 2 - 250, height / 2 + i * 50, 500, 40)
        }

        drawNotifications(g, this)


        fun generateColor(f: Float): Color {
            val rg = (1 - f * (f - 0.7f) * 1.7f).coerceIn(0f, 1f).times(255).toInt()
            return Color(rg, rg, 255 - (f * 20).roundToInt())
        }

        g.font = Font("", Font.BOLD, 150)
        g.paint = generateGradient(
            0f,
            (height / 4).toFloat() - g.fontMetrics.height.toFloat(),
            0f,
            (height / 4).toFloat(),
            30,
            ::generateColor
        )
        g.drawString(MAIN_TITLE, width / 2 - g.fontMetrics.stringWidth(MAIN_TITLE) / 2, height / 4)

        g.font = Font("", Font.BOLD, 40)
        g.paint = generateGradient(
            0f,
            (height * 3 / 10).toFloat() - g.fontMetrics.height.toFloat(),
            0f,
            (height * 3 / 10).toFloat(),
            11,
            ::generateColor
        )
        g.drawString(EDITION_TITLE, width / 2 - g.fontMetrics.stringWidth(EDITION_TITLE) / 2, height * 3 / 10)
    }
}

