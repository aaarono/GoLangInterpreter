package parser;

import lexer.Token;
import lexer.TokenType;
import parser.ast.*;

import java.util.ArrayList;
import java.util.List;

public class GoParser {
    private List<Token> tokens;
    private int pos;

    public GoParser(List<Token> tokens) {
        this.tokens = tokens;
        this.pos = 0;
    }

    private Token currentToken() {
        if (pos < tokens.size()) {
            return tokens.get(pos);
        }
        return tokens.get(tokens.size()-1); // EOF
    }

    private Token advance() {
        if (pos < tokens.size()) pos++;
        return tokens.get(pos-1);
    }

    private boolean match(TokenType type) {
        if (check(type)) {
            advance();
            return true;
        }
        return false;
    }

    private Token expect(TokenType type, String errMsg) throws ParseException {
        if (check(type)) {
            return advance();
        } else {
            Token t = currentToken();
            throw new ParseException(errMsg + " at line " + t.getLine() + ", col " + t.getColumn());
        }
    }

    private boolean check(TokenType type) {
        return currentToken().getType() == type;
    }

    private Token peekToken() {
        if (pos+1 < tokens.size()) return tokens.get(pos+1);
        return tokens.get(tokens.size()-1);
    }

    public ProgramNode parseProgram() throws ParseException {
        int line = currentToken().getLine();
        int column = currentToken().getColumn();
        List<ASTNode> globalItems = new ArrayList<>();
        while (!check(TokenType.EOF)) {
            globalItems.add(parseGlobalItem());
        }
        return new ProgramNode(globalItems, line, column);
    }

    private ASTNode parseGlobalItem() throws ParseException {
        // global_item = declaration | statement
        if (check(TokenType.CONST)) {
            return parseConstantDeclaration();
        } else if (check(TokenType.VAR)) {
            return parseVariableDeclaration();
        } else if (check(TokenType.TYPE)) {
            return parseTypeDeclaration();
        } else if (check(TokenType.FUNC)) {
            return parseFunctionDeclaration();
        } else {
            return parseStatement();
        }
    }

    private DeclarationNode parseFunctionDeclaration() throws ParseException {
        Token start = expect(TokenType.FUNC, "Expected 'func'");
        Token nameToken = expect(TokenType.IDENT, "Expected function name");
        expect(TokenType.LPAR, "Expected '(' after function name");
        List<ParameterNode> params = new ArrayList<>();
        if (!check(TokenType.RPAR)) {
            params = parseParameterList();
        }
        expect(TokenType.RPAR, "Expected ')' after parameters");

        TypeNode returnType = null;
        if (isTypeStart()) {
            returnType = parseTypeSpecifier();
        }

        BlockNode body = parseBlock();
        return new FunctionDeclarationNode(nameToken.getText(), params, returnType, body, start.getLine(), start.getColumn());
    }

    private List<ParameterNode> parseParameterList() throws ParseException {
        List<ParameterNode> params = new ArrayList<>();
        params.add(parseParameter());
        while (match(TokenType.COMMA)) {
            params.add(parseParameter());
        }
        return params;
    }

    private ParameterNode parseParameter() throws ParseException {
        Token nameToken = expect(TokenType.IDENT, "Expected parameter name");
        TypeNode type = parseTypeSpecifier();
        return new ParameterNode(nameToken.getText(), type, nameToken.getLine(), nameToken.getColumn());
    }

    private DeclarationNode parseVariableDeclaration() throws ParseException {
        Token start = expect(TokenType.VAR, "Expected 'var'");
        List<String> idents = parseIdentList();
        TypeNode type = null;
        List<ExpressionNode> initExprs = new ArrayList<>();

        // variable_declaration_tail =
        //   type_init = type [ "=" expression_list ]
        // | init_only = "=" expression_list
        // | type_only = type

        if (check(TokenType.ASSIGN)) {
            // init_only
            advance();
            initExprs = parseExpressionList();
        } else if (isTypeStart()) {
            type = parseTypeSpecifier();
            if (match(TokenType.ASSIGN)) {
                initExprs = parseExpressionList();
            }
        } else {
            throw new ParseException("Expected type or '=' after var declaration");
        }

        return new VariableDeclarationNode(idents, type, initExprs, start.getLine(), start.getColumn());
    }

    private DeclarationNode parseConstantDeclaration() throws ParseException {
        Token start = expect(TokenType.CONST, "Expected 'const'");
        List<String> idents = parseIdentList();
        List<ExpressionNode> values = new ArrayList<>();
        if (match(TokenType.ASSIGN)) {
            values = parseExpressionList();
        }
        return new ConstantDeclarationNode(idents, values, start.getLine(), start.getColumn());
    }

    private DeclarationNode parseTypeDeclaration() throws ParseException {
        Token start = expect(TokenType.TYPE, "Expected 'type'");
        Token name = expect(TokenType.IDENT, "Expected type name");
        TypeNode type = parseTypeSpecifier();
        return new TypeDeclarationNode(name.getText(), type, start.getLine(), start.getColumn());
    }

    private List<String> parseIdentList() throws ParseException {
        List<String> idents = new ArrayList<>();
        Token nameToken = expect(TokenType.IDENT, "Expected identifier");
        idents.add(nameToken.getText());
        while (match(TokenType.COMMA)) {
            Token n = expect(TokenType.IDENT, "Expected identifier after comma");
            idents.add(n.getText());
        }
        return idents;
    }

    private List<ExpressionNode> parseExpressionList() throws ParseException {
        List<ExpressionNode> exprs = new ArrayList<>();
        exprs.add(parseExpression());
        while (match(TokenType.COMMA)) {
            exprs.add(parseExpression());
        }
        return exprs;
    }

    private boolean isTypeStart() {
        return check(TokenType.INT_TYPE) || check(TokenType.FLOAT64_TYPE) || check(TokenType.STRING_TYPE)
                || check(TokenType.IDENT) || check(TokenType.MAP) || check(TokenType.LBRACK);
    }

    private StatementNode parseStatement() throws ParseException {
        if (check(TokenType.VAR)) {
            return parseVariableDeclaration();
        } else if (check(TokenType.CONST)) {
            return parseConstantDeclaration();
        } else if (check(TokenType.TYPE)) {
            return parseTypeDeclaration();
        } else if (check(TokenType.IF)) {
            return parseIfStatement();
        } else if (check(TokenType.FOR)) {
            return parseForStatement();
        } else if (check(TokenType.RETURN)) {
            return parseReturnStatement();
        } else if (check(TokenType.LBRACE)) {
            return parseBlock();
        } else {
            return parseComplexSimpleStatement();
        }
    }

    private BlockNode parseBlock() throws ParseException {
        Token start = expect(TokenType.LBRACE, "Expected '{'");
        List<StatementNode> stmts = new ArrayList<>();
        while (!check(TokenType.RBRACE) && !check(TokenType.EOF)) {
            stmts.add(parseStatement());
        }
        expect(TokenType.RBRACE, "Expected '}'");
        return new BlockNode(stmts, start.getLine(), start.getColumn());
    }

    private StatementNode parseIfStatement() throws ParseException {
        Token start = expect(TokenType.IF, "Expected 'if'");
        ExpressionNode cond = parseExpression();
        BlockNode ifBlock = parseBlock();

        StatementNode elsePart = null;
        if (match(TokenType.ELSE)) {
            if (check(TokenType.IF)) {
                elsePart = parseIfStatement();
            } else {
                elsePart = parseBlock();
            }
        }

        return new IfNode(cond, ifBlock, elsePart, start.getLine(), start.getColumn());
    }

    private StatementNode parseForStatement() throws ParseException {
        Token start = expect(TokenType.FOR, "Expected 'for'");
        if (check(TokenType.IDENT)) {
            int savePos = pos;
            List<String> idents = new ArrayList<>();
            Token firstIdent = expect(TokenType.IDENT, "Expected identifier in range clause");
            idents.add(firstIdent.getText());

            if (match(TokenType.COMMA)) {
                Token secondIdent = expect(TokenType.IDENT, "Expected second identifier in range clause");
                idents.add(secondIdent.getText());
            }

            if (match(TokenType.COLON_ASSIGN)) {
                // for index, value := range expr
                expect(TokenType.RANGE, "Expected 'range' after ':='");
                ExpressionNode rangeExpr = parseExpression();
                BlockNode body = parseBlock();
                return new RangeForNode(idents, rangeExpr, body, start.getLine(), start.getColumn());
            } else {
                pos = savePos;
            }
        }

        ExpressionNode cond = null;
        if (!check(TokenType.LBRACE)) {
            cond = parseExpression();
        }
        BlockNode body = parseBlock();
        return new ForNode(cond, body, start.getLine(), start.getColumn());
    }


    private StatementNode parseReturnStatement() throws ParseException {
        Token start = expect(TokenType.RETURN, "Expected 'return'");
        List<ExpressionNode> exprs = new ArrayList<>();
        if (!check(TokenType.SEMICOLON) && !check(TokenType.RBRACE) && !check(TokenType.EOF) && !check(TokenType.ELSE)) {
            exprs = parseExpressionList();
        }

        return new ReturnNode(exprs, start.getLine(), start.getColumn());
    }

    private StatementNode parseSimpleStatement() throws ParseException {
        ExpressionNode expr = parseExpression();
        return new ExpressionStatementNode(expr, expr.line, expr.column);
    }

    // short var decl, assignment, increment/decrement, expression statement
    private StatementNode parseComplexSimpleStatement() throws ParseException {
        int startPos = pos;
        ExpressionNode firstExpr = parseExpression(); // arr[0][0]

        List<ExpressionNode> lhsList = new ArrayList<>();
        lhsList.add(firstExpr);
        while (match(TokenType.COMMA)) {
            lhsList.add(parseExpression());
        }

        if (match(TokenType.COLON_ASSIGN)) {
            // short var decl
            List<ExpressionNode> exprs = parseExpressionList();
            return new ShortVarDeclNode(lhsList, exprs, currentToken().getLine(), currentToken().getColumn());
        }
        else if (check(TokenType.ASSIGN) || check(TokenType.PLUS_ASSIGN) || check(TokenType.MINUS_ASSIGN)
                || check(TokenType.MULT_ASSIGN) || check(TokenType.DIV_ASSIGN) || check(TokenType.MOD_ASSIGN)) {
            Token op = advance();
            List<ExpressionNode> exprs = parseExpressionList();
            return new AssignmentNode(lhsList, op.getType(), exprs, op.getLine(), op.getColumn());
        } else if (match(TokenType.INC)) {
            if (lhsList.size() > 1) {
                throw new ParseException("Increment applies to only one variable");
            }
            return new IncrementNode(lhsList.get(0), lhsList.get(0).line, lhsList.get(0).column);
        } else if (match(TokenType.DEC)) {
            if (lhsList.size() > 1) {
                throw new ParseException("Decrement applies to only one variable");
            }
            return new DecrementNode(lhsList.get(0), lhsList.get(0).line, lhsList.get(0).column);
        } else {
            if (lhsList.size() > 1) {
                throw new ParseException("Multiple expressions not allowed in statement without assignment");
            }
            return new ExpressionStatementNode(firstExpr, firstExpr.line, firstExpr.column);
        }
    }


    private ExpressionNode parseExpression() throws ParseException {
        return parseOrExpr();
    }

    private ExpressionNode parseOrExpr() throws ParseException {
        ExpressionNode left = parseAndExpr();
        while (match(TokenType.OR_OR)) {
            Token op = tokens.get(pos-1);
            ExpressionNode right = parseAndExpr();
            left = new BinaryOpNode(left, BinaryOpNode.BinaryOp.OR, right, op.getLine(), op.getColumn());
        }
        return left;
    }

    private ExpressionNode parseAndExpr() throws ParseException {
        ExpressionNode left = parseEqualityExpr();
        while (match(TokenType.AND_AND)) {
            Token op = tokens.get(pos-1);
            ExpressionNode right = parseEqualityExpr();
            left = new BinaryOpNode(left, BinaryOpNode.BinaryOp.AND, right, op.getLine(), op.getColumn());
        }
        return left;
    }

    private ExpressionNode parseEqualityExpr() throws ParseException {
        ExpressionNode left = parseRelationalExpr();
        while (check(TokenType.EQUALS) || check(TokenType.NEQ)) {
            Token op = advance();
            ExpressionNode right = parseRelationalExpr();
            BinaryOpNode.BinaryOp bop = op.getType() == TokenType.EQUALS ? BinaryOpNode.BinaryOp.EQ : BinaryOpNode.BinaryOp.NEQ;
            left = new BinaryOpNode(left, bop, right, op.getLine(), op.getColumn());
        }
        return left;
    }

    private ExpressionNode parseRelationalExpr() throws ParseException {
        ExpressionNode left = parseAddExpr();
        while (check(TokenType.LT) || check(TokenType.GT) || check(TokenType.LEQ) || check(TokenType.GEQ)) {
            Token op = advance();
            BinaryOpNode.BinaryOp bop;
            switch (op.getType()) {
                case LT: bop = BinaryOpNode.BinaryOp.LT; break;
                case GT: bop = BinaryOpNode.BinaryOp.GT; break;
                case LEQ: bop = BinaryOpNode.BinaryOp.LEQ; break;
                case GEQ: bop = BinaryOpNode.BinaryOp.GEQ; break;
                default: throw new ParseException("Unexpected operator");
            }
            ExpressionNode right = parseAddExpr();
            left = new BinaryOpNode(left, bop, right, op.getLine(), op.getColumn());
        }
        return left;
    }

    private ExpressionNode parseAddExpr() throws ParseException {
        ExpressionNode left = parseMultExpr();
        while (check(TokenType.PLUS) || check(TokenType.MINUS)) {
            Token op = advance();
            BinaryOpNode.BinaryOp bop = (op.getType() == TokenType.PLUS) ? BinaryOpNode.BinaryOp.PLUS : BinaryOpNode.BinaryOp.MINUS;
            ExpressionNode right = parseMultExpr();
            left = new BinaryOpNode(left, bop, right, op.getLine(), op.getColumn());
        }
        return left;
    }

    private ExpressionNode parseMultExpr() throws ParseException {
        ExpressionNode left = parseUnaryExpr();
        while (check(TokenType.MULT) || check(TokenType.DIV) || check(TokenType.MOD)) {
            Token op = advance();
            BinaryOpNode.BinaryOp bop;
            switch (op.getType()) {
                case MULT: bop = BinaryOpNode.BinaryOp.MULT; break;
                case DIV: bop = BinaryOpNode.BinaryOp.DIV; break;
                case MOD: bop = BinaryOpNode.BinaryOp.MOD; break;
                default: throw new ParseException("Unexpected operator");
            }
            ExpressionNode right = parseUnaryExpr();
            left = new BinaryOpNode(left, bop, right, op.getLine(), op.getColumn());
        }
        return left;
    }

    private ExpressionNode parseUnaryExpr() throws ParseException {
        if (check(TokenType.PLUS) || check(TokenType.MINUS) || check(TokenType.NOT)) {
            Token op = advance();
            UnaryOpNode.UnaryOp uop;
            switch (op.getType()) {
                case PLUS: uop = UnaryOpNode.UnaryOp.PLUS; break;
                case MINUS: uop = UnaryOpNode.UnaryOp.MINUS; break;
                case NOT: uop = UnaryOpNode.UnaryOp.NOT; break;
                default: throw new ParseException("Unknown unary op");
            }
            ExpressionNode expr = parseUnaryExpr();
            return new UnaryOpNode(uop, expr, op.getLine(), op.getColumn());
        } else {
            return parsePostfixExpr();
        }
    }

    private ExpressionNode parsePostfixExpr() throws ParseException {
        ExpressionNode expr = parsePrimaryExpr();
        boolean done = false;
        while (!done) {
            if (match(TokenType.DOT)) {
                Token field = expect(TokenType.IDENT, "Expected field name after '.'");
                expr = new SelectorNode(expr, field.getText(), field.getLine(), field.getColumn());
            } else if (match(TokenType.LBRACK)) {
                ExpressionNode indexExpr = parseExpression();
                expect(TokenType.RBRACK, "Expected ']' after index");
                expr = new IndexNode(expr, indexExpr, expr.line, expr.column);
            } else if (match(TokenType.LPAR)) {
                // function call
                List<ExpressionNode> args = new ArrayList<>();
                if (!check(TokenType.RPAR)) {
                    args = parseExpressionList();
                }
                expect(TokenType.RPAR, "Expected ')' after arguments");
                expr = new CallNode(expr, args, expr.line, expr.column);
            } else {
                done = true;
            }
        }
        return expr;
    }

    private ExpressionNode parsePrimaryExpr() throws ParseException {
        Token t = currentToken();
        if (check(TokenType.IDENT)) {
            advance();
            return new IdentNode(t.getText(), t.getLine(), t.getColumn());
        } else if (check(TokenType.INT_NUMBER)) {
            advance();
            return new ValueNode(ValueNode.ValueType.INT, t.getText(), t.getLine(), t.getColumn());
        } else if (check(TokenType.FLOAT_NUMBER)) {
            advance();
            return new ValueNode(ValueNode.ValueType.FLOAT, t.getText(), t.getLine(), t.getColumn());
        } else if (check(TokenType.STRING)) {
            advance();
            return new ValueNode(ValueNode.ValueType.STRING, t.getText(), t.getLine(), t.getColumn());
        } else if (match(TokenType.LPAR)) {
            ExpressionNode expr = parseExpression();
            expect(TokenType.RPAR, "Expected ')' after expression");
            return expr;
        } else if (check(TokenType.LBRACK)) {
            return parseCompositeLiteral();
        }
        else {
            throw new ParseException("Unexpected token in primary expression: " + t.getText());
        }
    }

    private ExpressionNode parseCompositeLiteral() throws ParseException {
        // composite_literal = literal_type literal_value
        // slice_literal = "[]" type "{" expression_list "}"
        // array_literal = "[" length? "]" type "{" expression_list "}"
        // Starts with '['
        Token startToken = currentToken();
        if (match(TokenType.LBRACK)) {
            if (match(TokenType.RBRACK)) {
                // slice
                TypeNode elemType = parseTypeSpecifier();
                // "{"
                expect(TokenType.LBRACE, "Expected '{' in slice literal");
                List<ExpressionNode> elements = new ArrayList<>();
                if (!check(TokenType.RBRACE)) {
                    elements = parseExpressionList();
                }
                expect(TokenType.RBRACE, "Expected '}' at end of slice literal");
                return new CompositeLiteralNode(elemType, elements, startToken.getLine(), startToken.getColumn());
            } else {
                // array
                Integer length = null;
                if (check(TokenType.INT_NUMBER)) {
                    Token num = advance();
                    length = Integer.parseInt(num.getText());
                }
                expect(TokenType.RBRACK, "Expected ']' in array literal");
                TypeNode elemType = parseTypeSpecifier();
                expect(TokenType.LBRACE, "Expected '{' in array literal");
                List<ExpressionNode> elements = new ArrayList<>();
                if (!check(TokenType.RBRACE)) {
                    elements = parseExpressionList();
                }
                expect(TokenType.RBRACE, "Expected '}' at end of array literal");
                return new CompositeLiteralNode(elemType, elements, startToken.getLine(), startToken.getColumn(), length);
            }
        } else {
            throw new ParseException("Expected '[' at start of composite literal");
        }
    }


    private TypeNode parseTypeSpecifier() throws ParseException {
        Token t = currentToken();
        if (match(TokenType.INT_TYPE) || match(TokenType.FLOAT64_TYPE) || match(TokenType.STRING_TYPE)) {
            return new SimpleTypeNode(t.getText(), t.getLine(), t.getColumn());
        } else if (check(TokenType.LBRACK)) {
            int line = t.getLine();
            int column = t.getColumn();
            advance(); // '['
            Integer length = null;
            if (check(TokenType.INT_NUMBER)) {
                Token num = advance();
                length = Integer.parseInt(num.getText());
            }
            expect(TokenType.RBRACK, "Expected ']' in array type");
            TypeNode elementType = parseTypeSpecifier();
            return new ArrayTypeNode(length, elementType, line, column);
        } else if (check(TokenType.MAP)) {
            Token start = advance(); // map
            expect(TokenType.LBRACK, "Expected '[' after 'map'");
            TypeNode keyType = parseTypeSpecifier();
            expect(TokenType.RBRACK, "Expected ']' after map key type");
            TypeNode valueType = parseTypeSpecifier();
            return new MapTypeNode(keyType, valueType, start.getLine(), start.getColumn());
        } else if (check(TokenType.LBRACK) && peekToken().getType() == TokenType.RBRACK) {
            // "[]" type
            int line = t.getLine();
            int column = t.getColumn();
            advance(); // '['
            expect(TokenType.RBRACK, "Expected ']' for slice type");
            TypeNode elem = parseTypeSpecifier();
            return new SliceTypeNode(elem, line, column);
        } else if (check(TokenType.IDENT)) {
            // type_name
            Token name = advance();
            return new SimpleTypeNode(name.getText(), name.getLine(), name.getColumn());
        } else {
            throw new ParseException("Expected a type specifier, got " + currentToken().getText());
        }
    }
}
