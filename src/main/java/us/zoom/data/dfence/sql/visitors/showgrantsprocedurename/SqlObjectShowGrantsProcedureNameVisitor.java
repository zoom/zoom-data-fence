package us.zoom.data.dfence.sql.visitors.showgrantsprocedurename;

import us.zoom.data.dfence.sql.error.ParserException;
import us.zoom.data.dfence.sql.models.SqlObject;
import us.zoom.data.dfence.sql.parser.ShowGrantsProcedureNameBaseVisitor;
import us.zoom.data.dfence.sql.parser.ShowGrantsProcedureNameParser;

public class SqlObjectShowGrantsProcedureNameVisitor extends ShowGrantsProcedureNameBaseVisitor<SqlObject> {

    private final PartShowGrantsProcedureNameVisitor partShowGrantsProcedureNameVisitor
            = new PartShowGrantsProcedureNameVisitor();
    private final SqlDataTypeGrantsProcedureNameVisitor sqlDataTypeGrantsProcedureNameVisitor
            = new SqlDataTypeGrantsProcedureNameVisitor();

    @Override
    public SqlObject visitShowGrantsProcedureName(ShowGrantsProcedureNameParser.ShowGrantsProcedureNameContext ctx) {
        SqlObject sqlObject = new SqlObject();
        sqlObject.setHasArguments(true);
        ShowGrantsProcedureNameParser.UnquotedIdentifierContext unquotedIdentifierContext;
        if (ctx.part() != null && !ctx.part().isEmpty()) {
            sqlObject.getNames()
                    .addAll(ctx.part().stream().map(x -> x.accept(partShowGrantsProcedureNameVisitor)).toList());
        } else {
            throw new ParserException(String.format("Invalid part identifier '%s'.", ctx.getText()));
        }
        if (ctx.callableName() != null && ctx.callableName().unquotedIdentifier() != null) {
            String procedureName = ctx.callableName().unquotedIdentifier().accept(partShowGrantsProcedureNameVisitor);
            sqlObject.getNames().add(procedureName);
        } else {
            throw new ParserException(String.format(
                    "Invalid callable name identifier '%s'.",
                    ctx.callableName().getText()));
        }
        if (ctx.callableName() != null && ctx.callableName().expandedArgs() != null && !ctx.callableName()
                .expandedArgs().isEmpty()) {
            sqlObject.getArguments().addAll(ctx.callableName().expandedArgs().expandedArgument().stream()
                    .map(x -> x.accept(sqlDataTypeGrantsProcedureNameVisitor)).toList());
        }
        return sqlObject;
    }

    @Override
    public SqlObject visitCallableName(ShowGrantsProcedureNameParser.CallableNameContext ctx) {
        SqlObject sqlObject = new SqlObject();
        sqlObject.setHasArguments(true);
        try {
            if (ctx.unquotedIdentifier() != null) {
                String procedureName = ctx.unquotedIdentifier().accept(partShowGrantsProcedureNameVisitor);
                sqlObject.getNames().add(procedureName);
            } else {
                throw new ParserException(String.format("Invalid callable name identifier '%s'.", ctx.getText()));
            }
            if (ctx.expandedArgs() != null && !ctx.expandedArgs().isEmpty()) {
                sqlObject.getArguments().addAll(ctx.expandedArgs().expandedArgument().stream()
                        .map(x -> x.accept(sqlDataTypeGrantsProcedureNameVisitor)).toList());
            }
            return sqlObject;

        } catch (ParserException e) {
            throw new ParserException(String.format("Failure to parse callable name '%s'.", ctx.getText()), e);
        }
    }
}
