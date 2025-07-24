package us.zoom.security.dfence.providers.snowflake;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import us.zoom.security.dfence.providers.snowflake.grant.builder.SnowflakeObjectType;
import us.zoom.security.dfence.providers.snowflake.informationschema.SnowflakeDefaultObjectService;
import us.zoom.security.dfence.test.fixtures.resultset.MockResultSet;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static us.zoom.security.dfence.test.fixtures.resultset.MockResultSet.I;

class SnowflakeDefaultObjectsServiceTestService {

    @Mock
    SnowflakeConnectionService snowflakeConnectionService;

    @InjectMocks
    SnowflakeDefaultObjectService snowflakeObjectsService;

    @Mock
    Connection connection;

    @Mock
    Statement statement;
    AutoCloseable mocks;

    public static Stream<GetContainerObjectQualNamesRawParams> getContainerObjectQualNamesRawParamsStream() {
        return Stream.of(
                new GetContainerObjectQualNamesRawParams(
                        new MockResultSet(
                                List.of(
                                        List.of(new I("MOCK_TABLE_0"), new I("MOCK_SCHEMA_0")),
                                        List.of(new I("mock_table_1"), new I("MOCK_SCHEMA_1"))),
                                List.of("name", "schema_name")),
                        List.of("MOCK_DB.MOCK_SCHEMA_0.MOCK_TABLE_0", "MOCK_DB.MOCK_SCHEMA_1.\"mock_table_1\""),
                        "MOCK_DB",
                        SnowflakeObjectType.DATABASE,
                        SnowflakeObjectType.TABLE,
                        "show tables in database MOCK_DB;"), new GetContainerObjectQualNamesRawParams(
                        new MockResultSet(
                                List.of(
                                        List.of(new I("MOCK_TABLE_0"), new I("MOCK_SCHEMA_0")),
                                        List.of(new I("mock_table_1"), new I("MOCK_SCHEMA_1"))),
                                List.of("name", "schema_name")),
                        List.of("\"mock_db\".MOCK_SCHEMA_0.MOCK_TABLE_0", "\"mock_db\".MOCK_SCHEMA_1.\"mock_table_1\""),
                        "\"mock_db\"",
                        SnowflakeObjectType.DATABASE,
                        SnowflakeObjectType.TABLE,
                        "show tables in database \"mock_db\";"), new GetContainerObjectQualNamesRawParams(
                        new MockResultSet(
                                List.of(
                                        List.of(new I("MOCK_TABLE_0"), new I("mock_schema")),
                                        List.of(new I("mock_table_1"), new I("mock_schema"))),
                                List.of("name", "schema_name")),
                        List.of(
                                "\"mock_db\".\"mock_schema\".MOCK_TABLE_0",
                                "\"mock_db\".\"mock_schema\".\"mock_table_1\""),
                        "\"mock_db\".\"mock_schema\"",
                        SnowflakeObjectType.SCHEMA,
                        SnowflakeObjectType.TABLE,
                        "show tables in schema \"mock_db\".\"mock_schema\";"), new GetContainerObjectQualNamesRawParams(
                        new MockResultSet(
                                List.of(List.of(new I("MOCK_SCHEMA_0")), List.of(new I("mock_schema_1"))),
                                List.of("name")),
                        List.of("MOCK_DB.MOCK_SCHEMA_0", "MOCK_DB.\"mock_schema_1\""),
                        "MOCK_DB",
                        SnowflakeObjectType.DATABASE,
                        SnowflakeObjectType.SCHEMA,
                        "show schemas in database MOCK_DB;"), new GetContainerObjectQualNamesRawParams(
                        new MockResultSet(
                                List.of(List.of(new I("MOCK_SCHEMA_0")), List.of(new I("mock_schema_1"))),
                                List.of("name")),
                        List.of("\"mock_db\".MOCK_SCHEMA_0", "\"mock_db\".\"mock_schema_1\""),
                        "\"mock_db\"",
                        SnowflakeObjectType.DATABASE,
                        SnowflakeObjectType.SCHEMA,
                        "show schemas in database \"mock_db\";"), new GetContainerObjectQualNamesRawParams(
                        new MockResultSet(
                                List.of(List.of(new I("MOCK_ROLE_0")), List.of(new I("mock_role_1"))),
                                List.of("name")),
                        List.of("MOCK_ROLE_0", "\"mock_role_1\""),
                        "",
                        SnowflakeObjectType.ACCOUNT,
                        SnowflakeObjectType.ROLE,
                        "show roles;"), new GetContainerObjectQualNamesRawParams(
                        new MockResultSet(
                                List.of(
                                        List.of(new I("MOCK_TABLE_0"), new I("MOCK_SCHEMA_0")),
                                        List.of(new I("mock_table_1"), new I("MOCK_SCHEMA_1")),
                                        List.of(new I("mock_table_1"), new I("INFORMATION_SCHEMA"))),
                                List.of("name", "schema_name")),
                        List.of("MOCK_DB.MOCK_SCHEMA_0.MOCK_TABLE_0", "MOCK_DB.MOCK_SCHEMA_1.\"mock_table_1\""),
                        "MOCK_DB",
                        SnowflakeObjectType.DATABASE,
                        SnowflakeObjectType.TABLE,
                        "show tables in database MOCK_DB;"), new GetContainerObjectQualNamesRawParams(
                        new MockResultSet(
                                List.of(
                                        List.of(new I("MOCK_TABLE_0"), new I("MOCK_SCHEMA_0")),
                                        List.of(new I("mock_table_1"), new I("MOCK_SCHEMA_1")),
                                        List.of(new I("mock_table_1"), new I("INFORMATION_SCHEMA"))),
                                List.of("name", "schema_name")),
                        List.of("\"mock_db\".MOCK_SCHEMA_0.MOCK_TABLE_0", "\"mock_db\".MOCK_SCHEMA_1.\"mock_table_1\""),
                        "\"mock_db\"",
                        SnowflakeObjectType.DATABASE,
                        SnowflakeObjectType.TABLE,
                        "show tables in database \"mock_db\";"), new GetContainerObjectQualNamesRawParams(
                        new MockResultSet(
                                List.of(
                                        List.of(new I("MOCK_SCHEMA_0")),
                                        List.of(new I("MOCK_SCHEMA_1")),
                                        List.of(new I("INFORMATION_SCHEMA"))), List.of("name")),
                        List.of("MOCK_DB.MOCK_SCHEMA_0", "MOCK_DB.MOCK_SCHEMA_1"),
                        "MOCK_DB",
                        SnowflakeObjectType.DATABASE,
                        SnowflakeObjectType.SCHEMA,
                        "show schemas in database MOCK_DB;"));
    }

    @BeforeEach
    void setUp() {

        mocks = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void tearDown() throws Exception {
        mocks.close();
    }

    @ParameterizedTest
    @MethodSource("getContainerObjectQualNamesRawParamsStream")
    void getContainerObjectQualNamesRaw(GetContainerObjectQualNamesRawParams params) throws SQLException {
        when(snowflakeConnectionService.connection(SnowflakeRoleType.SYSADMIN)).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);
        when(statement.getResultSet()).thenReturn(params.resultSet);
        List<String> actual = snowflakeObjectsService.getContainerObjectQualNamesDefault(
                params.containerType,
                params.objectType,
                params.containerName);
        assertEquals(params.expected, actual);
        verify(statement).execute(params.expectedQueryString);
    }

    public record GetContainerObjectQualNamesRawParams(
            ResultSet resultSet,
            List<String> expected,
            String containerName,
            SnowflakeObjectType containerType,
            SnowflakeObjectType objectType,
            String expectedQueryString) {
    }
}