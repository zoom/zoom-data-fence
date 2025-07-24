package us.zoom.data.dfence.sql.visitors.showgrantsprocedurename;

import us.zoom.data.dfence.sql.error.ParserException;
import us.zoom.data.dfence.sql.parser.ShowGrantsProcedureNameBaseVisitor;
import us.zoom.data.dfence.sql.parser.ShowGrantsProcedureNameParser;

public class PartShowGrantsProcedureNameVisitor extends ShowGrantsProcedureNameBaseVisitor<String> {
    @Override
    public String visitPart(ShowGrantsProcedureNameParser.PartContext ctx) {
        return ctx.getText();
    }

    @Override
    public String visitUnquotedIdentifier(ShowGrantsProcedureNameParser.UnquotedIdentifierContext ctx) {
        if (ctx.STANDARD_UPPERCASE_IDENTIFIER() != null) {
            return ctx.STANDARD_UPPERCASE_IDENTIFIER().getText();
        }
        if (ctx.NONSTANDARD_IDENTIFIER_UNQUOTED() != null) {
            return "\"" + ctx.NONSTANDARD_IDENTIFIER_UNQUOTED().getText() + "\"";
        }
        throw new ParserException(String.format("Invalid unquoted identifier '%s'.", ctx.getText()));
    }
}
