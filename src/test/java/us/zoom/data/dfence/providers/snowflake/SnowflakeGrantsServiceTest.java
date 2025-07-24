package us.zoom.data.dfence.providers.snowflake;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import us.zoom.data.dfence.providers.snowflake.grant.builder.SnowflakeGrantBuilder;
import us.zoom.data.dfence.providers.snowflake.grant.builder.SnowflakePermissionGrantBuilder;
import us.zoom.data.dfence.providers.snowflake.models.SnowflakeGrantModel;
import us.zoom.data.dfence.test.fixtures.resultset.MockResultSet;
import us.zoom.data.dfence.test.fixtures.resultset.MockResultSet.I;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

class SnowflakeGrantsServiceTest {
    AutoCloseable mocks;

    @Mock
    SnowflakeConnectionService snowflakeConnectionService;

    @Mock
    Connection snowflakeConnection;

    @InjectMocks
    SnowflakeGrantsService snowflakeGrantsService;

    @AfterEach
    void tearDown() throws Exception {
        mocks.close();
    }

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        when(snowflakeConnectionService.connection()).thenReturn(snowflakeConnection);
    }

    @Test
    void getGrants() throws SQLException {
        String roleName = "MOCK_ROLE";
        Boolean skipUnkownTypes = true;
        try (Statement currentGrantsStatement = Mockito.mock(Statement.class)) {
            try (Statement futureGrantsStatement = Mockito.mock(Statement.class)) {
                when(snowflakeConnection.createStatement()).thenReturn(currentGrantsStatement, futureGrantsStatement);
                List columnNames = List.of(
                        "privilege",
                        "granted_on",
                        "name",
                        "granted_to",
                        "grantee_name",
                        "grant_option");
                when(currentGrantsStatement.getResultSet()).thenReturn(new MockResultSet(
                        List.of(
                                List.of(
                                        new I("SELECT"),
                                        new I("TABLE"),
                                        new I("MOCK_DB.MOCK_SCHEMA.MOCK_TABLE"),
                                        new I("ROLE"),
                                        new I("MOCK_ROLE"),
                                        new I(false)), List.of(
                                        new I("INVALID_PRIVILEGE"),
                                        new I("TABLE"),
                                        new I("MOCK_TABLE"),
                                        new I("ROLE"),
                                        new I("MOCK_ROLE"),
                                        new I(false))), columnNames));
                when(futureGrantsStatement.getResultSet()).thenReturn(new MockResultSet(List.of(), columnNames));
                SnowflakeGrantBuilder builder = new SnowflakePermissionGrantBuilder(new SnowflakeGrantModel(
                        "SELECT",
                        "TABLE",
                        "MOCK_DB.MOCK_SCHEMA.MOCK_TABLE",
                        "ROLE",
                        "MOCK_ROLE",
                        false,
                        false,
                        false));
                Map<String, SnowflakeGrantBuilder> expected = Map.of(builder.getKey(), builder);
                Map<String, SnowflakeGrantBuilder> actual = snowflakeGrantsService.getGrants(roleName, skipUnkownTypes);
                assertEquals(expected, actual);
            }
        }
    }

    @Test
    void getGrantsDuplicateKey() throws SQLException {
        String roleName = "MOCK_ROLE";
        Boolean skipUnkownTypes = true;
        try (Statement currentGrantsStatement = Mockito.mock(Statement.class)) {
            try (Statement futureGrantsStatement = Mockito.mock(Statement.class)) {
                when(snowflakeConnection.createStatement()).thenReturn(currentGrantsStatement, futureGrantsStatement);
                List columnNames = List.of(
                        "privilege",
                        "granted_on",
                        "name",
                        "granted_to",
                        "grantee_name",
                        "grant_option");
                when(currentGrantsStatement.getResultSet()).thenReturn(new MockResultSet(
                        List.of(
                                List.of(
                                        new I("SELECT"),
                                        new I("TABLE"),
                                        new I("MOCK_DB.MOCK_SCHEMA.MOCK_TABLE"),
                                        new I("ROLE"),
                                        new I("MOCK_ROLE"),
                                        new I(false)), List.of(
                                        new I("SELECT"),
                                        new I("TABLE"),
                                        new I("MOCK_DB.MOCK_SCHEMA.MOCK_TABLE"),
                                        new I("ROLE"),
                                        new I("MOCK_ROLE"),
                                        new I(false))), columnNames));
                when(futureGrantsStatement.getResultSet()).thenReturn(new MockResultSet(List.of(), columnNames));
                SnowflakeGrantBuilder builder = new SnowflakePermissionGrantBuilder(new SnowflakeGrantModel(
                        "SELECT",
                        "TABLE",
                        "MOCK_DB.MOCK_SCHEMA.MOCK_TABLE",
                        "ROLE",
                        "MOCK_ROLE",
                        false,
                        false,
                        false));
                Map<String, SnowflakeGrantBuilder> expected = Map.of(builder.getKey(), builder);
                Map<String, SnowflakeGrantBuilder> actual = snowflakeGrantsService.getGrants(roleName, skipUnkownTypes);
                assertEquals(expected, actual);
            }
        }
    }

    @Test
    void filterUnsupportedObjectGrantType() throws SQLException {
        String roleName = "MOCK_ROLE";
        Boolean skipUnkownTypes = true;
        try (Statement currentGrantsStatement = Mockito.mock(Statement.class)) {
            try (Statement futureGrantsStatement = Mockito.mock(Statement.class)) {
                when(snowflakeConnection.createStatement()).thenReturn(currentGrantsStatement, futureGrantsStatement);
                List columnNames = List.of(
                        "privilege",
                        "granted_on",
                        "name",
                        "granted_to",
                        "grantee_name",
                        "grant_option");
                when(currentGrantsStatement.getResultSet()).thenReturn(new MockResultSet(
                        List.of(
                                List.of(
                                        new I("SELECT"),
                                        new I("TABLE"),
                                        new I("MOCK_DB.MOCK_SCHEMA.MOCK_TABLE"),
                                        new I("ROLE"),
                                        new I("MOCK_ROLE"),
                                        new I(false)), List.of(
                                        new I("OWNERSHIP"),
                                        new I("DIRECTORY_TABLE"),
                                        new I("DEV_ZOOM_DATA.STREAMLIT_APPS.\"DIRECTORY_TB_\"\"FOV76IX1A0QDJ719 (Stage)\"\"_34537889316558814\""),
                                        new I("ROLE"),
                                        new I("MOCK_ROLE"),
                                        new I(false))), columnNames));
                when(futureGrantsStatement.getResultSet()).thenReturn(new MockResultSet(List.of(), columnNames));
                SnowflakeGrantBuilder builder = new SnowflakePermissionGrantBuilder(new SnowflakeGrantModel(
                        "SELECT",
                        "TABLE",
                        "MOCK_DB.MOCK_SCHEMA.MOCK_TABLE",
                        "ROLE",
                        "MOCK_ROLE",
                        false,
                        false,
                        false));
                Map<String, SnowflakeGrantBuilder> expected = Map.of(builder.getKey(), builder);
                Map<String, SnowflakeGrantBuilder> actual = snowflakeGrantsService.getGrants(roleName, skipUnkownTypes);
                assertEquals(expected, actual);
            }
        }
    }
}