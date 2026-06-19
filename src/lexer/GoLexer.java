package lexer;

import java.util.ArrayList;
import java.util.List;

public class GoLexer {
    private final String input;
    private int pos;
    private int line;
    private int column;
    private char currentChar;

    private static final List<String> KEYWORDS = List.of(
            "const", "var", "type", "func", "if", "else", "for", "return", "range", "map", "int", "float64", "string"
    );

    private final List<String> errors;

    public GoLexer(String input) {
        this.input = input;
        this.pos = 0;
        this.line = 1;
        this.column = 1;
        this.currentChar = input.length() > 0 ? input.charAt(0) : '\0';
        this.errors = new ArrayList<>();
    }

    private void advance() {
        if (currentChar == '\n') {
            line++;
            column = 0;
        }
        pos++;
        column++;
        if (pos >= input.length()) {
            currentChar = '\0';
        } else {
            currentChar = input.charAt(pos);
        }
    }

    private void skipWhitespaceAndComments() {
        while (currentChar != '\0') {
            if (Character.isWhitespace(currentChar)) {
                advance();
            } else if (currentChar == '/' && peek() == '/') {
                advance(); // '/'
                advance(); // '/'
                while (currentChar != '\0' && currentChar != '\n') {
                    advance();
                }
            } else if (currentChar == '/' && peek() == '*') {
                // Многострочный комментарий
                advance(); // '/'
                advance(); // '*'
                while (currentChar != '\0') {
                    if (currentChar == '*' && peek() == '/') {
                        advance();
                        advance();
                        break;
                    }
                    advance();
                }
            } else {
                break;
            }
        }
    }

    private char peek() {
        int peekPos = pos + 1;
        if (peekPos >= input.length()) {
            return '\0';
        } else {
            return input.charAt(peekPos);
        }
    }

    public List<Token> tokenize() {
        List<Token> tokens = new ArrayList<>();
        while (currentChar != '\0') {
            skipWhitespaceAndComments();
            if (currentChar == '\0') break;

            Token token = nextToken();
            if (token != null) {
                tokens.add(token);
            }
        }
        tokens.add(new Token(TokenType.EOF, "", line, column));
        return tokens;
    }

    private Token nextToken() {
        if (Character.isLetter(currentChar) || currentChar == '_') {
            return matchIdentOrKeyword();
        }
        if (Character.isDigit(currentChar)) {
            return matchNumber();
        }
        if (currentChar == '"') {
            return matchString();
        }

        return matchOperatorOrDelimiter();
    }

    private Token matchIdentOrKeyword() {
        int startLine = line;
        int startColumn = column;
        StringBuilder sb = new StringBuilder();
        while (Character.isLetterOrDigit(currentChar) || currentChar == '_') {
            sb.append(currentChar);
            advance();
        }
        String value = sb.toString();
        if (KEYWORDS.contains(value)) {
            // int/float64/string
            switch (value) {
                case "int": return new Token(TokenType.INT_TYPE, value, startLine, startColumn);
                case "float64": return new Token(TokenType.FLOAT64_TYPE, value, startLine, startColumn);
                case "string": return new Token(TokenType.STRING_TYPE, value, startLine, startColumn);
                default:
                    return new Token(TokenType.valueOf(value.toUpperCase()), value, startLine, startColumn);
            }
        } else {
            return new Token(TokenType.IDENT, value, startLine, startColumn);
        }
    }

    private Token matchNumber() {
        int startLine = line;
        int startColumn = column;
        StringBuilder sb = new StringBuilder();
        boolean isFloat = false;

        while (Character.isDigit(currentChar)) {
            sb.append(currentChar);
            advance();
        }

        if (currentChar == '.') {
            isFloat = true;
            sb.append('.');
            advance();
            while (Character.isDigit(currentChar)) {
                sb.append(currentChar);
                advance();
            }
        }

        String numValue = sb.toString();
        if (isFloat) {
            return new Token(TokenType.FLOAT_NUMBER, numValue, startLine, startColumn);
        } else {
            return new Token(TokenType.INT_NUMBER, numValue, startLine, startColumn);
        }
    }

    private Token matchString() {
        int startLine = line;
        int startColumn = column;
        advance();
        StringBuilder sb = new StringBuilder();
        while (currentChar != '\0' && currentChar != '"') {
            if (currentChar == '\\' && peek() == '"') {
                advance();
                sb.append('"');
            } else {
                sb.append(currentChar);
            }
            advance();
        }

        if (currentChar == '"') {
            advance();
        } else {
            errors.add("Unterminated string at line " + startLine + ", column " + startColumn);
        }

        return new Token(TokenType.STRING, sb.toString(), startLine, startColumn);
    }

    private Token matchOperatorOrDelimiter() {
        int startLine = line;
        int startColumn = column;
        char c = currentChar;

        // :=, ==, !=, <=, >=, +=, -=, *=, /=, %=, &&, ||, ++, --
        String twoChars = "" + c + peek();
        switch (twoChars) {
            case ":=":
                advance(); advance();
                return new Token(TokenType.COLON_ASSIGN, ":=", startLine, startColumn);
            case "==":
                advance(); advance();
                return new Token(TokenType.EQUALS, "==", startLine, startColumn);
            case "!=":
                advance(); advance();
                return new Token(TokenType.NEQ, "!=", startLine, startColumn);
            case "<=":
                advance(); advance();
                return new Token(TokenType.LEQ, "<=", startLine, startColumn);
            case ">=":
                advance(); advance();
                return new Token(TokenType.GEQ, ">=", startLine, startColumn);
            case "+=":
                advance(); advance();
                return new Token(TokenType.PLUS_ASSIGN, "+=", startLine, startColumn);
            case "-=":
                advance(); advance();
                return new Token(TokenType.MINUS_ASSIGN, "-=", startLine, startColumn);
            case "*=":
                advance(); advance();
                return new Token(TokenType.MULT_ASSIGN, "*=", startLine, startColumn);
            case "/=":
                advance(); advance();
                return new Token(TokenType.DIV_ASSIGN, "/=", startLine, startColumn);
            case "%=":
                advance(); advance();
                return new Token(TokenType.MOD_ASSIGN, "%=", startLine, startColumn);
            case "&&":
                advance(); advance();
                return new Token(TokenType.AND_AND, "&&", startLine, startColumn);
            case "||":
                advance(); advance();
                return new Token(TokenType.OR_OR, "||", startLine, startColumn);
            case "++":
                advance(); advance();
                return new Token(TokenType.INC, "++", startLine, startColumn);
            case "--":
                advance(); advance();
                return new Token(TokenType.DEC, "--", startLine, startColumn);
        }

        switch (c) {
            case '=': advance(); return new Token(TokenType.ASSIGN, "=", startLine, startColumn);
            case '!': advance(); return new Token(TokenType.NOT, "!", startLine, startColumn);
            case '<': advance(); return new Token(TokenType.LT, "<", startLine, startColumn);
            case '>': advance(); return new Token(TokenType.GT, ">", startLine, startColumn);
            case '+': advance(); return new Token(TokenType.PLUS, "+", startLine, startColumn);
            case '-': advance(); return new Token(TokenType.MINUS, "-", startLine, startColumn);
            case '*': advance(); return new Token(TokenType.MULT, "*", startLine, startColumn);
            case '/': advance(); return new Token(TokenType.DIV, "/", startLine, startColumn);
            case '%': advance(); return new Token(TokenType.MOD, "%", startLine, startColumn);
            case ';': advance(); return new Token(TokenType.SEMICOLON, ";", startLine, startColumn);
            case ',': advance(); return new Token(TokenType.COMMA, ",", startLine, startColumn);
            case ':': advance(); return new Token(TokenType.COLON, ":", startLine, startColumn);
            case '.': advance(); return new Token(TokenType.DOT, ".", startLine, startColumn);
            case '(': advance(); return new Token(TokenType.LPAR, "(", startLine, startColumn);
            case ')': advance(); return new Token(TokenType.RPAR, ")", startLine, startColumn);
            case '{': advance(); return new Token(TokenType.LBRACE, "{", startLine, startColumn);
            case '}': advance(); return new Token(TokenType.RBRACE, "}", startLine, startColumn);
            case '[': advance(); return new Token(TokenType.LBRACK, "[", startLine, startColumn);
            case ']': advance(); return new Token(TokenType.RBRACK, "]", startLine, startColumn);
            default:
                errors.add("Unexpected character: " + c + " at line " + line + ", column " + column);
                advance();
                return null;
        }
    }

    public List<String> getErrors() {
        return errors;
    }
}
