package analyzer;

import parser.ast.*;
import java.util.*;

public class SemanticAnalyzer {
    private Deque<Map<String, SymbolInfo>> symbolStack = new ArrayDeque<>();
    private List<String> errors = new ArrayList<>();
    private Deque<FunctionContext> functionStack = new ArrayDeque<>();

    public SemanticAnalyzer() {
        symbolStack.push(new HashMap<>());
        symbolStack.peek().put("bool", new SymbolInfo(SymbolKind.TYPE, "bool"));
        symbolStack.peek().put("int", new SymbolInfo(SymbolKind.TYPE, "int"));
        symbolStack.peek().put("float64", new SymbolInfo(SymbolKind.TYPE, "float64"));
        symbolStack.peek().put("string", new SymbolInfo(SymbolKind.TYPE, "string"));

        List<String> paramTypes = new ArrayList<>();
        paramTypes.add("string");
        symbolStack.peek().put("println", new SymbolInfo(SymbolKind.FUNCTION, "void", paramTypes));
        List<String> oneArgNumeric = new ArrayList<>();
        oneArgNumeric.add("int");
        symbolStack.peek().put("numToStr", new SymbolInfo(SymbolKind.FUNCTION, "string", oneArgNumeric));

        List<String> oneArgString = new ArrayList<>();
        oneArgString.add("string");
        symbolStack.peek().put("strToNum", new SymbolInfo(SymbolKind.FUNCTION, "float64", oneArgString));
    }


    public void analyze(ProgramNode program) {
        visitProgram(program);
    }

    private void visitProgram(ProgramNode program) {
        for (ASTNode item : program.globalItems) {
            visitNode(item);
        }
    }

    private void visitNode(ASTNode node) {
        if (node instanceof VariableDeclarationNode) {
            visitVariableDeclaration((VariableDeclarationNode) node);
        } else if (node instanceof ConstantDeclarationNode) {
            visitConstantDeclaration((ConstantDeclarationNode) node);
        } else if (node instanceof TypeDeclarationNode) {
            visitTypeDeclaration((TypeDeclarationNode) node);
        } else if (node instanceof FunctionDeclarationNode) {
            visitFunctionDeclaration((FunctionDeclarationNode) node);
        } else if (node instanceof BlockNode) {
            visitBlock((BlockNode) node);
        } else if (node instanceof IfNode) {
            visitIf((IfNode) node);
        } else if (node instanceof ForNode) {
            visitFor((ForNode) node);
        } else if (node instanceof RangeForNode) {
            visitRangeFor((RangeForNode) node);
        } else if (node instanceof ReturnNode) {
            visitReturn((ReturnNode) node);
        } else if (node instanceof ExpressionStatementNode) {
            visitExpressionStatement((ExpressionStatementNode) node);
        } else if (node instanceof ShortVarDeclNode) {
            visitShortVarDecl((ShortVarDeclNode) node);
        } else if (node instanceof AssignmentNode) {
            visitAssignment((AssignmentNode) node);
        } else if (node instanceof IncrementNode) {
            visitIncrement((IncrementNode) node);
        } else if (node instanceof DecrementNode) {
            visitDecrement((DecrementNode) node);
        } else if (node instanceof StatementNode) {

        } else if (node instanceof ExpressionNode) {
            visitExpression((ExpressionNode) node);
        }
    }

    private void visitVariableDeclaration(VariableDeclarationNode node) {
        String varType = null;
        if (node.type == null) {
            if (!node.initExprs.isEmpty()) {
                String firstExprType = visitExpression(node.initExprs.get(0));
                varType = firstExprType;
            } else {
                reportError("Variable declaration without type and initialization not allowed", node.line, node.column);
                varType = "error";
            }
        } else {
            varType = resolveTypeNode(node.type);
        }

        for (String ident : node.idents) {
            if (symbolStack.peek().containsKey(ident)) {
                reportError("Variable '" + ident + "' is already declared", node.line, node.column);
            } else {
                symbolStack.peek().put(ident, new SymbolInfo(SymbolKind.VARIABLE, varType));
            }
        }

        if (!node.initExprs.isEmpty()) {
            if (node.initExprs.size() != node.idents.size()) {
                reportError("Number of expressions in initialization does not match number of variables", node.line, node.column);
            } else {
                for (int i = 0; i < node.idents.size(); i++) {
                    String varName = node.idents.get(i);
                    SymbolInfo info = symbolStack.peek().get(varName);
                    String exprType = visitExpression(node.initExprs.get(i));
                    if (!typeCompatible(info.type, exprType)) {
                        reportError("Type mismatch in variable initialization: expected " + info.type + ", got " + exprType, node.line, node.column);
                    }
                }
            }
        }
    }


    private void visitConstantDeclaration(ConstantDeclarationNode node) {
        if (!node.values.isEmpty() && node.values.size() != node.idents.size()) {
            reportError("Number of values does not match number of constants", node.line, node.column);
        }
        for (int i = 0; i < node.idents.size(); i++) {
            String ident = node.idents.get(i);
            if (symbolStack.peek().containsKey(ident)) {
                reportError("Constant '" + ident + "' is already declared", node.line, node.column);
            } else {
                String exprType = "error";
                if (!node.values.isEmpty()) {
                    exprType = visitExpression(node.values.get(i));
                } else {
                    reportError("Constant '" + ident + "' must have a value", node.line, node.column);
                }
                symbolStack.peek().put(ident, new SymbolInfo(SymbolKind.CONSTANT, exprType));
            }
        }
    }

    private void visitTypeDeclaration(TypeDeclarationNode node) {
        if (symbolStack.peek().containsKey(node.ident)) {
            reportError("Type '" + node.ident + "' is already declared", node.line, node.column);
        } else {
            String typeName = node.ident;
            symbolStack.peek().put(typeName, new SymbolInfo(SymbolKind.TYPE, typeName));
        }
    }

    private void visitFunctionDeclaration(FunctionDeclarationNode node) {
        if (symbolStack.peek().containsKey(node.name)) {
            reportError("Function '" + node.name + "' is already declared", node.line, node.column);
        } else {
            String returnType = node.returnType != null ? resolveTypeNode(node.returnType) : "void";
            List<String> paramTypes = new ArrayList<>();
            for (ParameterNode p : node.parameters) {
                paramTypes.add(resolveTypeNode(p.type));
            }
            symbolStack.peek().put(node.name, new SymbolInfo(SymbolKind.FUNCTION, returnType, paramTypes));
        }

        symbolStack.push(new HashMap<>());
        for (ParameterNode p : node.parameters) {
            String ptype = resolveTypeNode(p.type);
            symbolStack.peek().put(p.name, new SymbolInfo(SymbolKind.VARIABLE, ptype));
        }

        functionStack.push(new FunctionContext(node.name, (node.returnType != null ? resolveTypeNode(node.returnType) : "void")));
        visitBlock(node.body);
        functionStack.pop();

        symbolStack.pop();
    }

    private void visitBlock(BlockNode node) {
        symbolStack.push(new HashMap<>());
        for (StatementNode stmt : node.statements) {
            visitNode(stmt);
        }
        symbolStack.pop();
    }

    private void visitIf(IfNode node) {
        String condType = visitExpression(node.condition);
        if (!condType.equals("bool")) {
            reportError("If condition must be boolean", node.line, node.column);
        }

        visitNode(node.ifBlock);
        if (node.elseBlockOrIf != null) {
            visitNode(node.elseBlockOrIf);
        }
    }

    private void visitFor(ForNode node) {
        if (node.condition != null) {
            String condType = visitExpression(node.condition);
            if (!condType.equals("bool")) {
                reportError("For condition must be boolean", node.line, node.column);
            }
        }
        visitBlock(node.body);
    }

    private void visitRangeFor(RangeForNode node) {
        String containerType = visitExpression(node.rangeExpr);
        boolean isArray = containerType.startsWith("array_of_") || containerType.startsWith("slice_of_");
        if (!isArray) {
            reportError("Range expression must be array or slice", node.line, node.column);
        }

        String elementType;
        if (containerType.startsWith("array_of_")) {
            elementType = containerType.substring("array_of_".length());
        } else if (containerType.startsWith("slice_of_")) {
            elementType = containerType.substring("slice_of_".length());
        } else {
            elementType = "error";
        }

        symbolStack.push(new HashMap<>());
        if (node.idents.size() == 1) {
            String valName = node.idents.get(0);
            symbolStack.peek().put(valName, new SymbolInfo(SymbolKind.VARIABLE, elementType));
        } else if (node.idents.size() == 2) {
            String indexName = node.idents.get(0);
            String valName = node.idents.get(1);
            symbolStack.peek().put(indexName, new SymbolInfo(SymbolKind.VARIABLE, "int"));
            symbolStack.peek().put(valName, new SymbolInfo(SymbolKind.VARIABLE, elementType));
        }

        visitBlock(node.body);
        symbolStack.pop();
    }


    private void visitReturn(ReturnNode node) {
        String expectedType = "void";
        if (!functionStack.isEmpty()) {
            expectedType = functionStack.peek().returnType;
        }

        if (expectedType.equals("void")) {
            if (!node.exprs.isEmpty()) {
                reportError("Return with value in void function", node.line, node.column);
            }
        } else {
            if (node.exprs.isEmpty()) {
                reportError("Return without value in non-void function", node.line, node.column);
            } else if (node.exprs.size() == 1) {
                String actualType = visitExpression(node.exprs.get(0));
                if (!typeCompatible(expectedType, actualType)) {
                    reportError("Return type mismatch. Expected " + expectedType + ", got " + actualType, node.line, node.column);
                }
            } else {
                reportError("Multiple return values not supported in this simplified model", node.line, node.column);
            }
        }
    }

    private String visitCompositeLiteral(CompositeLiteralNode node) {
        String elemType = resolveTypeNode(node.elemType);
        if (node.length == null) {
            for (ExpressionNode e : node.elements) {
                String t = visitExpression(e);
                if (!typeCompatible(elemType, t)) {
                    reportError("Element type mismatch in slice literal. Expected " + elemType + ", got " + t, node.line, node.column);
                }
            }
            return "slice_of_" + elemType;
        } else {
            if (node.elements.size() != node.length) {
                reportError("Array literal length mismatch. Expected " + node.length + " elements, got " + node.elements.size(), node.line, node.column);
            }
            for (ExpressionNode e : node.elements) {
                String t = visitExpression(e);
                if (!typeCompatible(elemType, t)) {
                    reportError("Element type mismatch in array literal. Expected " + elemType + ", got " + t, node.line, node.column);
                }
            }
            return "array_of_" + elemType;
        }
    }


    private void visitExpressionStatement(ExpressionStatementNode node) {
        visitExpression(node.expression);
    }

    private void visitShortVarDecl(ShortVarDeclNode node) {
        if (node.exprs.size() != node.lhs.size()) {
            reportError("Number of values does not match number of variables in short var decl", node.line, node.column);
        } else {
            for (int i = 0; i < node.lhs.size(); i++) {
                ExpressionNode lhsExpr = node.lhs.get(i);
                if (!(lhsExpr instanceof IdentNode)) {
                    reportError("Short var declaration requires identifiers on the left-hand side", node.line, node.column);
                    continue;
                }
                IdentNode idNode = (IdentNode) lhsExpr;
                String ident = idNode.name;
                if (symbolStack.peek().containsKey(ident)) {
                    reportError("Variable '" + ident + "' is already declared", node.line, node.column);
                } else {
                    String exprType = visitExpression(node.exprs.get(i));
                    symbolStack.peek().put(ident, new SymbolInfo(SymbolKind.VARIABLE, exprType));
                }
            }
        }
    }

    private void visitAssignment(AssignmentNode node) {
        if (node.values.size() != node.lhs.size()) {
            reportError("Number of values does not match number of variables in assignment", node.line, node.column);
        } else {
            for (int i = 0; i < node.lhs.size(); i++) {
                ExpressionNode lhsExpr = node.lhs.get(i);
                String exprType = visitExpression(node.values.get(i));
                String lhsType = checkLhsAndGetType(lhsExpr, node.line, node.column);
                if (!typeCompatible(lhsType, exprType)) {
                    reportError("Type mismatch in assignment. Expected " + lhsType + ", got " + exprType, node.line, node.column);
                }
            }
        }
    }

    private String checkLhsAndGetType(ExpressionNode lhs, int line, int column) {
        if (lhs instanceof IdentNode) {
            IdentNode id = (IdentNode)lhs;
            SymbolInfo info = lookupSymbol(id.name);
            if (info == null) {
                reportError("Variable '" + id.name + "' not declared", line, column);
                return "error";
            }
            return info.type;
        } else if (lhs instanceof IndexNode) {
            IndexNode idx = (IndexNode)lhs;
            String containerType = visitExpression(idx.arrayOrSlice);
            String indexType = visitExpression(idx.indexExpr);
            return getElementTypeForIndexing(containerType, indexType, line, column);
        } else {
            reportError("Left-hand side of assignment must be a variable or an index", line, column);
            return "error";
        }
    }

    private String getElementTypeForIndexing(String containerType, String indexType, int line, int column) {
        if ((containerType.startsWith("array_of_") || containerType.startsWith("slice_of_")) && !indexType.equals("int")) {
            reportError("Index must be int for array/slice", line, column);
            return "error";
        }
        if (containerType.startsWith("array_of_")) {
            return containerType.substring("array_of_".length());
        } else if (containerType.startsWith("slice_of_")) {
            return containerType.substring("slice_of_".length());
        } else if (containerType.startsWith("map_of_")) {
            String[] mapParts = parseMapType(containerType);
            if (mapParts == null) {
                reportError("Invalid map type format: " + containerType, line, column);
                return "error";
            }
            String keyType = mapParts[0];
            String valueType = mapParts[1];
            if (!typeCompatible(keyType, indexType)) {
                reportError("Map index type mismatch. Expected " + keyType + ", got " + indexType, line, column);
                return "error";
            }
            return valueType;
        } else {
            reportError("Indexing applied to non-array/slice/map type", line, column);
            return "error";
        }
    }

    private void visitIncrement(IncrementNode node) {
        String exprType = visitExpression(node.expr);
        if (!exprType.equals("int") && !exprType.equals("float64")) {
            reportError("Increment operator can only be applied to numeric types", node.line, node.column);
        }
    }

    private void visitDecrement(DecrementNode node) {
        String exprType = visitExpression(node.expr);
        if (!exprType.equals("int") && !exprType.equals("float64")) {
            reportError("Decrement operator can only be applied to numeric types", node.line, node.column);
        }
    }

    private String visitExpression(ExpressionNode expr) {
        if (expr instanceof ValueNode) {
            return visitValueNode((ValueNode) expr);
        } else if (expr instanceof IdentNode) {
            return visitIdentNode((IdentNode) expr);
        } else if (expr instanceof UnaryOpNode) {
            return visitUnaryOp((UnaryOpNode) expr);
        } else if (expr instanceof BinaryOpNode) {
            return visitBinaryOp((BinaryOpNode) expr);
        } else if (expr instanceof CallNode) {
            return visitCall((CallNode) expr);
        } else if (expr instanceof IndexNode) {
            return visitIndex((IndexNode) expr);
        } else if (expr instanceof SelectorNode) {
            return visitSelector((SelectorNode) expr);
        } else if (expr instanceof CompositeLiteralNode) {
            return visitCompositeLiteral((CompositeLiteralNode) expr);
        }
        return "error";
    }

    private String visitValueNode(ValueNode node) {
        switch (node.valueType) {
            case INT: return "int";
            case FLOAT: return "float64";
            case STRING: return "string";
        }
        return "error";
    }

    private String visitIdentNode(IdentNode node) {
        SymbolInfo info = lookupSymbol(node.name);
        if (info == null) {
            reportError("Undeclared identifier '" + node.name + "'", node.line, node.column);
            return "error";
        }
        return info.type;
    }

    private String visitUnaryOp(UnaryOpNode node) {
        String exprType = visitExpression(node.expr);
        switch (node.op) {
            case PLUS:
            case MINUS:
                if (exprType.equals("int") || exprType.equals("float64")) {
                    return exprType;
                } else {
                    reportError("Unary +/- applied to non-numeric type", node.line, node.column);
                    return "error";
                }
            case NOT:
                if (exprType.equals("bool")) {
                    return "bool";
                } else {
                    reportError("Unary ! applied to non-boolean type", node.line, node.column);
                    return "error";
                }
        }
        return "error";
    }

    private String visitBinaryOp(BinaryOpNode node) {
        String leftType = visitExpression(node.left);
        String rightType = visitExpression(node.right);
        switch (node.op) {
            case PLUS:
            case MINUS:
            case MULT:
                if (leftType.equals("string") && rightType.equals("int")) {
                    return "string";
                }
            case DIV:
            case MOD:
                if ((isNumeric(leftType) && isNumeric(rightType)) ||
                        (node.op == BinaryOpNode.BinaryOp.PLUS && leftType.equals("string") && rightType.equals("string"))) {
                    if (leftType.equals("string") && rightType.equals("string")) {
                        return "string";
                    }
                    // numeric
                    if (leftType.equals("float64") || rightType.equals("float64"))
                        return "float64";
                    return "int";
                } else {
                    reportError("Arithmetic operation on non-numeric types", node.line, node.column);
                    return "error";
                }

            case EQ:
            case NEQ:
            case LT:
            case GT:
            case LEQ:
            case GEQ:
                if (typeCompatible(leftType, rightType)) {
                    return "bool";
                } else {
                    reportError("Comparison of different types", node.line, node.column);
                    return "error";
                }

            case AND:
            case OR:
                if (leftType.equals("bool") && rightType.equals("bool")) {
                    return "bool";
                } else {
                    reportError("Logical operation on non-boolean types", node.line, node.column);
                    return "error";
                }
        }
        return "error";
    }

    private String visitCall(CallNode node) {
        ExpressionNode funcExpr = node.function;
        String funcName = null;
        if (funcExpr instanceof IdentNode) {
            funcName = ((IdentNode)funcExpr).name;
        } else {
            reportError("Call on non-identifier function", node.line, node.column);
            return "error";
        }

        if (funcName.equals("println")) {
            for (ExpressionNode arg : node.arguments) {
                visitExpression(arg);
            }
            return "void";
        }

        SymbolInfo info = lookupSymbol(funcName);
        if (info == null || info.kind != SymbolKind.FUNCTION) {
            reportError("Called symbol '" + funcName + "' is not a function", node.line, node.column);
            return "error";
        }
        if (info.paramTypes.size() != node.arguments.size()) {
            reportError("Function '" + funcName + "' argument count mismatch", node.line, node.column);
            return "error";
        }
        for (int i = 0; i < node.arguments.size(); i++) {
            String argType = visitExpression(node.arguments.get(i));
            if (!typeCompatible(info.paramTypes.get(i), argType)) {
                reportError("Function '" + funcName + "' argument type mismatch. Expected "
                        + info.paramTypes.get(i) + ", got " + argType, node.line, node.column);
            }
        }
        return info.type;
    }


    private String visitIndex(IndexNode node) {
        String containerType = visitExpression(node.arrayOrSlice);
        String indexType = visitExpression(node.indexExpr);

        if ((containerType.startsWith("array_of_") || containerType.startsWith("slice_of_")) && !indexType.equals("int")) {
            reportError("Index must be int for array/slice", node.line, node.column);
            return "error";
        }

        if (containerType.startsWith("array_of_")) {
            return containerType.substring("array_of_".length());
        } else if (containerType.startsWith("slice_of_")) {
            return containerType.substring("slice_of_".length());
        } else if (containerType.startsWith("map_of_")) {
            String mapStr = containerType.substring("map_of_".length());
            // mapStr = "<K>_to_<V>"
            int toIndex = mapStr.indexOf("_to_");
            if (toIndex == -1) {
                reportError("Invalid map type format: " + containerType, node.line, node.column);
                return "error";
            }

            String keyType = mapStr.substring(0, toIndex); // K
            String valueType = mapStr.substring(toIndex + "_to_".length()); // V

            if (!typeCompatible(keyType, indexType)) {
                reportError("Map index type mismatch. Expected " + keyType + ", got " + indexType, node.line, node.column);
                return "error";
            }

            return valueType;
        } else {
            reportError("Indexing applied to non-array/slice/map type", node.line, node.column);
            return "error";
        }
    }


    private String visitSelector(SelectorNode node) {
        reportError("Selectors not supported or no struct type info", node.line, node.column);
        return "error";
    }

    private SymbolInfo lookupSymbol(String name) {
        for (Map<String, SymbolInfo> scope : symbolStack) {
            if (scope.containsKey(name)) {
                return scope.get(name);
            }
        }
        return null;
    }

    private boolean typeCompatible(String expected, String actual) {
        if (expected.equals("error") || actual.equals("error")) {
            return false;
        }

        if (expected.equals(actual)) return true;

        if ((isNumeric(expected) && isNumeric(actual))) {
            return true;
        }

        // array_of_...
        if (expected.startsWith("array_of_") && actual.startsWith("array_of_")) {
            String expElem = expected.substring("array_of_".length());
            String actElem = actual.substring("array_of_".length());
            return typeCompatible(expElem, actElem);
        }

        // slice_of_...
        if (expected.startsWith("slice_of_") && actual.startsWith("slice_of_")) {
            String expElem = expected.substring("slice_of_".length());
            String actElem = actual.substring("slice_of_".length());
            return typeCompatible(expElem, actElem);
        }

        // map_of_...
        if (expected.startsWith("map_of_") && actual.startsWith("map_of_")) {
            String[] expMapParts = parseMapType(expected);
            String[] actMapParts = parseMapType(actual);
            if (expMapParts == null || actMapParts == null) {
                return false;
            }

            return typeCompatible(expMapParts[0], actMapParts[0]) && typeCompatible(expMapParts[1], actMapParts[1]);
        }

        return false;
    }

    private String[] parseMapType(String mapType) {
        if (!mapType.startsWith("map_of_")) {
            return null;
        }
        String mapStr = mapType.substring("map_of_".length());
        int toIndex = mapStr.indexOf("_to_");
        if (toIndex == -1) {
            return null;
        }
        String keyType = mapStr.substring(0, toIndex);
        String valueType = mapStr.substring(toIndex + "_to_".length());
        return new String[]{keyType, valueType};
    }

    private boolean isNumeric(String type) {
        return type.equals("int") || type.equals("float64");
    }

    private String resolveTypeNode(TypeNode tnode) {
        if (tnode == null) return "var";
        if (tnode instanceof SimpleTypeNode) {
            return ((SimpleTypeNode)tnode).typeName;
        } else if (tnode instanceof ArrayTypeNode) {
            ArrayTypeNode at = (ArrayTypeNode)tnode;
            String elemType = resolveTypeNode(at.elementType);
            return "array_of_" + elemType;
        } else if (tnode instanceof SliceTypeNode) {
            SliceTypeNode st = (SliceTypeNode)tnode;
            return "slice_of_" + resolveTypeNode(st.elementType);
        } else if (tnode instanceof MapTypeNode) {
            MapTypeNode mt = (MapTypeNode)tnode;
            return "map_of_" + resolveTypeNode(mt.keyType) + "_to_" + resolveTypeNode(mt.valueType);
        }
        return "var";
    }

    private void reportError(String message, int line, int column) {
        errors.add("Semantic Error at line " + line + ", column " + column + ": " + message);
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public void printErrors() {
        for (String err : errors) {
            System.out.println(err);
        }
    }

    // Вспомогательный класс для хранения информации о символах
    private static class SymbolInfo {
        SymbolKind kind;
        String type;
        List<String> paramTypes; // для функций

        SymbolInfo(SymbolKind kind, String type) {
            this.kind = kind;
            this.type = type;
            this.paramTypes = new ArrayList<>();
        }

        SymbolInfo(SymbolKind kind, String type, List<String> paramTypes) {
            this.kind = kind;
            this.type = type;
            this.paramTypes = paramTypes;
        }
    }

    private enum SymbolKind {
        VARIABLE, CONSTANT, TYPE, FUNCTION
    }

    private static class FunctionContext {
        String name;
        String returnType;

        FunctionContext(String name, String returnType) {
            this.name = name;
            this.returnType = returnType;
        }
    }
}
