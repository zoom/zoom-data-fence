package us.zoom.data.dfence.providers.snowflake.models;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SnowflakeGrantModelTest {

    @ParameterizedTest
    @CsvSource(
            {
                    "MOCK_DB.MOCK_SCHEMA.MOCK_TABLE,\"MOCK_DB\".\"MOCK_SCHEMA\".\"MOCK_TABLE\"",
                    "MOCK_DB.MOCK_SCHEMA.\"MOCK_table\",\"MOCK_DB\".\"MOCK_SCHEMA\".\"MOCK_table\"",
                    "MOCK_DB.\"mock_schema\".\"mock_table\",\"MOCK_DB\".\"mock_schema\".\"mock_table\"",
                    "MOCK_DB.MOCK_SCHEMA,\"MOCK_DB\".\"MOCK_SCHEMA\"",
                    "MOCK_DB,\"MOCK_DB\"",
                    "mock_db,\"MOCK_DB\""
            })
    void getEscapedName(String name, String expected) {
        SnowflakeGrantModel snowflakeGrantModel = new SnowflakeGrantModel(
                "SELECT",
                "TABLE",
                name,
                "ROLE",
                "MOCK_ROLE",
                false,
                false,
                false);
        String actual = snowflakeGrantModel.getEscapedName();
        assertEquals(expected, actual);
    }

    @ParameterizedTest
    @CsvSource(
            {
                    "MOCK_DB.MOCK_SCHEMA.MOCK_TABLE,MOCK_DB.MOCK_SCHEMA.MOCK_TABLE",
                    "MOCK_DB.MOCK_SCHEMA.\"MOCK_table\",MOCK_DB.MOCK_SCHEMA.\"MOCK_table\"",
                    "MOCK_DB.\"mock_schema\".\"mock_table\",MOCK_DB.\"mock_schema\".\"mock_table\"",
                    "MOCK_DB.MOCK_SCHEMA,MOCK_DB.MOCK_SCHEMA",
                    "MOCK_DB,MOCK_DB",
                    "mock_db,MOCK_DB"
            })
    void getName(String name, String expected) {
        SnowflakeGrantModel snowflakeGrantModel = new SnowflakeGrantModel(
                "SELECT",
                "TABLE",
                name,
                "ROLE",
                "MOCK_ROLE",
                false,
                false,
                false);
        String actual = snowflakeGrantModel.name();
        assertEquals(expected, actual);

    }

    @Test
    void grantModelProcedure() {
        SnowflakeGrantModel snowflakeGrantModel = new SnowflakeGrantModel(
                "USAGE",
                "PROCEDURE",
                "FOO.BAR.ZAR(VARCHAR, NUMBER)",
                "ROLE",
                "MOCK_ROLE",
                false,
                false,
                false);
    }

    @Test
    void futureGrantNormalizationAgentMapsToCortexAgent() {
        String input = "MOCK_DB.MOCK_SCHEMA.<AGENT>";
        String normalized = SnowflakeGrantModel.normalizeFutureGrantName(input, "CORTEX_AGENT", true);
        assertEquals("MOCK_DB.MOCK_SCHEMA.<CORTEX_AGENT>", normalized);
    }

    @Test
    void futureGrantNormalizationKeepsCortexAgent() {
        String input = "MOCK_DB.MOCK_SCHEMA.<CORTEX_AGENT>";
        String normalized = SnowflakeGrantModel.normalizeFutureGrantName(input, "CORTEX_AGENT",  true);
        assertEquals("MOCK_DB.MOCK_SCHEMA.<CORTEX_AGENT>", normalized);
    }

    @Test
    void futureGrantNormalizationKeepsTableViewProcedure() {
        assertEquals("MOCK_DB.MOCK_SCHEMA.<TABLE>",
                SnowflakeGrantModel.normalizeFutureGrantName("MOCK_DB.MOCK_SCHEMA.<TABLE>", "TABLE", true));
        assertEquals("MOCK_DB.MOCK_SCHEMA.<VIEW>",
                SnowflakeGrantModel.normalizeFutureGrantName("MOCK_DB.MOCK_SCHEMA.<VIEW>", "VIEW", true));
        assertEquals("MOCK_DB.MOCK_SCHEMA.<PROCEDURE>",
                SnowflakeGrantModel.normalizeFutureGrantName("MOCK_DB.MOCK_SCHEMA.<PROCEDURE>", "PROCEDURE", true));
    }

    @Test
    void constructorStoresNormalizedFutureGrantName() {
        SnowflakeGrantModel model = new SnowflakeGrantModel(
                "USAGE",
                "CORTEX_AGENT",
                "MOCK_DB.MOCK_SCHEMA.<AGENT>",
                "ROLE",
                "MOCK_ROLE",
                false,
                true,
                false);
        assertEquals("MOCK_DB.MOCK_SCHEMA.<CORTEX_AGENT>", model.name());
    }
}
