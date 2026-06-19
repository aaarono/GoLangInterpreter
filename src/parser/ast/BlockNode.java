package parser.ast;
import java.util.List;

public class BlockNode extends StatementNode {
    public List<StatementNode> statements;

    public BlockNode(List<StatementNode> statements, int line, int column) {
        super(line, column);
        this.statements = statements;
    }
}
