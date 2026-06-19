package parser.ast;

import java.util.List;

public class FunctionDeclarationNode extends DeclarationNode {
    public String name;
    public List<ParameterNode> parameters;
    public TypeNode returnType; // nullable
    public BlockNode body;

    public FunctionDeclarationNode(String name, List<ParameterNode> parameters, TypeNode returnType, BlockNode body, int line, int column) {
        super(line, column);
        this.name = name;
        this.parameters = parameters;
        this.returnType = returnType;
        this.body = body;
    }
}
