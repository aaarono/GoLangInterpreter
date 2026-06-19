package parser.ast;

public class TypeDeclarationNode extends DeclarationNode {
    public String ident;
    public TypeNode typeSpecifier;

    public TypeDeclarationNode(String ident, TypeNode typeSpecifier, int line, int column) {
        super(line, column);
        this.ident = ident;
        this.typeSpecifier = typeSpecifier;
    }
}
