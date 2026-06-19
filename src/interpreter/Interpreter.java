package interpreter;

import parser.ast.*;
import java.util.*;
import java.util.stream.Collectors;

public class Interpreter {
    private Deque<ExecutionContext> contextStack = new ArrayDeque<>();
    private Deque<FunctionInfo> functionStack = new ArrayDeque<>();

    public Interpreter() {
        contextStack.push(new ExecutionContext());
        Value printlnFunc = new Value(Value.Type.BUILTIN_FUNCTION, (BuiltinFunction)(args -> {
            System.out.println(args.stream().map(Value::toStringValue).collect(Collectors.joining(" ")));
            return Value.nullValue();
        }));
        contextStack.peek().declareVariable("println", printlnFunc);

        Value numToStrFunc = new Value(Value.Type.BUILTIN_FUNCTION, (BuiltinFunction)(args -> {
            if (args.size() != 1) {
                throw new RuntimeException("numToStr expects 1 argument");
            }
            Value v = args.get(0);
            if (!v.isNumeric()) {
                throw new RuntimeException("numToStr expects numeric argument");
            }
            return new Value(Value.Type.STRING, v.toStringValue());
        }));
        contextStack.peek().declareVariable("numToStr", numToStrFunc);

        Value strToNumFunc = new Value(Value.Type.BUILTIN_FUNCTION, (BuiltinFunction)(args -> {
            if (args.size() != 1) {
                throw new RuntimeException("strToNum expects 1 argument");
            }
            Value v = args.get(0);
            if (v.type != Value.Type.STRING) {
                throw new RuntimeException("strToNum expects string argument");
            }
            String s = (String)v.value;
            if (s.contains(".")) {
                try {
                    double d = Double.parseDouble(s);
                    return new Value(Value.Type.FLOAT, d);
                } catch (NumberFormatException e) {
                    throw new RuntimeException("Invalid float format in strToNum");
                }
            } else {
                try {
                    int n = Integer.parseInt(s);
                    return new Value(Value.Type.INT, n);
                } catch (NumberFormatException e) {
                    throw new RuntimeException("Invalid int format in strToNum");
                }
            }
        }));
        contextStack.peek().declareVariable("strToNum", strToNumFunc);

    }

    public void execProgram(ProgramNode program) {
        for (ASTNode node : program.globalItems) {
            execNode(node);
        }
    }

    private void execNode(ASTNode node) {
        if (node instanceof VariableDeclarationNode) {
            execVariableDeclaration((VariableDeclarationNode) node);
        } else if (node instanceof ConstantDeclarationNode) {
            execConstantDeclaration((ConstantDeclarationNode) node);
        } else if (node instanceof TypeDeclarationNode) {
            // Типы игнорируем на этапе исполнения
        } else if (node instanceof FunctionDeclarationNode) {
            execFunctionDeclaration((FunctionDeclarationNode) node);
        } else if (node instanceof BlockNode) {
            execBlock((BlockNode) node);
        } else if (node instanceof IfNode) {
            execIf((IfNode) node);
        } else if (node instanceof ForNode) {
            execFor((ForNode) node);
        } else if (node instanceof RangeForNode) {
            execRangeFor((RangeForNode)node);
        } else if (node instanceof ReturnNode) {
            throw new ReturnSignal(evalReturn((ReturnNode) node));
        } else if (node instanceof ExpressionStatementNode) {
            evalExpression(((ExpressionStatementNode)node).expression);
        } else if (node instanceof ShortVarDeclNode) {
            execShortVarDecl((ShortVarDeclNode) node);
        } else if (node instanceof AssignmentNode) {
            execAssignment((AssignmentNode) node);
        } else if (node instanceof IncrementNode) {
            execIncrement((IncrementNode) node);
        } else if (node instanceof DecrementNode) {
            execDecrement((DecrementNode) node);
        } else if (node instanceof BreakNode) {
            throw new BreakSignal();
        } else if (node instanceof ContinueNode) {
            throw new ContinueSignal();
        }
    }

    private void execBlock(BlockNode block) {
        contextStack.push(new ExecutionContext(contextStack.peek()));
        try {
            for (StatementNode stmt : block.statements) {
                execNode(stmt);
            }
        } catch (ReturnSignal | BreakSignal | ContinueSignal rs) {
            throw rs;
        } finally {
            contextStack.pop();
        }
    }

    private void execVariableDeclaration(VariableDeclarationNode node) {
        for (int i = 0; i < node.idents.size(); i++) {
            String ident = node.idents.get(i);
            Value val;
            if (node.type != null) {
                val = createDefaultValue(node.type);
            } else {
                if (node.initExprs.isEmpty()) {
                    val = Value.defaultValueForType(); // int=0
                } else {
                    val = Value.defaultValueForType();
                }
            }
            contextStack.peek().declareVariable(ident, val);
        }

        if (!node.initExprs.isEmpty()) {
            for (int i = 0; i < node.idents.size(); i++) {
                String ident = node.idents.get(i);
                Value val = evalExpression(node.initExprs.get(i));
                contextStack.peek().setVariable(ident, val);
            }
        }
    }

    private Value createDefaultValue(TypeNode typeNode) {
        if (typeNode == null) {
            return Value.defaultValueForType();
        } else if (typeNode instanceof SimpleTypeNode) {
            String t = ((SimpleTypeNode)typeNode).typeName;
            return createDefaultValueForType(t);
        } else if (typeNode instanceof ArrayTypeNode) {
            ArrayTypeNode at = (ArrayTypeNode)typeNode;
            int length = (at.length != null) ? at.length : 0;
            Value elemVal = createDefaultValue(at.elementType);
            List<Value> arr = new ArrayList<>();
            for (int i=0; i<length; i++) {
                arr.add(cloneValue(elemVal));
            }
            return new Value(Value.Type.ARRAY, arr);
        } else if (typeNode instanceof SliceTypeNode) {
            return new Value(Value.Type.ARRAY, new ArrayList<>());
        } else if (typeNode instanceof MapTypeNode) {
            throw new RuntimeException("map not supported yet");
        }
        return Value.nullValue();
    }

    private Value createDefaultValueForType(String t) {
        switch (t) {
            case "int": return new Value(Value.Type.INT, 0);
            case "float64": return new Value(Value.Type.FLOAT, 0.0);
            case "bool": return new Value(Value.Type.BOOL, false);
            case "string": return new Value(Value.Type.STRING, "");
        }
        return Value.nullValue();
    }

    private Value cloneValue(Value val) {
        switch (val.type) {
            case INT:
                return new Value(Value.Type.INT, val.value);
            case FLOAT:
                return new Value(Value.Type.FLOAT, val.value);
            case BOOL:
                return new Value(Value.Type.BOOL, val.value);
            case STRING:
                return new Value(Value.Type.STRING, val.value);
            case ARRAY:
                List<Value> oldArr = (List<Value>)val.value;
                List<Value> newArr = new ArrayList<>();
                for (Value v : oldArr) {
                    newArr.add(cloneValue(v));
                }
                return new Value(Value.Type.ARRAY, newArr);
            case NULL:
                return Value.nullValue();
            default:
                // FUNCTION, BUILTIN_FUNCTION
                return val;
        }
    }

    private void execConstantDeclaration(ConstantDeclarationNode node) {
        for (int i = 0; i < node.idents.size(); i++) {
            String ident = node.idents.get(i);
            Value val = evalExpression(node.values.get(i));
            contextStack.peek().declareVariable(ident, val);
        }
    }

    private void execFunctionDeclaration(FunctionDeclarationNode node) {
        Value funcVal = new Value(Value.Type.FUNCTION, node);
        contextStack.peek().declareVariable(node.name, funcVal);
    }

    private void execIf(IfNode node) {
        Value cond = evalExpression(node.condition);
        if (cond.toBoolean()) {
            execNode(node.ifBlock);
        } else if (node.elseBlockOrIf != null) {
            execNode(node.elseBlockOrIf);
        }
    }

    private void execFor(ForNode node) {
        while (true) {
            if (node.condition != null) {
                Value cond = evalExpression(node.condition);
                if (!cond.toBoolean()) break;
            }
            try {
                execBlock(node.body);
            } catch (ReturnSignal rs) {
                throw rs;
            } catch (BreakSignal bs) {
                break;
            } catch (ContinueSignal cs) {

            }
        }
    }

    private void execRangeFor(RangeForNode node) {
        Value container = evalExpression(node.rangeExpr);
        if (container.type != Value.Type.ARRAY) {
            throw new RuntimeException("Range expression must be array or slice");
        }
        List<Value> arr = (List<Value>)container.value;

        contextStack.push(new ExecutionContext(contextStack.peek()));
        try {
            if (node.idents.size() == 1) {
                String valName = node.idents.get(0);
                contextStack.peek().declareVariable(valName, Value.nullValue());
            } else {
                String indexName = node.idents.get(0);
                String valName = node.idents.get(1);
                contextStack.peek().declareVariable(indexName, new Value(Value.Type.INT, 0));
                contextStack.peek().declareVariable(valName, Value.nullValue());
            }

            for (int i = 0; i < arr.size(); i++) {
                if (node.idents.size() == 1) {
                    // value
                    String valName = node.idents.get(0);
                    contextStack.peek().setVariable(valName, arr.get(i));
                } else {
                    // index, value
                    String indexName = node.idents.get(0);
                    String valName = node.idents.get(1);
                    contextStack.peek().setVariable(indexName, new Value(Value.Type.INT, i));
                    contextStack.peek().setVariable(valName, arr.get(i));
                }

                try {
                    execBlock(node.body);
                } catch (ContinueSignal cs) {
                    // continue
                } catch (BreakSignal bs) {
                    // break
                    break;
                }
            }
        } catch (ReturnSignal rs) {
            throw rs;
        } finally {
            contextStack.pop();
        }
    }



    private List<Value> evalReturn(ReturnNode node) {
        List<Value> values = new ArrayList<>();
        for (ExpressionNode expr : node.exprs) {
            values.add(evalExpression(expr));
        }
        return values;
    }

    private void execShortVarDecl(ShortVarDeclNode node) {
        for (int i = 0; i < node.lhs.size(); i++) {
            ExpressionNode lhsExpr = node.lhs.get(i);
            if (!(lhsExpr instanceof IdentNode)) {
                throw new RuntimeException("Short var decl lhs is not an identifier");
            }
            IdentNode identNode = (IdentNode) lhsExpr;
            String ident = identNode.name;
            Value val = evalExpression(node.exprs.get(i));
            contextStack.peek().declareVariable(ident, val);
        }
    }

    private void execAssignment(AssignmentNode node) {
        for (int i = 0; i < node.lhs.size(); i++) {
            ExpressionNode lhsExpr = node.lhs.get(i);
            Value val = evalExpression(node.values.get(i));
            assignToLhs(lhsExpr, val);
        }
    }

    private void assignToLhs(ExpressionNode lhs, Value val) {
        if (lhs instanceof IdentNode) {
            String ident = ((IdentNode)lhs).name;
            contextStack.peek().setVariable(ident, val);
        } else if (lhs instanceof IndexNode) {
            IndexNode idxNode = (IndexNode) lhs;
            Value container = evalExpression(idxNode.arrayOrSlice);
            Value indexVal = evalExpression(idxNode.indexExpr);

            if (container.type == Value.Type.ARRAY) {
                if (indexVal.type != Value.Type.INT) {
                    throw new RuntimeException("Index must be int for array");
                }
                int index = (Integer)indexVal.value;
                List<Value> arr = (List<Value>)container.value;
                if (index < 0 || index >= arr.size()) {
                    throw new RuntimeException("Array index out of bounds");
                }
                arr.set(index, val);
            } else {
                throw new RuntimeException("Indexing assigned to non-array/slice/map value");
            }
        } else {
            throw new RuntimeException("Left-hand side of assignment is not assignable");
        }
    }

    private void execIncrement(IncrementNode node) {
        if (node.expr instanceof IdentNode) {
            String name = ((IdentNode)node.expr).name;
            Value val = contextStack.peek().getVariable(name);
            if (val.type == Value.Type.INT) {
                val = new Value(Value.Type.INT, (Integer)val.value + 1);
            } else if (val.type == Value.Type.FLOAT) {
                val = new Value(Value.Type.FLOAT, ((Double)val.value) + 1.0);
            } else {
                throw new RuntimeException("Increment on non-numeric value");
            }
            contextStack.peek().setVariable(name, val);
        } else {
            throw new RuntimeException("Increment can only be applied to a variable");
        }
    }

    private Value evalCompositeLiteral(CompositeLiteralNode node) {
        List<Value> vals = new ArrayList<>();
        for (ExpressionNode e : node.elements) {
            vals.add(evalExpression(e));
        }

        if (node.length == null) {
            return new Value(Value.Type.ARRAY, vals);
        } else {
            return new Value(Value.Type.ARRAY, vals);
        }
    }


    private void execDecrement(DecrementNode node) {
        if (node.expr instanceof IdentNode) {
            String name = ((IdentNode)node.expr).name;
            Value val = contextStack.peek().getVariable(name);
            if (val.type == Value.Type.INT) {
                val = new Value(Value.Type.INT, (Integer)val.value - 1);
            } else if (val.type == Value.Type.FLOAT) {
                val = new Value(Value.Type.FLOAT, ((Double)val.value) - 1.0);
            } else {
                throw new RuntimeException("Decrement on non-numeric value");
            }
            contextStack.peek().setVariable(name, val);
        } else {
            throw new RuntimeException("Decrement can only be applied to a variable");
        }
    }

    private Value evalExpression(ExpressionNode expr) {
        if (expr instanceof ValueNode) {
            return evalValueNode((ValueNode)expr);
        } else if (expr instanceof IdentNode) {
            return contextStack.peek().getVariable(((IdentNode)expr).name);
        } else if (expr instanceof UnaryOpNode) {
            return evalUnaryOp((UnaryOpNode)expr);
        } else if (expr instanceof BinaryOpNode) {
            return evalBinaryOp((BinaryOpNode)expr);
        } else if (expr instanceof CallNode) {
            return evalCall((CallNode)expr);
        } else if (expr instanceof IndexNode) {
            return evalIndex((IndexNode)expr);
        } else if (expr instanceof CompositeLiteralNode) {
            return evalCompositeLiteral((CompositeLiteralNode)expr);
        } else if (expr instanceof SelectorNode) {
            throw new RuntimeException("Selector not implemented");
        }
        return Value.nullValue();
    }

    private Value evalValueNode(ValueNode node) {
        switch (node.valueType) {
            case INT:
                return new Value(Value.Type.INT, Integer.parseInt(node.value));
            case FLOAT:
                return new Value(Value.Type.FLOAT, Double.parseDouble(node.value));
            case STRING:
                return new Value(Value.Type.STRING, node.value);
        }
        return Value.nullValue();
    }

    private Value evalUnaryOp(UnaryOpNode node) {
        Value val = evalExpression(node.expr);
        switch (node.op) {
            case PLUS:
                return val;
            case MINUS:
                if (val.type == Value.Type.INT) {
                    return new Value(Value.Type.INT, -(Integer)val.value);
                } else if (val.type == Value.Type.FLOAT) {
                    return new Value(Value.Type.FLOAT, - (Double)val.value);
                } else {
                    throw new RuntimeException("Unary minus on non-numeric");
                }
            case NOT:
                if (val.type == Value.Type.BOOL) {
                    return new Value(Value.Type.BOOL, !(Boolean)val.value);
                } else {
                    throw new RuntimeException("NOT on non-boolean");
                }
        }
        return Value.nullValue();
    }

    private Value evalBinaryOp(BinaryOpNode node) {
        Value left = evalExpression(node.left);
        Value right = evalExpression(node.right);

        switch (node.op) {
            case PLUS:
                if (left.type == Value.Type.INT && right.type == Value.Type.INT) {
                    return new Value(Value.Type.INT, (Integer)left.value + (Integer)right.value);
                } else if (left.isNumeric() && right.isNumeric()) {
                    double l = left.toDouble();
                    double r = right.toDouble();
                    return new Value(Value.Type.FLOAT, l + r);
                } else if (left.type == Value.Type.STRING && right.type == Value.Type.STRING) {
                    return new Value(Value.Type.STRING, (String)left.value + (String)right.value);
                } else {
                    throw new RuntimeException("Invalid types for +");
                }
            case MINUS:
            case MULT:
                if (left.type == Value.Type.STRING && right.type == Value.Type.INT) {
                    StringBuilder sb = new StringBuilder();
                    int times = (Integer)right.value;
                    for (int k=0; k<times; k++) {
                        sb.append((String)left.value);
                    }
                    return new Value(Value.Type.STRING, sb.toString());
                }
            case DIV:
            case MOD:
                if (node.op == BinaryOpNode.BinaryOp.MULT && left.type == Value.Type.STRING && right.type == Value.Type.INT) {
                    StringBuilder sb = new StringBuilder();
                    int times = (Integer)right.value;
                    for (int k=0; k<times; k++) {
                        sb.append((String)left.value);
                    }
                    return new Value(Value.Type.STRING, sb.toString());
                }

                if (left.isNumeric() && right.isNumeric()) {
                    double l = left.toDouble();
                    double r = right.toDouble();
                    switch (node.op) {
                        case MINUS:
                            if (left.type==Value.Type.INT && right.type==Value.Type.INT)
                                return new Value(Value.Type.INT,(int)l-(int)r);
                            return new Value(Value.Type.FLOAT, l - r);
                        case MULT:
                            if (left.type==Value.Type.INT && right.type==Value.Type.INT)
                                return new Value(Value.Type.INT,(int)l*(int)r);
                            return new Value(Value.Type.FLOAT, l * r);
                        case DIV:
                            if (r == 0.0) throw new RuntimeException("Division by zero");
                            return new Value(Value.Type.FLOAT, l / r);
                        case MOD:
                            if (left.type==Value.Type.INT && right.type==Value.Type.INT) {
                                return new Value(Value.Type.INT, (int)l % (int)r);
                            } else {
                                throw new RuntimeException("Mod only for integers");
                            }
                    }
                } else {
                    throw new RuntimeException("Arithmetic operation on non-numeric");
                }
                break;
            case EQ:
                return new Value(Value.Type.BOOL, left.equalsValue(right));
            case NEQ:
                return new Value(Value.Type.BOOL, !left.equalsValue(right));
            case LT:
            case GT:
            case LEQ:
            case GEQ:
                if (left.isNumeric() && right.isNumeric()) {
                    double l = left.toDouble();
                    double r = right.toDouble();
                    switch (node.op) {
                        case LT: return new Value(Value.Type.BOOL, l < r);
                        case GT: return new Value(Value.Type.BOOL, l > r);
                        case LEQ: return new Value(Value.Type.BOOL, l <= r);
                        case GEQ: return new Value(Value.Type.BOOL, l >= r);
                    }
                } else if (left.type == Value.Type.STRING && right.type == Value.Type.STRING) {
                    int cmp = ((String)left.value).compareTo((String)right.value);
                    switch (node.op) {
                        case LT: return new Value(Value.Type.BOOL, cmp < 0);
                        case GT: return new Value(Value.Type.BOOL, cmp > 0);
                        case LEQ: return new Value(Value.Type.BOOL, cmp <= 0);
                        case GEQ: return new Value(Value.Type.BOOL, cmp >= 0);
                    }
                } else {
                    throw new RuntimeException("Comparison on non-comparable types");
                }
                break;
            case AND:
            case OR:
                if (left.type == Value.Type.BOOL && right.type == Value.Type.BOOL) {
                    boolean lb = (Boolean)left.value;
                    boolean rb = (Boolean)right.value;
                    return new Value(Value.Type.BOOL, node.op == BinaryOpNode.BinaryOp.AND ? (lb && rb) : (lb || rb));
                } else {
                    throw new RuntimeException("Logical op on non-boolean");
                }
        }

        return Value.nullValue();
    }

    private Value evalCall(CallNode node) {
        Value funcVal = evalExpression(node.function);
        List<Value> args = new ArrayList<>();
        for (ExpressionNode e : node.arguments) {
            args.add(evalExpression(e));
        }

        if (funcVal.type == Value.Type.FUNCTION) {
            FunctionDeclarationNode funcNode = (FunctionDeclarationNode) funcVal.value;
            if (funcNode.parameters.size() != args.size()) {
                throw new RuntimeException("Argument count mismatch");
            }

            contextStack.push(new ExecutionContext(contextStack.peek()));
            try {
                for (int i = 0; i < funcNode.parameters.size(); i++) {
                    ParameterNode p = funcNode.parameters.get(i);
                    contextStack.peek().declareVariable(p.name, args.get(i));
                }

                execBlock(funcNode.body);
                return Value.nullValue();
            } catch (ReturnSignal rs) {
                return rs.values.isEmpty() ? Value.nullValue() : rs.values.get(0);
            } finally {
                contextStack.pop();
            }
        } else if (funcVal.type == Value.Type.BUILTIN_FUNCTION) {
            BuiltinFunction bf = (BuiltinFunction)funcVal.value;
            return bf.call(args);
        }
        else {
            throw new RuntimeException("Call on non-function");
        }
    }

    private Value evalIndex(IndexNode node) {
        Value arrVal = evalExpression(node.arrayOrSlice);
        Value idxVal = evalExpression(node.indexExpr);
        if (idxVal.type != Value.Type.INT) {
            throw new RuntimeException("Index must be int");
        }
        int index = (Integer)idxVal.value;
        if (arrVal.type == Value.Type.ARRAY) {
            List<Value> arr = (List<Value>)arrVal.value;
            if (index < 0 || index >= arr.size()) {
                throw new RuntimeException("Array index out of bounds");
            }
            return arr.get(index);
        } else {
            throw new RuntimeException("Indexing non-array value");
        }
    }

    private interface BuiltinFunction {
        Value call(List<Value> args);
    }

    private static class ReturnSignal extends RuntimeException {
        List<Value> values;
        ReturnSignal(List<Value> values) {
            this.values = values;
        }
    }

    private static class BreakSignal extends RuntimeException {
    }

    private static class ContinueSignal extends RuntimeException {
    }

    private static class FunctionInfo {
        String name;
        FunctionInfo(String name) {
            this.name = name;
        }
    }
}
