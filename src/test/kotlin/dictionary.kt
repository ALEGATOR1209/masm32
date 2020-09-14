import converters.Dictionary
import model.AbstractCommand
import model.AbstractOperand

object DICTIONARY: Dictionary {
    private const val MOV = "MOV"
    private const val INC = "INC"
    override val commands = listOf(
            AbstractCommand(MOV, 0x88, dest = AbstractOperand.Reg8, source = AbstractOperand.Reg8),
            AbstractCommand(MOV, 0x88, dest = AbstractOperand.Memory8, source = AbstractOperand.Reg8),
            AbstractCommand(MOV, 0x89, dest = AbstractOperand.Memory32, source = AbstractOperand.Reg32),
            AbstractCommand(MOV, 0x8A, dest = AbstractOperand.Reg8, source = AbstractOperand.Reg8),
            AbstractCommand(MOV, 0x8A, dest = AbstractOperand.Reg8, source = AbstractOperand.Memory8),
            AbstractCommand(MOV, 0x8B, dest = AbstractOperand.Reg32, source = AbstractOperand.Reg32),
            AbstractCommand(MOV, 0x8B, dest = AbstractOperand.Reg32, source = AbstractOperand.Memory32),
            AbstractCommand(MOV, 0xB0, dest = AbstractOperand.AL, source = AbstractOperand.Constant8),
            AbstractCommand(MOV, 0xB1, dest = AbstractOperand.CL, source = AbstractOperand.Constant8),
            AbstractCommand(MOV, 0xB2, dest = AbstractOperand.DL, source = AbstractOperand.Constant8),
            AbstractCommand(MOV, 0xB3, dest = AbstractOperand.BL, source = AbstractOperand.Constant8),
            AbstractCommand(MOV, 0xB4, dest = AbstractOperand.AH, source = AbstractOperand.Constant8),
            AbstractCommand(MOV, 0xB5, dest = AbstractOperand.CH, source = AbstractOperand.Constant8),
            AbstractCommand(MOV, 0xB6, dest = AbstractOperand.DH, source = AbstractOperand.Constant8),
            AbstractCommand(MOV, 0xB7, dest = AbstractOperand.BH, source = AbstractOperand.Constant8),
            AbstractCommand(MOV, 0xB8, dest = AbstractOperand.EAX, source = AbstractOperand.Constant32),
            AbstractCommand(MOV, 0xB9, dest = AbstractOperand.ECX, source = AbstractOperand.Constant32),
            AbstractCommand(MOV, 0xBA, dest = AbstractOperand.EDX, source = AbstractOperand.Constant32),
            AbstractCommand(MOV, 0xBB, dest = AbstractOperand.EBX, source = AbstractOperand.Constant32),
            AbstractCommand(MOV, 0xBC, dest = AbstractOperand.ESP, source = AbstractOperand.Constant32),
            AbstractCommand(MOV, 0xBD, dest = AbstractOperand.EBP, source = AbstractOperand.Constant32),
            AbstractCommand(MOV, 0xBE, dest = AbstractOperand.ESI, source = AbstractOperand.Constant32),
            AbstractCommand(MOV, 0xBF, dest = AbstractOperand.EDI, source = AbstractOperand.Constant32),
            AbstractCommand(MOV, 0xC6, dest = AbstractOperand.Memory8, source = AbstractOperand.Constant8),
            AbstractCommand(MOV, 0xC7, dest = AbstractOperand.Memory32, source = AbstractOperand.Constant32),
            AbstractCommand(INC, 0x40, dest = AbstractOperand.EAX, source = AbstractOperand.NoOperand)
    )
}

