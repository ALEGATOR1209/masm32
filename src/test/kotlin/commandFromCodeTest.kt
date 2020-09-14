import converters.Masm32Converter
import model.toCodeString
import model.toCommandString
import org.junit.Assert
import org.junit.Test

class CommandFromCodeTest {
    private val converter = Masm32Converter(DICTIONARY)

    @Test
    fun movRegRegTest() {
        val commands = mapOf(
                "8BC3" to "MOV EAX, EBX",
                "8BD9" to "MOV EBX, ECX",
                "88C4" to "MOV AH, AL"
        )
        testCommands(commands)
    }

    @Test
    fun movRegConstTest() {
        val commands = mapOf(
                "B801000000" to "MOV EAX, 1",
                "BB10000000" to "MOV EBX, 10",
                "B012" to "MOV AL, 12"
        )
        testCommands(commands)
    }

    @Test
    fun movRegMemTest() {
        val commands = mapOf(
                "8B0533221100" to "MOV EAX, [00112233]",
                "8B19" to "MOV EBX, [ECX]",
                "8B0C8500000000" to "MOV ECX, [EAX*4]",
                "8B8133221100" to "MOV EAX, [ECX][00112233]",
                "8B048D33221100" to "MOV EAX, [ECX*4][00112233]",
                "8B040B" to "MOV EAX, [ECX][EBX]",
                "8B849933221100" to "MOV EAX, [EBX*4][ECX][00112233]"
        )

        testCommands(commands)
    }

    @Test
    fun movMemDestTest() {
        val commands = mapOf(
                "890D44332211" to "MOV [11223344], ECX",
                "890B" to "MOV [EBX], ECX",
                "890C9D00000000" to "MOV [EBX*4], ECX",
                "898B44332211" to "MOV [EBX][11223344], ECX",
                "890C9D44332211" to "MOV [EBX*4][11223344], ECX",
                "898C9A44332211" to "MOV [EBX*4][EDX][11223344], ECX",
                "C7849A4433221180000000" to "MOV [EBX*4][EDX][11223344], 80",
                "C7054433221110000000" to "MOV [11223344], 10"
        )

        testCommands(commands)
    }

    @Test
    fun incTest() {
        val commands = mapOf("40" to "INC EAX")
        testCommands(commands)
    }

    private fun testCommands(commands: Map<String, String>) = commands.forEach { (code, command) ->
        println(command)
        val decoded = converter.commandFromCode(code.toBigInteger(16))[0]
        val string = decoded.toCommandString()
        val decodedCode = decoded.toCodeString().toUpperCase()
        Assert.assertEquals(command, command, string)
        Assert.assertEquals(command, code, decodedCode)
    }
}