import lexer.GoLexer;
import lexer.Token;
import parser.GoParser;
import parser.ParseException;
import parser.ast.ProgramNode;
import analyzer.SemanticAnalyzer;
import interpreter.Interpreter;

import java.util.List;

public class Main {
    public static void main(String[] args) {
        String code = ""
                // Examples of variable declarations, arithmetic, and output
                + "var xx int = 10\n"
                + "var yy = 20\n"
                + "var zz int\n"
                + "zz = xx + yy * 2\n"
                + "println(\"xx =\", xx, \"yy =\", yy, \"xx + yy * 2 =\", zz)\n"
                + "\n"
                // Function max
                + "func max(a int, b int) int {\n"
                + "    if a > b {\n"
                + "        return a\n"
                + "    } else {\n"
                + "        return b\n"
                + "    }\n"
                + "}\n"
                + "println(\"max(3,5) =\", max(3,5))\n"
                + "println(\"max(10,7) =\", max(10,7))\n"
                + "\n"
                // Example with array (two-dimensional)
                + "var arr [2][3]int\n"
                + "arr[0][0] = 1\n"
                + "arr[0][1] = 2\n"
                + "arr[0][2] = 3\n"
                + "arr[1][0] = 4\n"
                + "arr[1][1] = 5\n"
                + "arr[1][2] = 6\n"
                + "println(\"arr[1][0] =\", arr[1][0])\n"
                + "\n"
                + "func sumArray(a [2][3]int) int {\n"
                + "    var total int = 0\n"
                + "    var i int = 0\n"
                + "    for i < 2 {\n"
                + "        var j int = 0\n"
                + "        for j < 3 {\n"
                + "            total = total + a[i][j]\n"
                + "            j++\n"
                + "        }\n"
                + "        i++\n"
                + "    }\n"
                + "    return total\n"
                + "}\n"
                + "println(\"sum of arr =\", sumArray(arr))\n"
                + "\n"
                // Working with strings
                + "var s = \"Hello\"\n"
                + "var t = \"World\"\n"
                + "println(\"s + t =\", s + \" \" + t)\n"
                + "println(\"Repeat A 3 times:\", \"A\" * 3)\n"
                + "\n"
                // Simple declaration using :=
                + "x := 10\n"
                + "y := 2.5\n"
                + "println(\"x =\", x, \", y =\", y)\n"
                + "\n"
                // Conversions numToStr, strToNum
                + "println(\"numToStr(x) =\", numToStr(x))\n"
                + "println(\"numToStr(y) =\", numToStr(y))\n"
                + "\n"
                + "var str = \"123\"\n"
                + "println(\"strToNum(str) + 10 =\", strToNum(str) + 10)\n"
                + "var fstr = \"45.67\"\n"
                + "println(\"strToNum(fstr) + 1.33 =\", strToNum(fstr) + 1.33)\n"
                + "\n"
                // Nested loops for complex calculations
                + "var res int = 0\n"
                + "var i int = 0\n"
                + "for i < 5 {\n"
                + "    var j int = 0\n"
                + "    for j < 3 {\n"
                + "        res = res + i*j\n"
                + "        j++\n"
                + "    }\n"
                + "    i++\n"
                + "}\n"
                + "println(\"res after nested loops =\", res)\n"
                + "\n"
                // Example with slice and for range
                + "var numbers = []int{1, 2, 3, 4, 5}\n"
                + "for index, value := range numbers {\n"
                + "    println(\"Index:\", index, \"Value:\", value)\n"
                + "}\n"
                ;

        try {
            GoLexer lexer = new GoLexer(code);
            List<Token> tokens = lexer.tokenize();
            if (!lexer.getErrors().isEmpty()) {
                System.out.println("Lexing errors:");
                for (String err : lexer.getErrors()) {
                    System.out.println(err);
                }
                return;
            }

            System.out.println("Tokens:");
            for (Token tk : tokens) {
                System.out.println(tk);
            }

            GoParser parser = new GoParser(tokens);
            ProgramNode program = parser.parseProgram();

            SemanticAnalyzer analyzer = new SemanticAnalyzer();
            analyzer.analyze(program);
            if (analyzer.hasErrors()) {
                analyzer.printErrors();
                System.out.println("Program has semantic errors and cannot be executed.");
                return;
            }

            Interpreter interpreter = new Interpreter();
            interpreter.execProgram(program);

        } catch (ParseException e) {
            System.out.println("Parse Error: " + e.getMessage());
        } catch (RuntimeException e) {
            System.out.println("Runtime Error: " + e.getMessage());
        }
    }
}
