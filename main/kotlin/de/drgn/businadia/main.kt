package de.drgn.businadia

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import de.drgn.businadia.client.gui.ClientFrame
import de.drgn.businadia.client.gui.LoginPanel
import de.drgn.businadia.server.Server
import java.io.File

fun readInteger(name: String): Int {
	print("$name: ")
	var value: Int?
	do {
		value = readln().toIntOrNull()
	} while (value == null)
	return value
}

fun main(args: Array<String>) {
	if(args.firstOrNull() == "server") {
		val rounds = args.getOrNull(1)?.toIntOrNull() ?: readInteger("Rounds")
		val save = args.getOrNull(3)?.let {
			jacksonObjectMapper().readValue<Server.GameState>(File(it))
		}
		Server.start(args.getOrNull(2)?.toIntOrNull(), rounds, save)
	} else {
		LoginPanel.hostField.text = args.getOrNull(0) ?: ""
		LoginPanel.portField.text = args.getOrNull(1) ?: ""
		LoginPanel.nameField.text = args.getOrNull(2) ?: ""
		ClientFrame
	}
}