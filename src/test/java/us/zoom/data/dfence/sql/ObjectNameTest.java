package us.zoom.data.dfence.sql;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import us.zoom.data.dfence.exception.ObjectNameException;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class ObjectNameTest {

    static Stream<SplitPartsTestArg> splitObjectNameFixture() {
        return Stream.of(
                new SplitPartsTestArg("foo.bar", List.of("FOO", "BAR")),
                new SplitPartsTestArg("\"foo.bar\".zar", List.of("\"foo.bar\"", "ZAR")),
                new SplitPartsTestArg("\"?%^&*\".zar", List.of("\"?%^&*\"", "ZAR")),
                new SplitPartsTestArg(
                        "FOO.BAR.\"ZAR(ARG1 VARCHAR, ARG2 VARCHAR)\"",
                        List.of("FOO", "BAR", "\"ZAR(ARG1 VARCHAR, ARG2 VARCHAR)\"")),
                new SplitPartsTestArg("", List.of("")),
                new SplitPartsTestArg("FOO_ZOO.BAR_ZAR.GET_USER_NETPOLICY()", List.of("FOO_ZOO", "BAR_ZAR", "GET_USER_NETPOLICY()")),
                new SplitPartsTestArg("FOO_ZOO.BAR_ZAR.GET_USER_NETPOLICY(VARCHAR, TABLE(VARCHAR, NUMBER))", List.of("FOO_ZOO", "BAR_ZAR", "GET_USER_NETPOLICY(VARCHAR, TABLE(VARCHAR, NUMBER))"))
        );
    }

    @ParameterizedTest
    @CsvSource(
            value = {
                    "FOO;FOO",
                    "<EXTERNAL TABLE>;<EXTERNAL TABLE>",
                    "FOO_1;FOO_1",
                    "foo;FOO",
                    "FOO.BAR.ZAR;FOO.BAR.ZAR",
                    "FOO.bar.ZAR;FOO.BAR.ZAR",
                    "FOO.\"bar\".ZAR;FOO.\"bar\".ZAR",
                    "\"Things-?\".\"Stuff-0123\".FOO;\"Things-?\".\"Stuff-0123\".FOO",
                    "\"Things-?\".\"Stuff-(0123\".FOO;\"Things-?\".\"Stuff-(0123\".FOO",
                    "\"A.B\".C;\"A.B\".C",
                    "\"a.b\".c;\"a.b\".C",
                    "FOO.BAR.<TABLES>;FOO.BAR.<TABLES>",
                    "FOO.bar.<TABLES>;FOO.BAR.<TABLES>",
                    "FOO._BAR;FOO._BAR",
                    "\"FOO\".\"BAR\";FOO.BAR",
                    "\"\"\"\";\"\"\"\"",
                    "\"foo\"\"zoo\".boo;\"foo\"\"zoo\".BOO",
                    "FOO.BAR.ZAR(NUMBER); FOO.BAR.ZAR(NUMBER)",
                    "FOO.BAR.ZAR_2(NUMBER); FOO.BAR.ZAR_2(NUMBER)",
                    "FOO.BAR.ZAR(VARCHAR, NUMBER); FOO.BAR.ZAR(VARCHAR, NUMBER)",
                    "FOO.BAR.\"zar\"(VARCHAR, NUMBER); FOO.BAR.\"zar\"(VARCHAR, NUMBER)",
                    "FOO.BAR.zar(VARCHAR, NUMBER); FOO.BAR.ZAR(VARCHAR, NUMBER)",
                    "FOO.BAR.zar(VARCHAR, NUMBER(38,2)); FOO.BAR.ZAR(VARCHAR, NUMBER(38,2))",
                    "FOO.BAR.ZAR(); FOO.BAR.ZAR()",
                    "SNOWFLAKE.CORE.AVG(TABLE(FLOAT));SNOWFLAKE.CORE.AVG(TABLE(FLOAT))",
                    "SNOWFLAKE.CORE.AVG(TABLE(FLOAT), TABLE(FLOAT, VARCHAR, NUMBER(38,2)));SNOWFLAKE.CORE.AVG(TABLE(FLOAT), TABLE(FLOAT, VARCHAR, NUMBER(38,2)))",
                    "snowflake.CORE.\"avg$#()))(())\"(TABLE(FLOAT), TABLE(FLOAT, VARCHAR, NUMBER(38,2)));SNOWFLAKE.CORE.\"avg$#()))(())\"(TABLE(FLOAT), TABLE(FLOAT, VARCHAR, NUMBER(38,2)))"},
            delimiter = ';')
    void normalizeObjectName(String input, String expected) {
        // I am not a mean person. However, if I wake up evil some day I will make people write code to pass this
        // test in an interview coding question.
        assertEquals(expected, ObjectName.normalizeObjectName(input));
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                    "%", "-", "-abc"})
    void normalizedObjetNameRaises(String value) {
        assertThrows(ObjectNameException.class, () -> ObjectName.normalizeObjectName(value));
    }


    @ParameterizedTest
    @ValueSource(
            strings = {
                    "FOO.BAR.A-123", "A-123", "A.B.?", "A.B.C.D",})
    void normalizeObjectNameObjectNameException(String input) {
        assertThrows(ObjectNameException.class, () -> ObjectName.normalizeObjectNamePart(input));
    }

    @ParameterizedTest
    @MethodSource("splitObjectNameFixture")
    void splitObjectName(SplitPartsTestArg params) {
        List<String> actual = ObjectName.splitObjectName(params.value);
        try {
            assertArrayEquals(params.expected.toArray(), actual.toArray());
        } catch (AssertionError e) {
            throw e;
        }
    }

    @Test
    void normalizeObjectNamePartEmpty() {
        assertEquals("", ObjectName.normalizeObjectNamePart(""));
    }

    @ParameterizedTest
    @CsvSource(
            value = {
                    "FOO;\"FOO\"",
                    "\"FOO\";\"FOO\"",
                    "\"FOO_1\";\"FOO_1\"",
                    "FOO_1;\"FOO_1\"",
                    "foo;\"FOO\"",
                    "\"foo\";\"foo\"",
                    "FOO.BAR.ZAR;\"FOO\".\"BAR\".\"ZAR\"",
                    "\"FOO\".\"BAR\".\"ZAR\";\"FOO\".\"BAR\".\"ZAR\"",
                    "FOO.bar.ZAR;\"FOO\".\"BAR\".\"ZAR\"",
                    "\"FOO\".\"bar\".\"ZAR\";\"FOO\".\"bar\".\"ZAR\"",
                    "FOO.\"bar\".ZAR;\"FOO\".\"bar\".\"ZAR\"",
                    "\"Things-?\".\"Stuff-0123\".FOO;\"Things-?\".\"Stuff-0123\".\"FOO\"",
                    "FOO.BAR.ZAR(NUMBER); \"FOO\".\"BAR\".\"ZAR\"(NUMBER)",
                    "FOO.BAR.ZAR_2(NUMBER); \"FOO\".\"BAR\".\"ZAR_2\"(NUMBER)",
                    "FOO.BAR.ZAR(VARCHAR, NUMBER); \"FOO\".\"BAR\".\"ZAR\"(VARCHAR, NUMBER)",
                    "FOO.BAR.\"zar\"(VARCHAR, NUMBER); \"FOO\".\"BAR\".\"zar\"(VARCHAR, NUMBER)",
                    "FOO.BAR.zar(VARCHAR, NUMBER); \"FOO\".\"BAR\".\"ZAR\"(VARCHAR, NUMBER)",
                    "\"FOO\".\"BAR\".\"ZAR\"(VARCHAR, NUMBER); \"FOO\".\"BAR\".\"ZAR\"(VARCHAR, NUMBER)",
                    "\"FOO\".\"BAR\".\"ZAR\"(); \"FOO\".\"BAR\".\"ZAR\"()"}, delimiter = ';')
    void quotedObjectName(String input, String expected) {
        assertEquals(expected, ObjectName.quotedObjectName(input));
    }

    @ParameterizedTest
    @CsvSource(
            value = {
                    "DELETE_ME_DUSTIN_TEST.TEST.\"PYTHON_ADD1(A NUMBER, B VARCHAR):NUMBER(38,0)\";DELETE_ME_DUSTIN_TEST.TEST.PYTHON_ADD1(NUMBER, VARCHAR)",
                    "DELETE_ME_DUSTIN_TEST.TEST.\"python_add1(A NUMBER, B VARCHAR):NUMBER(38,0)\";DELETE_ME_DUSTIN_TEST.TEST.\"python_add1\"(NUMBER, VARCHAR)",
                    "DELETE_ME_DUSTIN_TEST.TEST.\"python_add1_2$%^(A NUMBER, B VARCHAR):NUMBER(38,0)\";DELETE_ME_DUSTIN_TEST.TEST.\"python_add1_2$%^\"(NUMBER, VARCHAR)",
                    "APP_REPLICA_GLOBAL_US_DB_DEV.DATA_GOVERNANCE.\"SP_APPLY_ROW_ACCESS_POLICY(TABLE_NAME VARCHAR, POLICY_NAME VARCHAR, COLUMNS ARRAY):VARCHAR\";APP_REPLICA_GLOBAL_US_DB_DEV.DATA_GOVERNANCE.SP_APPLY_ROW_ACCESS_POLICY(VARCHAR, VARCHAR, ARRAY)",
                    "DELETE_ME_DUSTIN_TEST.TEST.\"TEST_OUTPUT(ARG_A VARCHAR, ARG_B NUMBER):TABLE: (RET_COL_A VARCHAR, RET_COL_B VARCHAR)\";DELETE_ME_DUSTIN_TEST.TEST.TEST_OUTPUT(VARCHAR, NUMBER)",
                    "ADITYA_MOTWANI_DEV.TAG_TEST.\"TEST_OUTPUT():TABLE: ()\";ADITYA_MOTWANI_DEV.TAG_TEST.TEST_OUTPUT()",
                    "SNOWFLAKE.CORE.\"AVG(ARG_T TABLE(FLOAT)):FLOAT\";SNOWFLAKE.CORE.AVG(TABLE(FLOAT))",
                    "SNOWFLAKE.CORE.\"AVG(ARG_T TABLE(FLOAT), ARG2 TABLE(FLOAT)):FLOAT\";SNOWFLAKE.CORE.AVG(TABLE(FLOAT), TABLE(FLOAT))",
                    "SNOWFLAKE.CORE.\"AVG(ARG_T TABLE(FLOAT), ARG2 TABLE(FLOAT, VARCHAR, NUMBER(38,2))):FLOAT\";SNOWFLAKE.CORE.AVG(TABLE(FLOAT), TABLE(FLOAT, VARCHAR, NUMBER(38,2)))",
                    "SNOWFLAKE.CORE.\"AVG(ARG_T TABLE(FLOAT), ARG2 TABLE(FLOAT, VARCHAR, NUMBER(38,2))):FLOAT\";SNOWFLAKE.CORE.AVG(TABLE(FLOAT), TABLE(FLOAT, VARCHAR, NUMBER(38,2)))",
                    "SNOWFLAKE.CORE.\"AVG(ARG_T TABLE(TABLE(FLOAT)), ARG2 TABLE(FLOAT, VARCHAR, NUMBER(38,2))):FLOAT\";SNOWFLAKE.CORE.AVG(TABLE(TABLE(FLOAT)), TABLE(FLOAT, VARCHAR, NUMBER(38,2)))",
                    "SNOWFLAKE.CORE.\"avg123%(ARG_T TABLE(FLOAT), ARG2 TABLE(FLOAT, VARCHAR, NUMBER(38,2))):FLOAT\";SNOWFLAKE.CORE.\"avg123%\"(TABLE(FLOAT), TABLE(FLOAT, VARCHAR, NUMBER(38,2)))",
                    "FOO.BAR.\"DUPLICATE_COUNT(ARG_T TABLE(ARG_C FLOAT)):NUMBER(38,0)\";FOO.BAR.DUPLICATE_COUNT(TABLE(FLOAT))",
                    "FOO.BAR.\"ZAR()\";FOO.BAR.ZAR()"}, delimiter = ';')
    void procedureGrantNameToObjectName(String input, String expected) {
        String actual = ObjectName.procedureGrantNameToObjectName(input);
        assertEquals(expected, actual);
    }

    @ParameterizedTest
    @CsvSource(
            value = {
                    "FOO.BAR.ZAR;FOO.BAR",
                    "FOO.bar.ZAR;FOO.BAR",
                    "FOO.\"bar\".ZAR;FOO.\"bar\"",
                    "\"Things-?\".\"Stuff-0123\".FOO;\"Things-?\".\"Stuff-0123\"",
                    "\"A.B\".C;\"A.B\"",
                    "\"a.b\".c;\"a.b\"",
                    "FOO.BAR.<TABLES>;FOO.BAR",
                    "FOO.bar.<TABLES>;FOO.BAR",
                    "FOO._BAR;FOO",
                    "\"FOO\".\"BAR\";FOO",
                    "FOO.BAR.ZAR(NUMBER); FOO.BAR",
                    "FOO.BAR.ZAR_2(NUMBER); FOO.BAR",
                    "FOO.BAR.ZAR(VARCHAR, NUMBER); FOO.BAR",
                    "FOO.BAR.\"zar\"(VARCHAR, NUMBER); FOO.BAR",
                    "FOO.BAR.zar(VARCHAR, NUMBER); FOO.BAR",
                    "FOO.BAR.ZAR(); FOO.BAR",}, delimiter = ';')
    public void testContainerName(String input, String expected) {
        String result = ObjectName.containerName(input);
        assertEquals(expected, result);
    }


    public record SplitPartsTestArg(String value, List<String> expected) {
    }
}