package us.zoom.data.dfence.sql.visitors.objectname;

import org.antlr.v4.runtime.tree.ParseTree;

import us.zoom.data.dfence.exception.ObjectNameException;
import us.zoom.data.dfence.sql.models.SqlDataType;
import us.zoom.data.dfence.sql.parser.ObjectNameBaseVisitor;
import us.zoom.data.dfence.sql.parser.ObjectNameParser;

public class SqlDataTypeObjectNameVisitor extends ObjectNameBaseVisitor<SqlDataType> {

  @Override
  public SqlDataType visitArgument(ObjectNameParser.ArgumentContext ctx) {
    SqlDataType sqlDataType = new SqlDataType();
    if (ctx.IDENTIFIER() != null) {
      sqlDataType.setTypeName(ctx.IDENTIFIER().getText());
    } else {
      throw new ObjectNameException("Unable to parse argument name identifier" + ctx.getText());
    }
    if (ctx.argumentParams() != null
        && ctx.argumentParams().INTEGER() != null
        && !ctx.argumentParams().INTEGER().isEmpty()) {
      sqlDataType
          .getParams()
          .addAll(ctx.argumentParams().INTEGER().stream().map(ParseTree::getText).toList());
    }
    if (ctx.args() != null && ctx.args().argument() != null && !ctx.args().argument().isEmpty()) {
      sqlDataType
          .getColumnTypes()
          .addAll(ctx.args().argument().stream().map(x -> x.accept(this)).toList());
    }
    return sqlDataType;
  }
}
