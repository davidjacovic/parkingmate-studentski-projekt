package org.example

import com.sun.jdi.connect.Connector
import jdk.incubator.vector.VectorOperators
import java.io.InputStream
import java.io.File

const val NEWLINE = '\n'.code
const val EOF_SYMBOL = -1
const val ERROR = 0
const val CITY = 100
const val LET = 101
const val NEW = 102
const val LINK = 103
const val CALL = 104
const val PROCEDURE = 105
const val FOR = 106
const val TO = 107
const val FOREACH = 108
const val IN = 109
const val MARKER = 110
const val IF = 111
const val ELSE = 112
const val SET = 113

const val BUILDING = 114
const val PARK = 115
const val ROAD = 116
const val RIVER = 117
const val TREE = 118
const val JUNCTION = 119

const val LINE = 120
const val BEND = 121
const val BOX = 122
const val CIRC = 123
const val POLYLINE = 124
const val POLYSPLINE = 125

const val ASSIGN = 126
const val LESS = 127
const val LESS_EQUAL = 128
const val EQUAL = 129
const val NOT_EQUAL = 130
const val BIGGER_EQUAL = 131
const val BIGGER = 132

const val LCURLY = 10
const val RCURLY = 11
const val LPAREN = 12
const val RPAREN = 13
const val SEMICOLON = 14
const val COMMA = 15
const val PLUS = 16
const val MINUS = 17
const val TIMES = 18
const val DIVIDE = 19

const val IDENTIFIER = 200
const val INT = 202
const val REAL_NUM = 203
const val LINK_TOKEN = 204


interface DFA {
    val states: Set<Int>
    val alphabet: IntRange
    fun next(state: Int, code: Int): Int
    fun symbol(state: Int): Int
    val startState: Int
    val finalStates: Set<Int>
}


object CityAutomaton : DFA {
    override val states = (1..152).toSet()
    override val alphabet = 0..255
    override val startState = 1
    override var finalStates = setOf(
        5, 8, 10, 14, 17, 18, 19, 20, 21, 22, 23, 24,
        26, 27, 28, 30, 38, 47, 50, 53, 57, 67, 69, 72, 74, 75, 79, 82,
        86, 90, 98, 107, 110, 117, 123, 124, 125, 126, 127, 128, 129, 130, 131,
        132, 133, 140, 150
    )

    private val transitions = Array(states.max() + 1) { IntArray(alphabet.max() + 1) }
    private val values = Array(states.max() + 1) { ERROR }

    private fun setTransition(from: Int, chr: Char, to: Int) {
        transitions[from][chr.code] = to
    }

    private fun setSymbol(state: Int, symbol: Int) {
        values[state] = symbol
    }

    override fun next(state: Int, code: Int): Int = transitions[state][code]
    override fun symbol(state: Int): Int = values[state]

    init {
        //city, call, circ
        setTransition(1, 'c', 2)

        // city
        setTransition(2, 'i', 3)
        setTransition(3, 't', 4)
        setTransition(4, 'y', 5)
        setSymbol(5, CITY)

        // call
        setTransition(2, 'a', 6)
        setTransition(6, 'l', 7)
        setTransition(7, 'l', 8)
        setSymbol(8, CALL)

        // circ
        setTransition(3, 'r', 9)
        setTransition(9, 'c', 10)
        setSymbol(10, CIRC)

        //let, link, line
        setTransition(1, 'l', 12)

        // let
        setTransition(12, 'e', 13)
        setTransition(13, 't', 14)
        setSymbol(14, LET)

        // link i line imaju l i n
        setTransition(12, 'i', 15)
        setTransition(15, 'n', 16)

        // link
        setTransition(16, 'k', 17)
        setSymbol(17, LINK)

        // line
        setTransition(16, 'e', 18)
        setSymbol(18, LINE)

        // operacije
        setTransition(1, '=', 19)
        setTransition(19, '=', 20)
        setSymbol(20, EQUAL)
        setSymbol(19, ASSIGN)
        setTransition(1, '<', 21)
        setTransition(21, '=', 22)
        setSymbol(22, LESS_EQUAL)
        setSymbol(21, LESS)
        setTransition(1, '>', 23)
        setTransition(23, '=', 24)
        setSymbol(24, BIGGER_EQUAL)
        setSymbol(23, BIGGER)
        setTransition(1, '!', 25)
        setTransition(25, '=', 26)
        setSymbol(26, NOT_EQUAL)

        // int i real_num
        ('0'..'9').forEach { setTransition(1, it, 28) }
        ('0'..'9').forEach { setTransition(28, it, 28) }
        setSymbol(28, INT)

        setTransition(28, '.', 29)
        ('0'..'9').forEach { setTransition(29, it, 30) }
        ('0'..'9').forEach { setTransition(30, it, 30) }
        setSymbol(30, REAL_NUM)


        // building, bend, box
        setTransition(1, 'b', 31)

        // building
        setTransition(31, 'u', 32)
        setTransition(32, 'i', 33)
        setTransition(33, 'l', 34)
        setTransition(34, 'd', 35)
        setTransition(35, 'i', 36)
        setTransition(36, 'n', 37)
        setTransition(37, 'g', 38)
        setSymbol(38, BUILDING)

        // bend
        setTransition(31, 'e', 45)
        setTransition(45, 'n', 46)
        setTransition(46, 'd', 47)
        setSymbol(47, BEND)

        // box
        setTransition(31, 'o', 49)
        setTransition(49, 'x', 50)
        setSymbol(50, BOX)

        // for,foreach
        setTransition(1, 'f', 51)
        setTransition(51, 'o', 52)
        setTransition(52, 'r', 53)
        setSymbol(53, FOR)

        setTransition(53, 'e', 54)
        setTransition(54, 'a', 55)
        setTransition(55, 'c', 56)
        setTransition(56, 'h', 57)
        setSymbol(57, FOREACH)

        // marker
        setTransition(1, 'm', 62)
        setTransition(62, 'a', 63)
        setTransition(63, 'r', 64)
        setTransition(64, 'k', 65)
        setTransition(65, 'e', 66)
        setTransition(66, 'r', 67)
        setSymbol(67, MARKER)

        // to, tree
        setTransition(1, 't', 68)

        // to
        setTransition(68, 'o', 69)
        setSymbol(69, TO)

        // tree
        setTransition(68, 'r', 70)
        setTransition(70, 'e', 71)
        setTransition(71, 'e', 72)
        setSymbol(72, TREE)

        // in, if
        setTransition(1, 'i', 73)

        // in
        setTransition(73, 'n', 74)
        setSymbol(74, IN)

        // if
        setTransition(73, 'f', 75)
        setSymbol(75, IF)

        // else
        setTransition(1, 'e', 76)
        setTransition(76, 'l', 77)
        setTransition(77, 's', 78)
        setTransition(78, 'e', 79)
        setSymbol(79, ELSE)

        // set
        setTransition(1, 's', 80)
        setTransition(80, 'e', 81)
        setTransition(81, 't', 82)
        setSymbol(82, SET)

        // road, river
        setTransition(1, 'r', 83)

        // road
        setTransition(83, 'o', 84)
        setTransition(84, 'a', 85)
        setTransition(85, 'd', 86)
        setSymbol(86, ROAD)

        // river
        setTransition(83, 'i', 87)
        setTransition(87, 'v', 88)
        setTransition(88, 'e', 89)
        setTransition(89, 'r', 90)
        setSymbol(90, RIVER)

        // junction
        setTransition(1, 'j', 91)
        setTransition(91, 'u', 92)
        setTransition(92, 'n', 93)
        setTransition(93, 'c', 94)
        setTransition(94, 't', 95)
        setTransition(95, 'i', 96)
        setTransition(96, 'o', 97)
        setTransition(97, 'n', 98)
        setSymbol(98, JUNCTION)


        // procedure, park, polyline, polyspline
        setTransition(1, 'p', 99)

        // procedure
        setTransition(99, 'r', 100)
        setTransition(100, 'o', 101)
        setTransition(101, 'c', 102)
        setTransition(102, 'e', 103)
        setTransition(103, 'd', 104)
        setTransition(104, 'u', 105)
        setTransition(105, 'r', 106)
        setTransition(106, 'e', 107)
        setSymbol(107, PROCEDURE)

        // park
        setTransition(99, 'a', 108)
        setTransition(108, 'r', 109)
        setTransition(109, 'k', 110)
        setSymbol(110, PARK)

        // Dodaj:
        ('a'..'z').forEach { c -> setTransition(110, c, 27) }
        ('A'..'Z').forEach { c -> setTransition(110, c, 27) }
        ('0'..'9').forEach { c -> setTransition(110, c, 27) }
        setTransition(110, '_', 27)

        // polyline i polyspline
        setTransition(99, 'o', 111)
        setTransition(111, 'l', 112)
        setTransition(112, 'y', 113)

        // polyline
        setTransition(113, 'l', 114)
        setTransition(114, 'i', 115)
        setTransition(115, 'n', 116)
        setTransition(116, 'e', 117)
        setSymbol(117, POLYLINE)

        // polyspline
        setTransition(113, 's', 118)
        setTransition(118, 'p', 119)
        setTransition(119, 'l', 120)
        setTransition(120, 'i', 121)
        setTransition(121, 'n', 122)
        setTransition(122, 'e', 123)
        setSymbol(123, POLYSPLINE)

        // matematicki simboli
        setTransition(1, '{', 124); setSymbol(124, LCURLY)
        setTransition(1, '}', 125); setSymbol(125, RCURLY)
        setTransition(1, '(', 126); setSymbol(126, LPAREN)
        setTransition(1, ')', 127); setSymbol(127, RPAREN)
        setTransition(1, ';', 128); setSymbol(128, SEMICOLON)
        setTransition(1, ',', 129); setSymbol(129, COMMA)
        setTransition(1, '+', 130); setSymbol(130, PLUS)
        setTransition(1, '-', 131); setSymbol(131, MINUS)
        setTransition(1, '*', 132); setSymbol(132, TIMES)
        setTransition(1, '/', 133); setSymbol(133, DIVIDE)

        // link_token
        setTransition(1, 'h', 134)
        setTransition(134, 't', 135)
        setTransition(135, 't', 136)
        setTransition(136, 'p', 137)
        setTransition(137, ':', 138)
        setTransition(138, '/', 139)
        setTransition(139, '/', 140)

        setTransition(137, 's', 141)
        setTransition(141, ':', 138)
        setTransition(138, '/', 139)
        setTransition(139, '/', 140)

        // new
        setTransition(1, 'n', 141)
        setTransition(141, 'e', 142)
        setTransition(142, 'w', 143)
        setSymbol(143, NEW)


        val urlChars = ('a'..'z') + ('A'..'Z') + ('0'..'9') + listOf('.', '/', ':', '-', '_', '?', '=', '%', '&', '#')
        urlChars.forEach { c -> setTransition(140, c, 140) }
        setSymbol(140, LINK_TOKEN)


        // Stavio sam identifier i variable koa jedno, posto je sintaksa ista
        ('a'..'z').forEach { setTransition(1, it, 150) }
        ('A'..'Z').forEach { setTransition(1, it, 150) }
        setTransition(1, '_', 150)

        ('a'..'z').forEach { setTransition(150, it, 150) }
        ('A'..'Z').forEach { setTransition(150, it, 150) }
        ('0'..'9').forEach { setTransition(150, it, 150) }
        setTransition(150, '_', 150)
        setSymbol(150, IDENTIFIER)
    }
}

data class Token(val symbol: Int, val lexeme: String, val startRow: Int, val startColumn: Int)
/*
*
*   ===================SCANNER===================
*
*/
class Scanner(private val automaton: DFA, private val stream: InputStream) {
    private var last: Int? = null
    private var row = 1
    private var column = 1

    private fun updatePosition(code: Int) {
        if (code == NEWLINE) {
            row += 1
            column = 1
        } else {
            column += 1
        }
    }

    fun getToken(): Token {
        var code = last ?: stream.read()
        while (code != EOF_SYMBOL && code.toChar().isWhitespace()) {
            updatePosition(code)
            code = stream.read()
        }

        if (code == EOF_SYMBOL) throw NoSuchElementException("End of input")

        val startRow = row
        val startColumn = column
        val buffer = mutableListOf<Char>()

        var state = automaton.startState
        var lastFinalState = -1
        var lastFinalPosition = -1
        var lastFinalBuffer = mutableListOf<Char>()

        while (code != EOF_SYMBOL) {
            val nextState = automaton.next(state, code)
            if (nextState == ERROR) {
                break
            }
            state = nextState
            buffer.add(code.toChar())

            // Final state -> zapamti poziciju
            if (automaton.finalStates.contains(state)) {
                lastFinalState = state
                lastFinalPosition = buffer.size
                lastFinalBuffer = buffer.toMutableList()
            }

            updatePosition(code)
            code = stream.read()
        }

        // No final state -> greska u analizi
        if (lastFinalState == -1) {
            throw Error("Invalid token at $startRow:$startColumn")
        }

        // Visak pri citanju
        if (code != EOF_SYMBOL && lastFinalPosition < buffer.size) {
            last = buffer.subList(lastFinalPosition, buffer.size).first().code
            val unreadChars = buffer.size - lastFinalPosition
            column -= unreadChars
        } else {
            last = null
        }

        val lexeme = lastFinalBuffer.joinToString("")
        val symbol = automaton.symbol(lastFinalState)
        return Token(symbol, lexeme, startRow, startColumn)
    }
}

/*
*
*   ===================PARSER===================
*
*/
class Parser(private val scanner: Scanner) {
    private var currentToken: Token? = scanner.getToken()

    private fun advance() {
        currentToken = try {
            scanner.getToken()
        } catch (e: NoSuchElementException) {
            null
        }
    }

    private fun expectLexeme(expected: String) {
        if (currentToken?.lexeme != expected) {
            error("Expected '$expected' but found '${currentToken?.lexeme}'")
        }
    }

    fun Program(): Boolean {
        if (!(Declarations() && CityBlock())){
            return false
        }

        if (currentToken != null) {
            error("Expected end of input but found ${currentToken!!.lexeme}")
            return false
        } else return true
    }

    private fun Declarations() : Boolean {
        Let()
        return true
    }

    private fun Let(): Boolean {
        if (!(Syntax() && ValueType())){
            return false
        }
        if (currentToken?.symbol == SEMICOLON) {
            advance()
            return true
        } else return false
    }

    private fun Syntax(): Boolean {
        if (currentToken?.lexeme == "let") {
            advance()
            if (currentToken?.symbol == IDENTIFIER) {
                advance()
                if (currentToken?.lexeme == "=") {
                    advance()
                    return true
                } else return false
            } else return false
        } else return false
    }

    private fun ValueType(): Boolean {
        if (currentToken?.lexeme == "new") {
            advance()
            if (currentToken?.lexeme == "(") {
                advance()
                if (!Argument()){
                    return false
                } else if (currentToken?.lexeme == ",") {
                    advance()
                    if (!Argument()){
                        return false
                    } else if (currentToken?.lexeme == ")") {
                        advance()
                        return true
                    } else return false
                } else return false
            } else return false
        } else if (Argument()){
            return true
        } else if (CallP()){
            return true
        } else if (Link()){
            return true
        } else return false
    }

    private fun Link(): Boolean {
        if (currentToken?.lexeme == "link") {
            advance()
            if (currentToken?.lexeme == "(") {
                advance()
                if (currentToken?.symbol == IDENTIFIER) { //treba LINK_TOKEN
                    advance()
                    if (currentToken?.lexeme == ")") {
                        advance()
                        return true
                    } else return false
                } else return false
            } else return false
        } else return false
    }

    private fun CallP(): Boolean {
        if (currentToken?.lexeme == "call") {
            advance()
            if (currentToken?.symbol == IDENTIFIER) {
                advance()
                if (Parameter()) {
                    if (currentToken?.lexeme == ";") {
                        advance()
                        return true
                    } else return false
                } else return false
            } else return false
        }else return false
    }

    private fun Parameter(): Boolean {
        if (currentToken?.lexeme == "(") {
            advance()
            if (Arguments()) {
                if (currentToken?.lexeme == ")") {
                    advance()
                    return true
                } else return false
            } else return false
        } else return false
    }

    private fun Arguments(): Boolean {
        return (Argument() && ArgumentsTail())
    }

    private fun ArgumentsTail(): Boolean {
        if (currentToken?.lexeme == ",") {
            advance()
            if (!Argument()) {
                return false
            } else return ArgumentsTail()
        } else return true
    }

    private fun Argument(): Boolean {
        return Additive()
    }

    private fun Additive(): Boolean {
        return Multiplicative() && AdditivePrim()
    }

    private fun AdditivePrim(): Boolean {
        if (currentToken?.lexeme == "+" || currentToken?.lexeme == "-") {
            advance()
            return Multiplicative() && AdditivePrim()
        } else return true
    }

    private fun Multiplicative(): Boolean {
        return Unary() && MultiplicativePrim()
    }

    private fun MultiplicativePrim(): Boolean {
        if (currentToken?.lexeme == "*" || currentToken?.lexeme == "/") {
            advance()
            return Unary() && MultiplicativePrim()
        } else return true
    }

    private fun Unary(): Boolean {
        if (currentToken?.lexeme == "+" || currentToken?.lexeme == "-") {
            advance()
            return Primary()
        } else return Primary()
    }

    private fun Primary(): Boolean {
        if (currentToken?.symbol == INT || currentToken?.symbol == IDENTIFIER) {
            advance()
            return true
        } else if (currentToken?.lexeme == "(") {
            advance()
            if (Additive()){
                if (currentToken?.lexeme == ")") {
                    advance()
                    return true
                } else return false
            } else return false
        } else return false
    }

    private fun CityBlock(): Boolean {
        if (currentToken?.lexeme == "city") {
            advance()
            return City()
        } else return false
    }

    private fun City(): Boolean {
        if (currentToken?.symbol == IDENTIFIER) {
            advance()
            return Body()
        } else return false
    }

    private fun Body(): Boolean {
        if (currentToken?.symbol == LCURLY) {
            advance()
            if (Elements()){
                if (currentToken?.symbol == RCURLY){
                    advance()
                    return true
                } else return false
            } else return false
        } else return false
    }

    private fun Elements(): Boolean {
        return Element() && ElementsTail()
    }

    private fun ElementsTail(): Boolean {
        Element() && ElementsTail()
        return true
    }

    private fun Element(): Boolean {
        if (Block()){
            return true
        } else if (Command()) {
            return true
        } else if (Loop()) {
            return true
        } else if (DefineP()) {
            return true
        } else if (Marker()) {
            return true
        } else if (Conditions()) {
            return true
        } else return false
    }

    private fun Block(): Boolean {
        if (BlockTypeWid()){
            if (currentToken?.symbol == IDENTIFIER) {
                advance()
                if (currentToken?.symbol == LCURLY) {
                    advance()
                    if (BlockStatementList()) {
                        if (currentToken?.symbol == RCURLY) {
                            advance()
                            if (currentToken?.symbol == SEMICOLON) {
                                advance()
                                return true
                            } else return false
                        } else return false
                    } else return false
                } else return false
            } else return false
        } else if (currentToken?.lexeme == "tree") {
            advance()
            if (currentToken?.symbol == LCURLY) {
                advance()
                if (BlockStatementList()) {
                    if (currentToken?.symbol == RCURLY) {
                        advance()
                        if (currentToken?.symbol == SEMICOLON) {
                            advance()
                            return true
                        } else return false
                    } else return false
                } else return false
            } else return false
        } else if (currentToken?.lexeme == "junction") {
            advance()
            if (currentToken?.symbol == LCURLY) {
                advance()
                if (BlockStatementList()) {
                    if (currentToken?.symbol == RCURLY) {
                        advance()
                        if (currentToken?.symbol == SEMICOLON) {
                            advance()
                            return true
                        } else return false
                    } else return false
                } else return false
            } else return false
        } else return false
    }

    private fun BlockStatementList(): Boolean {
        BlockStatement() && BlockStatementList()
        return true
    }

    private fun BlockStatement(): Boolean {
        if (Command()) {
            return true
        } else if (Let()) {
            return true
        } else if (CallP()) {
            return true
        } else if (Loop()) {
            return true
        } else if (Meta()) {
            return true
        } else if (Conditions()) {
            return true
        } else return false
    }

    private fun BlockTypeWid(): Boolean {
        if (currentToken?.lexeme == "building") {
            advance()
            return true
        } else if (currentToken?.lexeme == "road") {
            advance()
            return true
        } else if (currentToken?.lexeme == "park") {
            advance()
            return true
        } else if (currentToken?.lexeme == "river") {
            advance()
            return true
        } else return false
    }

    private fun Command(): Boolean {
        return CommandType() && Parameter()
    }

    private fun CommandType(): Boolean {
        if (currentToken?.lexeme == "line") {
            advance()
            return true
        } else if (currentToken?.lexeme == "bend") {
            advance()
            return true
        } else if (currentToken?.lexeme == "box") {
            advance()
            return true
        } else if (currentToken?.lexeme == "circ") {
            advance()
            return true
        } else if (currentToken?.lexeme == "polyline") {
            advance()
            return true
        } else if (currentToken?.lexeme == "polyspline") {
            advance()
            return true
        } else return false
    }

    private fun Loop(): Boolean {
        if (LoopList() && Body()) {
            if (currentToken?.symbol == SEMICOLON){
                advance()
                return true
            } else return false
        } else return false
    }

    private fun LoopList(): Boolean {
        if (ForLoop()) {
            return true
        } else if (ForeachLoop()) {
            return true
        } else return false
    }

    private fun ForLoop(): Boolean {
        if (currentToken?.lexeme == "for") {
            advance()
            if (currentToken?.symbol == IDENTIFIER) {
                advance()
                if (currentToken?.symbol == ASSIGN) {
                    advance()
                    if (currentToken?.symbol == INT) {
                        advance()
                        if (currentToken?.lexeme == "to") {
                            advance()
                            if (currentToken?.symbol == INT) {
                                advance()
                                return true
                            } else return false
                        } else return false
                    } else return false
                } else return false
            } else return false
        } else return false
    }

    private fun ForeachLoop(): Boolean {
        if (currentToken?.lexeme == "foreach") {
            advance()
            if (currentToken?.symbol == IDENTIFIER) {
                advance()
                if (currentToken?.lexeme == "in") {
                    advance()
                    if (currentToken?.symbol == IDENTIFIER) {
                        advance()
                        return true
                    } else return false
                } else return false
            } else return false
        } else return false
    }

    private fun Meta(): Boolean {
        if (currentToken?.lexeme == "set") {
            advance()
            if (currentToken?.symbol == LPAREN) {
                advance()
                if (currentToken?.symbol == IDENTIFIER) {
                    advance()
                    if (currentToken?.symbol == COMMA) {
                        advance()
                        if (Additive()) {
                            if (currentToken?.symbol == RPAREN) {
                                advance()
                                if (currentToken?.symbol == SEMICOLON) {
                                    advance()
                                    return true
                                } else return false
                            } else return false
                        } else return false
                    } else return false
                } else return false
            } else return false
        } else return false
    }

    private fun Conditions(): Boolean {
        if (currentToken?.lexeme == "if") {
            advance()
            if (currentToken?.symbol == LPAREN) {
                advance()
                if (Condition()) {
                    if (currentToken?.symbol == RPAREN) {
                        advance()
                        if (Body()) {
                            if (ElseStatement()){
                                return true
                            } else return false
                        } else return false
                    } else return false
                } else return false
            } else return false
        } else return false
    }

    private fun Condition(): Boolean {
        if (Additive() && Compare() && Additive()) {
            return true
        } else return CallP()
    }

    private fun Compare(): Boolean {
        if (currentToken?.symbol == LESS) {
            advance()
            return true
        } else if (currentToken?.symbol == LESS_EQUAL) {
            advance()
            return true
        } else if (currentToken?.symbol == EQUAL) {
            advance()
            return true
        } else if (currentToken?.symbol == NOT_EQUAL) {
            advance()
            return true
        } else if (currentToken?.symbol == BIGGER_EQUAL) {
            advance()
            return true
        } else if (currentToken?.symbol == BIGGER) {
            advance()
            return true
        } else return false
    }

    private fun ElseStatement(): Boolean {
        if (currentToken?.lexeme == "else") {
            advance()
            if (Body()){
                if (currentToken?.symbol == SEMICOLON) {
                    advance()
                    return true
                } else return false
            } else return false
        } else return true
    }

    private fun DefineP(): Boolean {
        if (currentToken?.lexeme == "procedure") {
            advance()
            if (currentToken?.symbol == IDENTIFIER) {
                advance()
                if (Parameter() && Body()) {
                    if (currentToken?.symbol == SEMICOLON) {
                        advance()
                        return true
                    } else return false
                } else return false
            } else return false
        } else return false
    }

    private fun Marker(): Boolean {
        if (currentToken?.lexeme == "marker") {
            advance()
            return MarkerTail()
        } else return false
    }

    private fun MarkerTail(): Boolean {
        if (currentToken?.symbol == IDENTIFIER) {
            advance()
            if (Parameter() && MarkerBody()) {
                if (currentToken?.symbol == SEMICOLON) {
                    advance()
                    return true
                } else return false
            } else return false
        } else if (Parameter() && MarkerBody()) {
            if (currentToken?.symbol == SEMICOLON) {
                advance()
                return true
            } else return false
        } else return false
    }

    private fun MarkerBody(): Boolean {
        Body()
        return true
    }

    fun parse(): Boolean {
        if (Program()) {
            println("bravo ti ga tebi")
            return true
        } else {
            println("jebiga...")
            return false
        }
    }
}

fun tokenName(symbol: Int) = when (symbol) {
    CITY -> "CITY"
    CALL -> "CALL"
    CIRC -> "CIRC"
    LET -> "LET"
    LINK -> "LINK"
    LINE -> "LINE"
    EQUAL -> "EQUAL"
    ASSIGN -> "ASSIGN"
    LESS -> "LESS"
    LESS_EQUAL -> "LESS_EQUAL"
    BIGGER -> "BIGGER"
    BIGGER_EQUAL -> "BIGGER_EQUAL"
    NOT_EQUAL -> "NOT_EQUAL"
    IDENTIFIER -> "IDENTIFIER"
    INT -> "INT"
    REAL_NUM -> "REAL_NUM"
    BUILDING -> "BUILDING"
    BEND -> "BEND"
    BOX -> "BOX"
    FOR -> "FOR"
    FOREACH -> "FOREACH"
    MARKER -> "MARKER"
    TO -> "TO"
    TREE -> "TREE"
    IN -> "IN"
    IF -> "IF"
    ELSE -> "ELSE"
    SET -> "SET"
    ROAD -> "ROAD"
    RIVER -> "RIVER"
    JUNCTION -> "JUNCTION"
    PROCEDURE -> "PROCEDURE"
    PARK -> "PARK"
    POLYLINE -> "POLYLINE"
    POLYSPLINE -> "POLYSPLINE"
    LCURLY -> "LCURLY"
    RCURLY -> "RCURLY"
    LPAREN -> "LPAREN"
    RPAREN -> "RPAREN"
    SEMICOLON -> "SEMICOLON"
    COMMA -> "COMMA"
    PLUS -> "PLUS"
    MINUS -> "MINUS"
    TIMES -> "TIMES"
    DIVIDE -> "DIVIDE"
    LINK_TOKEN -> "LINK_TOKEN"
    ERROR -> "ERROR"
    NEW -> "NEW"
    else -> "UNKNOWN"
}

fun printTokens(scanner: Scanner) {
    try {
        while (true) {
            val token = scanner.getToken()
            if (token.symbol == IDENTIFIER) {
                val keywordSymbol = when (token.lexeme) {
                    "city" -> CITY
                    "call" -> CALL
                    "circ" -> CIRC
                    "let" -> LET
                    "link" -> LINK
                    "line" -> LINE
                    "building" -> BUILDING
                    "bend" -> BEND
                    "box" -> BOX
                    "for" -> FOR
                    "foreach" -> FOREACH
                    "marker" -> MARKER
                    "to" -> TO
                    "tree" -> TREE
                    "in" -> IN
                    "if" -> IF
                    "else" -> ELSE
                    "set" -> SET
                    "road" -> ROAD
                    "river" -> RIVER
                    "junction" -> JUNCTION
                    "procedure" -> PROCEDURE
                    "park" -> PARK
                    "polyline" -> POLYLINE
                    "polyspline" -> POLYSPLINE
                    "new" -> NEW
                    else -> null
                }

                if (keywordSymbol != null) {
                    println("${tokenName(keywordSymbol)}(\"${token.lexeme}\") at ${token.startRow}:${token.startColumn}")
                    continue
                }
            }
            println("${tokenName(token.symbol)}(\"${token.lexeme}\") at ${token.startRow}:${token.startColumn}")
        }
    } catch (e: NoSuchElementException) {
        println("Lexical analysis completed.")
    } catch (e: Error) {
        println("Lexical error: ${e.message}")
    }
}


fun main() {
    val filename = "C:\\Users\\keser\\parkingmate-studentski-projekt\\git2\\parkingmate-studentski-projekt\\prevajanje\\src\\main\\input.txt"

    val file = File(filename)
    if (!file.exists()) {
        println("File not found: $filename")
        return
    }

    val inputStream = file.inputStream()
    val scanner = Scanner(CityAutomaton, inputStream)
    val parser = Parser(scanner)

    if (parser.parse()) {
        println("accept")
    } else println("error")

    //printTokens(scanner)
}
