package org.example

import kotlin.math.*
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

data class Point(val x: Double, val y: Double)

fun segmentsIntersect(a1: Point, a2: Point, b1: Point, b2: Point): Point? {
    val d = (a1.x - a2.x) * (b1.y - b2.y) - (a1.y - a2.y) * (b1.x - b2.x)
    if (d == 0.0) return null // Paralelne

    val xi = ((b1.x - b2.x) * (a1.x * a2.y - a1.y * a2.x) - (a1.x - a2.x) * (b1.x * b2.y - b1.y * b2.x)) / d
    val yi = ((b1.y - b2.y) * (a1.x * a2.y - a1.y * a2.x) - (a1.y - a2.y) * (b1.x * b2.y - b1.y * b2.x)) / d
    val inter = Point(xi, yi)

    fun between(p: Point, q: Point, r: Point): Boolean {
        return minOf(p.x, q.x) <= r.x && r.x <= maxOf(p.x, q.x) &&
                minOf(p.y, q.y) <= r.y && r.y <= maxOf(p.y, q.y)
    }

    return if (between(a1, a2, inter) && between(b1, b2, inter)) inter else null
}

fun extractPoints(command: Commands): List<Point> {
    val coords = command.coordinates.values
    val points = mutableListOf<Point>()
    for (i in 0 until coords.size - 1 step 2) {
        val x = coords[i]
        val y = coords[i + 1]
        if (x != null && y != null) {
            points.add(Point(x, y))
        }
    }
    return points
}

fun findIntersections(lines: List<Commands>): List<Point> {
    val points = mutableListOf<Point>()

    // Helper: ekstrakcija tačaka iz komandne geometrije
    fun extractPoints(command: Commands): List<Point> {
        val coords = command.coordinates.values
        val points = mutableListOf<Point>()
        for (i in coords.indices step 2) {
            val x = coords[i]
            val y = coords.getOrNull(i + 1)
            if (x != null && y != null) {
                points.add(Point(x, y))
            }
        }
        return points
    }

    for (i in 0 until lines.size) {
        val line1 = extractPoints(lines[i])
        for (j in i + 1 until lines.size) {
            val line2 = extractPoints(lines[j])
            for (k in 0 until line1.size - 1) {
                val a1 = line1[k]
                val a2 = line1[k + 1]
                for (l in 0 until line2.size - 1) {
                    val b1 = line2[l]
                    val b2 = line2[l + 1]
                    val inter = segmentsIntersect(a1, a2, b1, b2)
                    if (inter != null) points.add(inter)
                }
            }
        }
    }

    return points
}


fun pointToGeoJSON(p: Point): String = """
    {
      "type": "Feature",
      "geometry": {
        "type": "Point",
        "coordinates": [${p.x}, ${p.y}]
      },
      "properties": {
        "marker": "intersection"
      }
    }
""".trimIndent()

fun createParallelLine(x1: Double, y1: Double, x2: Double, y2: Double, distance: Double): Pair<Point, Point> {
    // Vektor pravca
    val dx = x2 - x1
    val dy = y2 - y1

    // Dužina linije
    val length = hypot(dx, dy)
    if (length == 0.0) throw IllegalArgumentException("Pocetna i krajnja tacka su iste!")

    // Jedinični vektor normale (okomit na pravac)
    val nx = -dy / length
    val ny = dx / length

    // Pomera originalne tačke za datu udaljenost u pravcu normale
    val newStart = Point(x1 + nx * distance, y1 + ny * distance)
    val newEnd = Point(x2 + nx * distance, y2 + ny * distance)

    return Pair(newStart, newEnd)
}

class City(val name: String, val elements: List<Elt> = listOf()) {
    fun toGeoJSON(): String {
        val elementFeatures = elements.map { it.toGeoJSON() }

        val allLines = elements.flatMap { it.commands }
            .filterNotNull()
            .filter { it.type in listOf("line", "polyline", "bend", "circ" , "box") }

        val intersectionPoints = findIntersections(allLines)
        val intersectionFeatures = intersectionPoints.map { pointToGeoJSON(it) }

        val allFeatures = elementFeatures + intersectionFeatures

        return """
        {
          "type": "FeatureCollection",
          "features": [
            ${allFeatures.joinToString(",\n")}
          ]
        }
        """.trimIndent()
    }
}


class Elt(val name: String, val type: String, val commands: List<Commands?> = listOf()) {
    fun toGeoJSON(): String {
        val geometries = commands.filterNotNull().map { it.toGeoJSON() }

        return when (geometries.size) {
            0 -> throw Error("Element has no geometry")
            1 -> """
                {
                  "type": "Feature",
                  "geometry": ${geometries[0]},
                  "properties": {
                    "name": "$name",
                    "type": "$type"
                  }
                }
            """.trimIndent()

            else -> {
                val joined = geometries.joinToString(",\n")
                """
                {
                  "type": "Feature",
                  "geometry": {
                    "type": "GeometryCollection",
                    "geometries": [
                      $joined
                    ]
                  },
                  "properties": {
                    "name": "$name",
                    "type": "$type"
                  }
                }
                """.trimIndent()
            }
        }
    }
}

class Commands(val type: String, val coordinates: Coordinates) {
    fun createCircle(cx: Double, cy: Double, radius: Double, segments: Int = 64): String {
        val points = mutableListOf<String>()
        for (i in 0..segments) {
            val angle = 2 * PI * i / segments
            val x = cx + radius * cos(angle)
            val y = cy + radius * sin(angle)
            points.add("[${"%.6f".format(x)}, ${"%.6f".format(y)}]")
        }
        // Zatvaranje kruga (ponavljanje prve tačke)
        points.add(points.first())
        val joinedCoords = points.joinToString(", ")
        return """
            {
              "type": "Polygon",
              "coordinates": [
                [ $joinedCoords ]
              ]
            }
        """.trimIndent()
    }

    fun createBendLine(
        x1: Double, y1: Double,
        x2: Double, y2: Double,
        angleDeg: Double,
        segments: Int = 20
    ): String {
        val mx = (x1 + x2) / 2
        val my = (y1 + y2) / 2

        val dx = x2 - x1
        val dy = y2 - y1
        val length = hypot(dx, dy)

        val angleRad = Math.toRadians(angleDeg)

        val h = tan(angleRad / 2) * (length / 2)

        val nx = -dy / length
        val ny = dx / length

        val cx = mx + nx * h
        val cy = my + ny * h

        var startAngle = atan2(y1 - cy, x1 - cx)
        var endAngle = atan2(y2 - cy, x2 - cx)

        if (endAngle < startAngle) {
            endAngle += 2 * PI
        }

        val radius = (length / 2) / cos(angleRad / 2)

        val points = mutableListOf<List<Double>>()

        for (i in 0..segments) {
            val t = i.toDouble() / segments
            val angle = startAngle + t * (endAngle - startAngle)
            val px = cx + radius * cos(angle)
            val py = cy + radius * sin(angle)
            points.add(listOf("%.6f".format(px).toDouble(), "%.6f".format(py).toDouble()))
        }

        val coordinatesStr = points.joinToString(", ") { "[${it[0]}, ${it[1]}]" }

        return """
            {
            "type": "LineString",
            "coordinates": [ $coordinatesStr ]
            }
    """.trimIndent()
    }


    fun toGeoJSON(): String {
        val points = mutableListOf<String>()
        for (i in coordinates.values.indices step 2) {
            val x = coordinates.values[i]
            val y = coordinates.values.getOrNull(i + 1)
            if (x != null && y != null) {
                points.add("[$x, $y]")
            }
        }

        val joined = points.joinToString(", ")

        return when {
            type == "box" && coordinates.values.size == 8 -> """
            {
              "type": "Polygon",
              "coordinates": [
                [ $joined, [${coordinates.values[0]}, ${coordinates.values[1]}] ]
              ]
            }
        """.trimIndent()

            type == "box" && coordinates.values.size == 4 -> {
                val v = coordinates.values[0]
                val s = coordinates.values[1]
                val d = coordinates.values[2]
                val k = coordinates.values[3]
                """
            {
              "type": "Polygon",
              "coordinates": [
                [[$v, $s], [$v, $k], [$d, $k], [$d, $s], [$v, $s]]
              ]
            }
            """.trimIndent()
            }

            type == "line" && coordinates.values.size == 4 -> """
            {
              "type": "LineString",
              "coordinates": [ $joined ]
            }
        """.trimIndent()

            type == "polyline" && coordinates.values.size >= 6 -> """
            {
              "type": "LineString",
              "coordinates": [ $joined ]
            }
        """.trimIndent()

            type == "bend" && coordinates.values.size == 5 -> {
                val v = coordinates.values[0]
                val s = coordinates.values[1]
                val d = coordinates.values[2]
                val k = coordinates.values[3]
                val n = coordinates.values[4]
                if (v != null && s != null && d != null && k != null && n != null) {
                    createBendLine(v, s, d, k, n)
                } else throw Error("Invalid coordinates for bend")
            }

            // NOVO: podrška za krug

            type == "circ" && coordinates.values.size == 3 -> {
                val cx = coordinates.values[0]
                val cy = coordinates.values[1]
                val radius = coordinates.values[2]
                if (cx != null && cy != null && radius != null) {
                    createCircle(cx, cy, radius)
                } else throw Error("Invalid coordinates for circle")
            }

            type == "mark" && coordinates.values.size == 2 -> {
                val cx = coordinates.values[0]
                val cy = coordinates.values[1]
                return """{
                    "type": "Point",
                    "coordinates": [${cx}, ${cy}]
                }
                """

            }

            else -> throw Error("Invalid command type: $type")
        }
    }
}

class Coordinates(val values: List<Double?>) {
    fun toGeoJSON(): String {
        val temp = values[1]
        return "[${temp} ${values.drop(1).forEach { ", $it" }}]"
    }
}

/*
*
*   ===================PARSER===================
*
*/
class Parser(private val scanner: Scanner) {
    private var currentToken: Token? = scanner.getToken()
    val dictionaryNum: MutableMap<String?, Double> = mutableMapOf()
    val dictionaryString: MutableMap<String?, String?> = mutableMapOf()
    val dictionaryPairs: MutableMap<String?, Pair<Double?, Double?>> = mutableMapOf()
    var pair: Boolean = false

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

    fun Program(): City {
        while (currentToken?.lexeme == "let") {
            Let()
        }
        return CityBlock()
    }

    private fun Declarations(): Boolean {
        Let()
        return true
    }

    private fun Let() {
        val variable_name = Syntax() // Pretpostavljamo da je ovo String?
        val list = ValueType()
        val keyword = list[0]
        val values = list.drop(1)

        if (keyword == "num") {
            // Samo ako je brojčana vrednost
            try {
                val value = values.firstOrNull()?.toDouble()
                if (value != null) {
                    dictionaryNum[variable_name] = value
                }
            } catch (e: NumberFormatException) {
                throw Error("Cannot convert to number: ${values.firstOrNull()}")
            }
        } else if (keyword == "new") {
            // Samo ako je brojčana vrednost
            try {
                val value = values[0]?.toDouble()
                val value2 = values[1]?.toDouble()
                if (value != null && value2 != null) {
                    val vals = Pair(value, value2)
                    dictionaryPairs[variable_name] = vals
                }
            } catch (e: NumberFormatException) {
                throw Error("Cannot convert to number: ${values.firstOrNull()}")
            }
        } else if (keyword == "link") {
            val link = values[0]
            dictionaryString[variable_name] = values.firstOrNull()
        }

        if (currentToken?.symbol == SEMICOLON) {
            advance()
        } else throw Error("Missing semicolon.")
    }


    private fun Syntax(): String? {
        if (currentToken?.lexeme == "let") {
            advance()
            if (currentToken?.symbol == IDENTIFIER) {
                val variable = currentToken?.lexeme
                advance()
                if (currentToken?.lexeme == "=") {
                    advance()
                    return variable
                } else throw Error("Variable must have value assignment.")
            } else throw Error("Variable name expected.")
        } else throw Error("Let keyword expected.")
    }

    private fun ValueType(): List<String?> {
        val list = mutableListOf<String?>()
        if (currentToken?.lexeme == "new") {
            advance()
            if (currentToken?.lexeme == "(") {
                advance()
                val arg1 = Argument()
                if (currentToken?.lexeme == ",") {
                    advance()
                    val arg2 = Argument()
                    if (currentToken?.lexeme == ")") {
                        advance()
                        list.add("new")
                        list.add(arg1.toString())
                        list.add(arg2.toString())
                        return list
                    } else throw Error("Unexpected argument.")
                } else throw Error("Missing separator ','.")
            } else throw Error("Missing opening parenthesis '('.")
        } else if (currentToken?.symbol == INT || currentToken?.symbol == REAL_NUM || (currentToken?.symbol == IDENTIFIER && currentToken?.lexeme != "link") || currentToken?.symbol == PLUS || currentToken?.symbol == MINUS || currentToken?.symbol == LPAREN) {
            val arg = Argument()
            list.add("num")
            list.add(arg.toString())
            return list
        } else throw Error("Unexpected lexeme '${currentToken?.lexeme}'.")
    }

    private fun Parameter(): Coordinates {
        if (currentToken?.lexeme == "(") {
            advance()
            val args = Arguments()
            if (currentToken?.lexeme == ")") {
                advance()
                return args
            } else throw error("Unmatched parenthesis")
        } else throw error("Invalid parameter")
    }

    private fun Arguments(): Coordinates {
        val list = mutableListOf<Double?>()
        list.add(Argument())
        val tail = ArgumentsTail()
        list.addAll(tail)
        return Coordinates(list)
    }

    private fun ArgumentsTail(): MutableList<Double?> {
        val list = mutableListOf<Double?>()
        if (currentToken?.lexeme == ",") {
            advance()
            val left = Argument()
            list.add(left)
            val tail = ArgumentsTail()
            list.addAll(tail)
            return list
        } else return list
    }

    private fun Argument(): Double? {
        return Additive()
    }

    private fun Additive(): Double? {
        val left = Multiplicative()
        return AdditivePrim(left)
    }

    private fun AdditivePrim(multiplicative: Double?): Double? {
        if (currentToken?.lexeme == "+") {
            advance()
            return AdditivePrim(Multiplicative()?.let { multiplicative?.plus(it) })
        } else if (currentToken?.lexeme == "-") {
            advance()
            return AdditivePrim(Multiplicative()?.let { multiplicative?.minus(it) })
        } else return multiplicative
    }

    private fun Multiplicative(): Double? {
        val left = Unary()
        return MultiplicativePrim(left)
    }

    private fun MultiplicativePrim(unary: Double?): Double? {
        if (currentToken?.lexeme == "*") {
            advance()
            return MultiplicativePrim(Unary()?.let { unary?.times(it) })
        } else if (currentToken?.lexeme == "/") {
            advance()
            return MultiplicativePrim(Unary()?.let { unary?.div(it) })
        } else return unary
    }

    private fun Unary(): Double? {
        if (currentToken?.lexeme == "+") {
            advance()
            return Primary()
        } else if (currentToken?.lexeme == "-") {
            advance()
            return Primary()?.let { -it }
        } else return Primary()
    }

    private fun Primary(): Double? {
        var name: String? = "y"
        if (currentToken?.symbol == INT || currentToken?.symbol == REAL_NUM) {
            val num = currentToken?.lexeme?.toDouble()
            advance()
            return num
        } else if(currentToken?.symbol == RPAREN && pair == true){
            pair = false
            return dictionaryPairs[name]?.second ?: 0.0
        } else if (currentToken?.symbol == IDENTIFIER) {
            name = currentToken?.lexeme
            advance()
            if (name in dictionaryNum) {
                return dictionaryNum[name]
            } else if (name in dictionaryPairs){
                pair = true
                return dictionaryPairs[name]?.first ?: 0.0
            }else throw error("Invalid identifier '$name'")
        } else if (currentToken?.lexeme == "(") {
            advance()
            val value = Additive()
            if (currentToken?.lexeme == ",") {
                advance()
                return value
            }
            if (currentToken?.lexeme == ")") {
                advance()
                return value
            } else throw error("Unmatched parenthesis")

        } else throw error("Invalid primary")
    }

    private fun CityBlock(): City {
        if (currentToken?.lexeme == "city") {
            advance()
            val city = City()
            return city
        } else throw error("Invalid city block")
    }

    private fun City(): City {
        if (currentToken?.symbol == IDENTIFIER) {
            val name = currentToken?.lexeme
            advance()
            val body = Body()
            val city = name?.let { City(it, body) }
            return city!!
        } else throw error("Invalid syntax city.")
    }

    private fun Body(): List<Elt> {
        if (currentToken?.symbol == LCURLY) {
            advance()
            val elts = Elements()
            if (currentToken?.symbol == RCURLY) {
                advance()
                return elts
            } else throw error("Unmatched parenthesis")
        } else throw error("Invalid city body")
    }

    private fun Elements(): List<Elt> {
        val list = mutableListOf<Elt>()
        val element = Element()
        list.add(element)
        val tail = ElementsTail()
        list.addAll(tail)
        return list
    }

    private fun ElementsTail(): MutableList<Elt> {
        val list = mutableListOf<Elt>()
        if (currentToken?.symbol == RCURLY) {
            return list
        }
        var element = Element()
        list.add(element)
        if (currentToken?.lexeme == "building" || currentToken?.lexeme == "road" || currentToken?.lexeme == "park" || currentToken?.lexeme == "river" || currentToken?.lexeme == "tree"  || currentToken?.lexeme == "junction" || currentToken?.lexeme == "marker" || currentToken?.lexeme == "parking") {
            val tail = ElementsTail()
            list.addAll(tail)
            return list
        } else return list
    }

    private fun Element(): Elt {
        val elt = Block()
        return elt

    }

    private fun Block(): Elt {
        if (currentToken?.lexeme == "building" || currentToken?.lexeme == "road" || currentToken?.lexeme == "park" || currentToken?.lexeme == "river" || currentToken?.lexeme == "marker" || currentToken?.lexeme == "parking") {
            val type = currentToken?.lexeme
            advance()
            if (currentToken?.symbol == IDENTIFIER) {
                val name = currentToken?.lexeme
                advance()
                if (currentToken?.symbol == LCURLY) {
                    advance()
                    val list = BlockStatementList()
                    if (currentToken?.symbol == RCURLY) {
                        advance()
                        if (currentToken?.symbol == SEMICOLON) {
                            advance()
                            if (type != null && name != null && list.isNotEmpty()) {
                                val el = Elt(type, name, list)
                                return el
                            } else throw error("Invalid element")
                        } else throw error("Missing semicolon")
                    } else throw error("Unmatched parenthesis")
                } else throw error("Missing parenthesis")
            } else throw error("Invalid element")
        } else if (currentToken?.lexeme == "tree") {
            val type = currentToken?.lexeme
            advance()
            if (currentToken?.symbol == LCURLY) {
                advance()
                val list = BlockStatementList()
                if (currentToken?.symbol == RCURLY) {
                    advance()
                    if (currentToken?.symbol == SEMICOLON) {
                        advance()
                        val name = "/"
                        if (type != null && list.isNotEmpty()) {
                            val el = Elt(type, name, list)
                            return el
                        } else throw error("Invalid element")
                    } else throw error("Missing semicolon")
                } else throw error("Unmatched parenthesis")
            } else throw error("Missing parenthesis")
        } else if (currentToken?.lexeme == "junction") {
            val type = currentToken?.lexeme
            advance()
            if (currentToken?.symbol == LCURLY) {
                advance()
                val list = BlockStatementList()
                if (currentToken?.symbol == RCURLY) {
                    advance()
                    if (currentToken?.symbol == SEMICOLON) {
                        advance()
                        val name = "/"
                        if (type != null && list.isNotEmpty()) {
                            val el = Elt(type, name, list)
                            return el
                        } else throw error("Invalid element")
                    } else throw error("Missing semicolon")
                } else throw error("Unmatched parenthesis")
            } else throw error("Missing parenthesis")
        } else throw error("Invalid element")
    }

    private fun BlockStatementList(): List<Commands?> {
        val list = mutableListOf<Commands?>()
        list.add(BlockStatement())
        if (currentToken?.lexeme == "line" || currentToken?.lexeme == "bend" || currentToken?.lexeme == "box" || currentToken?.lexeme == "circ" || currentToken?.lexeme == "polyline" || currentToken?.lexeme == "polyspline" || currentToken?.lexeme == "mark") {
            list.addAll(BlockStatementList())
        }
        return list
    }

    private fun BlockStatement(): Commands? {
        while (currentToken?.lexeme == "let") {
            val let = Let()
        }
        if (currentToken?.lexeme == "line" || currentToken?.lexeme == "bend" || currentToken?.lexeme == "box" || currentToken?.lexeme == "circ" || currentToken?.lexeme == "polyline" || currentToken?.lexeme == "polyspline" || currentToken?.lexeme == "mark") {
            return Command()
        } else throw error("Invalid block statement")
    }

    private fun BlockTypeWid(): String? {
        if (currentToken?.lexeme == "building") {
            val identifier = currentToken?.lexeme
            advance()
            return identifier
        } else if (currentToken?.lexeme == "road") {
            val identifier = currentToken?.lexeme
            advance()
            return identifier
        } else if (currentToken?.lexeme == "park") {
            val identifier = currentToken?.lexeme
            advance()
            return identifier
        } else if (currentToken?.lexeme == "river") {
            val identifier = currentToken?.lexeme
            advance()
            return identifier
        } else throw error("Invalid block type")
    }

    private fun Command(): Commands? {
        val type = CommandType()
        val params = Parameter()
        val command = type?.let { Commands(it, params) }
        return command
    }

    private fun CommandType(): String? {
        if (currentToken?.lexeme == "line") {
            val type = currentToken?.lexeme
            advance()
            return type
        } else if (currentToken?.lexeme == "bend") {
            val type = currentToken?.lexeme
            advance()
            return type
        } else if (currentToken?.lexeme == "box") {
            val type = currentToken?.lexeme
            advance()
            return type
        } else if (currentToken?.lexeme == "circ") {
            val type = currentToken?.lexeme
            advance()
            return type
        } else if (currentToken?.lexeme == "polyline") {
            val type = currentToken?.lexeme
            advance()
            return type
        } else if (currentToken?.lexeme == "polyspline") {
            val type = currentToken?.lexeme
            advance()
            return type
        } else if (currentToken?.lexeme == "mark") {
            val type = currentToken?.lexeme
            advance()
            return type
        } else throw error("Invalid command type")
    }

    fun parse(): City {
        val city = Program()
        return city
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
    val filename =
        "C:\\Users\\keser\\parkingmate-studentski-projekt\\git2\\parkingmate-studentski-projekt\\prevajanje\\src\\main\\input.txt"

    val file = File(filename)
    if (!file.exists()) {
        println("File not found: $filename")
        return
    }

    val inputStream = file.inputStream()
    val scanner = Scanner(CityAutomaton, inputStream)
    val parser = Parser(scanner)

    val result = parser.parse()
    println(result.toGeoJSON())

}
