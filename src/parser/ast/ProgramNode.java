package parser.ast;

import java.util.List;

public class ProgramNode extends ASTNode {
    public List<ASTNode> globalItems;

    public ProgramNode(List<ASTNode> globalItems, int line, int column) {
        super(line, column);
        this.globalItems = globalItems;
    }
}
