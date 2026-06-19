package parser.ast;

import java.util.List;

public class RangeForNode extends StatementNode {
    // ident_list:
    // for i := range arr {}
    // for i, val := range arr {}
    public List<String> idents;
    public ExpressionNode rangeExpr;
    public BlockNode body;

    public RangeForNode(List<String> idents, ExpressionNode rangeExpr, BlockNode body, int line, int column) {
        super(line, column);
        this.idents = idents;
        this.rangeExpr = rangeExpr;
        this.body = body;
    }
}
