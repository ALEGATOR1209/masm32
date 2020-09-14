package converters

import model.AbstractCommand

interface Dictionary {
    val commands: List<AbstractCommand>
    infix fun getCommandsWithName(name: String) = commands.filter { it.name.equals(name, true) }
}
