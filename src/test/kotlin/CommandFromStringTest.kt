import converters.Dictionary
import converters.Masm32Converter
import model.*
import org.junit.Assert
import org.junit.Test

class CommandFromStringTest {
    private val converter = Masm32Converter(DICTIONARY)

    @Test
    fun movRegRegTest() {
        val commands = mapOf(
                "MOV EAX, EBX" to listOf("8BC3", "89C3"),
                "MOV EBX, ECX" to listOf("8BD9", "89D9"),
                "MOV AL, AH" to listOf("88C4", "8AC4")
        )
        testCommands(commands)
    }

    @Test
    fun movRegConstTest() {
        val commands = mapOf(
                "MOV EAX, 1" to listOf("B801000000"),
                "MOV EBX, 10" to listOf("BB10000000"),
                "MOV AL, 12" to listOf("B012")
        )
        testCommands(commands)
    }

    @Test
    fun movRegMemTest() {
        val commands = mapOf(
                "MOV EAX, [00112233]" to listOf("8B0533221100"),
                "MOV EBX, [ECX]" to listOf("8B19"),
                "MOV ECX, [EAX * 4]" to listOf("8B0C8500000000"),
                "MOV EAX, [00112233][ECX]" to listOf("8B8133221100"),
                "MOV EAX, [00112233][ECX*4]" to listOf("8B048D33221100"),
                "MOV EAX, [ECX][EBX]" to listOf("8B040B", "8B0419"),
                "MOV EAX, [00112233][ECX][EBX*4]" to listOf("8B849933221100"),
                "MOV EAX, [00112233][EBX*4][ECX]" to listOf("8B849933221100"),
                "MOV EAX, [EBX*4][00112233][ECX]" to listOf("8B849933221100"),
                "MOV EAX, [EBX*4][ECX][00112233]" to listOf("8B849933221100")
        )

        testCommands(commands)
    }

    @Test
    fun movMemDestTest() {
        val commands = mapOf(
                "MOV [11223344], ECX" to listOf("890D44332211"),
                "MOV [EBX], ECX" to listOf("890B"),
                "MOV [EBX*4], ECX" to listOf("890C9D00000000"),
                "MOV [11223344][EBX], ECX" to listOf("898B44332211"),
                "MOV [11223344][EBX*4], ECX" to listOf("890C9D44332211"),
                "MOV [11223344][EBX*4][EDX], ECX" to listOf("898C9A44332211"),
                "MOV [11223344][EDX][EBX*4], ECX" to listOf("898C9A44332211"),
                "MOV [EDX][11223344][EBX*4], ECX" to listOf("898C9A44332211"),
                "MOV [EDX][11223344][EBX*4], 80" to listOf("C7849A4433221180000000"),
                "MOV [11223344], 10" to listOf("C7054433221110000000")
        )

        testCommands(commands)
    }

    @Test
    fun incTest() {
        val commands = mapOf("INC EAX" to listOf("40"))
        testCommands(commands)
    }

    private fun testCommands(commands: Map<String, List<String>>) = commands.forEach { (command, codes) ->
        val encoded = converter.commandFromString(command)
        val names = encoded.map { it.toCommandString().toUpperCase() }
        val binaries = encoded.map { it.toCodeString().toUpperCase() }
        println("$command: $binaries")
        Assert.assertTrue(command, command.toUpperCase() in names)
        binaries.forEach { Assert.assertTrue(command, it in codes) }
    }
}
