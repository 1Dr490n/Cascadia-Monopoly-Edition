package de.drgn.businadia.client.gui

import de.drgn.businadia.GAME_TITLE
import javax.swing.JFrame

object ClientFrame : JFrame() {
	init {
		add(LoginPanel)
		setLocation(2000, 100)
		extendedState = MAXIMIZED_BOTH
		isUndecorated = true
		defaultCloseOperation = DO_NOTHING_ON_CLOSE
		title = GAME_TITLE
		isVisible = true
	}
}