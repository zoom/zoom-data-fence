package us.zoom.security.dfence.sql.visitors.objectname;

import us.zoom.security.dfence.sql.error.ParserException;
import us.zoom.security.dfence.sql.models.SqlObject;
import us.zoom.security.dfence.sql.parser.ObjectNameBaseVisitor;
import us.zoom.security.dfence.sql.parser.ObjectNameParser;

import java.util.List;

public class SqlObjectObjectNameVisitor extends ObjectNameBaseVisitor<SqlObject> {

    private final SqlDataTypeObjectNameVisitor dataTypeVisitor = new SqlDataTypeObjectNameVisitor();
    private final PartObjectNameVisitor namePartVisitor = new PartObjectNameVisitor();

    @Override
    public SqlObject visitObjectName(ObjectNameParser.ObjectNameContext ctx) {
        SqlObject sqlObject = new SqlObject();
        List<ObjectNameParser.PartContext> parts = ctx.part();
        if (parts.size() > 3) {
            throw new ParserException(String.format("Object name %s has more than 3 parts.", ctx.getText()));
        }
        sqlObject.getNames().addAll(parts.stream().map(x -> x.accept(namePartVisitor)).toList());
        sqlObject.setHasArguments(ctx.args() != null);
        if (sqlObject.isHasArguments()) {
            sqlObject.getArguments()
                    .addAll(ctx.args().argument().stream().map(x -> x.accept(dataTypeVisitor)).toList());
        }
        return sqlObject;
    }

}
