package parser.ast;

public abstract class DeclarationNode extends StatementNode {
    public DeclarationNode(int line, int column) {
        super(line, column);
    }
}
