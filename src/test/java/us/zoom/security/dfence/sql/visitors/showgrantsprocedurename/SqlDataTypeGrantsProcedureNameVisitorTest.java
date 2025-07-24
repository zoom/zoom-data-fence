package us.zoom.security.dfence.sql.visitors.showgrantsprocedurename;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import us.zoom.security.dfence.sql.models.SqlDataType;
import us.zoom.security.dfence.sql.parser.ShowGrantsProcedureNameLexer;
import us.zoom.security.dfence.sql.parser.ShowGrantsProcedureNameParser;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SqlDataTypeGrantsProcedureNameVisitorTest {

    private SqlDataTypeGrantsProcedureNameVisitor visitor;

    public static Stream<DataTypeTestParams> visitDataTypeTestParams() {
        return Stream.of(
                new DataTypeTestParams("FLOAT", new SqlDataType("FLOAT")),
                new DataTypeTestParams(
                        "TABLE(FLOAT)",
                        new SqlDataType("TABLE", List.of(new SqlDataType("FLOAT")), List.of())),
                new DataTypeTestParams(
                        "TABLE(FLOAT, NUMBER)",
                        new SqlDataType(
                                "TABLE",
                                List.of(new SqlDataType("FLOAT"), new SqlDataType("NUMBER")),
                                List.of())),
                new DataTypeTestParams(
                        "TABLE(ARG_C FLOAT)",
                        new SqlDataType("TABLE", List.of(new SqlDataType("FLOAT")), List.of())),
                new DataTypeTestParams(
                        "TABLE(ARG_C FLOAT, ARG_D NUMBER)",
                        new SqlDataType(
                                "TABLE",
                                List.of(new SqlDataType("FLOAT"), new SqlDataType("NUMBER")),
                                List.of())));
    }

    @BeforeEach
    void setUp() {

        visitor = new SqlDataTypeGrantsProcedureNameVisitor();
    }

    @ParameterizedTest
    @MethodSource("visitDataTypeTestParams")
    void visitDataType(DataTypeTestParams params) {
        ShowGrantsProcedureNameLexer lexer = new ShowGrantsProcedureNameLexer(CharStreams.fromString(params.input));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        ShowGrantsProcedureNameParser parser = new ShowGrantsProcedureNameParser(tokens);
        ParseTree tree = parser.dataType();
        SqlDataType actual = tree.accept(visitor);
        assertEquals(params.expected, actual);
    }

    public record DataTypeTestParams(String input, SqlDataType expected) {
    }
}