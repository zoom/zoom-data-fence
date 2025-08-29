package us.zoom.data.dfence.sql.visitors.objectname;

import java.util.regex.Pattern;

import us.zoom.data.dfence.exception.ObjectNameException;
import us.zoom.data.dfence.sql.parser.ObjectNameBaseVisitor;
import us.zoom.data.dfence.sql.parser.ObjectNameParser;

public class PartObjectNameVisitor extends ObjectNameBaseVisitor<String> {

  private final Pattern objectNameIdentifierQuotedPattern =
      Pattern.compile("^\"[A-Z_][A-Z0-9_]+\"|[a-zA-Z_][A-Z0-9_]+$");

  @Override
  public String visitPart(ObjectNameParser.PartContext ctx) {
    if (ctx.IDENTIFIER() != null) {
      return ctx.IDENTIFIER().getText().toUpperCase();
    }
    if (ctx.QUOTED_IDENTIFIER() != null) {
      if (objectNameIdentifierQuotedPattern.matcher(ctx.QUOTED_IDENTIFIER().getText()).find()) {
        return ctx.QUOTED_IDENTIFIER()
            .getText()
            .toUpperCase()
            .substring(1, ctx.QUOTED_IDENTIFIER().getText().length() - 1);
      }
      return ctx.QUOTED_IDENTIFIER().getText();
    }
    if (ctx.FUTURE_TYPE_OBJECT() != null) {
      return ctx.FUTURE_TYPE_OBJECT().getText();
    }
    throw new ObjectNameException("Unable to parse object name part" + ctx.getText());
  }
}
