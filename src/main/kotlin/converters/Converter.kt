package converters

import model.Command
import java.math.BigInteger

interface Converter {
    fun commandFromString(string: String): List<Command>
    fun commandFromCode(code: BigInteger): List<Command>
}
