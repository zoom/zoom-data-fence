package us.zoom.data.dfence.sql.visitors.showgrantsprocedurename;

import java.util.List;

import org.antlr.v4.runtime.tree.ParseTree;

import us.zoom.data.dfence.sql.error.ParserException;
import us.zoom.data.dfence.sql.models.SqlDataType;
import us.zoom.data.dfence.sql.parser.ShowGrantsProcedureNameBaseVisitor;
import us.zoom.data.dfence.sql.parser.ShowGrantsProcedureNameParser;

public class SqlDataTypeGrantsProcedureNameVisitor
    extends ShowGrantsProcedureNameBaseVisitor<SqlDataType> {

  @Override
  public SqlDataType visitExpandedArgument(
      ShowGrantsProcedureNameParser.ExpandedArgumentContext ctx) {
    if (ctx.dataType() != null) {
      return ctx.dataType().accept(this);
    }
    throw new ParserException("Unable to parse argument name identifier" + ctx.getText());
  }

  @Override
  public SqlDataType visitDataType(ShowGrantsProcedureNameParser.DataTypeContext ctx) {
    SqlDataType sqlDataType = new SqlDataType();
    sqlDataType.setTypeName(ctx.STANDARD_UPPERCASE_IDENTIFIER().getText());
    if (ctx.dataTypeParams() != null
        && ctx.dataTypeParams().INTEGER() != null
        && !ctx.dataTypeParams().INTEGER().isEmpty()) {
      List<String> params =
          ctx.dataTypeParams().INTEGER().stream().map(ParseTree::getText).toList();
      sqlDataType.getParams().addAll(params);
    } else if (ctx.columnTypes() != null
        && ctx.columnTypes().dataType() != null
        && !ctx.columnTypes().dataType().isEmpty()) {
      List<SqlDataType> columnTypes =
          ctx.columnTypes().dataType().stream().map(x -> x.accept(this)).toList();
      sqlDataType.getColumnTypes().addAll(columnTypes);
    } else if (ctx.expandedArgs() != null
        && ctx.expandedArgs().expandedArgument() != null
        && !ctx.expandedArgs().expandedArgument().isEmpty()) {
      List<SqlDataType> expandedArgs =
          ctx.expandedArgs().expandedArgument().stream().map(x -> x.accept(this)).toList();
      sqlDataType.getColumnTypes().addAll(expandedArgs);
    }
    return sqlDataType;
  }
}
