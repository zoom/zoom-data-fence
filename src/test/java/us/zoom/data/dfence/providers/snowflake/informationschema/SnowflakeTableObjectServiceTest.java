package us.zoom.data.dfence.providers.snowflake.informationschema;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import us.zoom.data.dfence.providers.snowflake.SnowflakeConnectionService;
import us.zoom.data.dfence.providers.snowflake.SnowflakeRoleType;
import us.zoom.data.dfence.providers.snowflake.grant.builder.SnowflakeObjectType;
import us.zoom.data.dfence.test.fixtures.resultset.MockResultSet;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static us.zoom.data.dfence.test.fixtures.resultset.MockResultSet.I;

class SnowflakeTableObjectServiceTest {

    @InjectMocks
    SnowflakeTableObjectService snowflakeTableObjectService;

    @Mock
    SnowflakeConnectionService snowflakeConnectionService;

    @Mock
    Connection connection;

    @Mock
    Statement statement;

    AutoCloseable mocks;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
    }


    @AfterEach
    void tearDown() throws Exception {
        mocks.close();
    }

    @Test
    void getContainerTables() throws SQLException {
        List<String> expected = List.of("MOCK_DB.MOCK_SCHEMA_0.MOCK_TABLE_0", "MOCK_DB.MOCK_SCHEMA_0.MOCK_TABLE_1");
        ResultSet resultSet = new MockResultSet(
                List.of(
                        List.of(new I("MOCK_DB"), new I("MOCK_SCHEMA_0"), new I("MOCK_TABLE_0")),
                        List.of(new I("MOCK_DB"), new I("MOCK_SCHEMA_0"), new I("MOCK_TABLE_1"))),
                List.of("TABLE_CATALOG", "TABLE_SCHEMA", "TABLE_NAME"));
        when(snowflakeConnectionService.connection(SnowflakeRoleType.SYSADMIN)).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);
        when(statement.getResultSet()).thenReturn(resultSet);
        List<String> actual = snowflakeTableObjectService.getContainerTables(
                "MOCK_DB.MOCK_SCHEMA_0",
                SnowflakeObjectType.SCHEMA,
                SnowflakeObjectType.TABLE);
        assertEquals(actual, expected);
        verify(statement).execute(
                "select table_catalog, table_name, table_schema from MOCK_DB.information_schema.tables where table_schema = 'MOCK_SCHEMA_0' and table_type in ( 'BASE TABLE', 'TEMPORARY TABLE' );");
    }

    @Test
    void getContainerTablesUnconventionalTableName() throws SQLException {
        List<String> expected = List.of(
                "\"MockDb!\".\"mock_schema_0$\".\"MockTable#1\"",
                "\"MockDb!\".\"mock_schema_0$\".\"MockTable#1\"");
        ResultSet resultSet = new MockResultSet(
                List.of(
                        List.of(new I("MockDb!"), new I("mock_schema_0$"), new I("MockTable#1")),
                        List.of(new I("MockDb!"), new I("mock_schema_0$"), new I("MockTable#1"))),
                List.of("TABLE_CATALOG", "TABLE_SCHEMA", "TABLE_NAME"));
        when(snowflakeConnectionService.connection(SnowflakeRoleType.SYSADMIN)).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);
        when(statement.getResultSet()).thenReturn(resultSet);
        List<String> actual = snowflakeTableObjectService.getContainerTables(
                "\"MockDb!\".\"mock_schema_0$\"",
                SnowflakeObjectType.SCHEMA,
                SnowflakeObjectType.TABLE);
        assertEquals(actual, expected);
        verify(statement).execute(
                "select table_catalog, table_name, table_schema from \"MockDb!\".information_schema.tables where table_schema = 'mock_schema_0$' and table_type in ( 'BASE TABLE', 'TEMPORARY TABLE' );");
    }

    @Test
    void getContainerTablesDatabase() throws SQLException {
        List<String> expected = List.of("MOCK_DB.MOCK_SCHEMA_0.MOCK_TABLE_0", "MOCK_DB.MOCK_SCHEMA_0.MOCK_TABLE_1");
        ResultSet resultSet = new MockResultSet(
                List.of(
                        List.of(new I("MOCK_DB"), new I("MOCK_SCHEMA_0"), new I("MOCK_TABLE_0")),
                        List.of(new I("MOCK_DB"), new I("MOCK_SCHEMA_0"), new I("MOCK_TABLE_1"))),
                List.of("TABLE_CATALOG", "TABLE_SCHEMA", "TABLE_NAME"));
        when(snowflakeConnectionService.connection(SnowflakeRoleType.SYSADMIN)).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);
        when(statement.getResultSet()).thenReturn(resultSet);
        List<String> actual = snowflakeTableObjectService.getContainerTables(
                "MOCK_DB",
                SnowflakeObjectType.DATABASE,
                SnowflakeObjectType.TABLE);
        assertEquals(actual, expected);
        verify(statement).execute(
                "select table_catalog, table_name, table_schema from MOCK_DB.information_schema.tables where table_schema != 'INFORMATION_SCHEMA' and table_type in ( 'BASE TABLE', 'TEMPORARY TABLE' );");
    }
}