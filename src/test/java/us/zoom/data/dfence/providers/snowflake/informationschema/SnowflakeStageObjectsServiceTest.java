package us.zoom.data.dfence.providers.snowflake.informationschema;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

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

class SnowflakeStageObjectsServiceTest {

  @InjectMocks SnowflakeStageObjectsService snowflakeStageObjectsService;

  @Mock SnowflakeConnectionService snowflakeConnectionService;

  @Mock Connection connection;

  @Mock Statement statement;
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
  void getContainerExternalStages() throws SQLException {
    List<String> expected =
        List.of("MOCK_DB.MOCK_SCHEMA_0.MOCK_STAGE_0", "MOCK_DB.MOCK_SCHEMA_0.MOCK_STAGE_1");
    ResultSet resultSet =
        new MockResultSet(
            List.of(
                List.of(
                    new MockResultSet.I("MOCK_DB"),
                    new MockResultSet.I("MOCK_SCHEMA_0"),
                    new MockResultSet.I("MOCK_STAGE_0"),
                    new MockResultSet.I("External Named")),
                List.of(
                    new MockResultSet.I("MOCK_DB"),
                    new MockResultSet.I("MOCK_SCHEMA_0"),
                    new MockResultSet.I("MOCK_STAGE_1"),
                    new MockResultSet.I("External Named"))),
            List.of("STAGE_CATALOG", "STAGE_SCHEMA", "STAGE_NAME", "STAGE_TYPE"));
    when(snowflakeConnectionService.connection(SnowflakeRoleType.SYSADMIN)).thenReturn(connection);
    when(connection.createStatement()).thenReturn(statement);
    when(statement.getResultSet()).thenReturn(resultSet);
    List<String> actual =
        snowflakeStageObjectsService.getContainerExternalStages(
            "MOCK_DB.MOCK_SCHEMA_0", SnowflakeObjectType.SCHEMA);
    assertEquals(actual, expected);
    verify(statement)
        .execute(
            "select stage_catalog, stage_schema, stage_name, stage_type from MOCK_DB.INFORMATION_SCHEMA.STAGES where stage_type = 'External Named' and stage_schema = 'MOCK_SCHEMA_0';");
  }

  @Test
  void getContainerExternalStagesDatabaseContainer() throws SQLException {
    List<String> expected =
        List.of("MOCK_DB.MOCK_SCHEMA_0.MOCK_STAGE_0", "MOCK_DB.MOCK_SCHEMA_0.MOCK_STAGE_1");
    ResultSet resultSet =
        new MockResultSet(
            List.of(
                List.of(
                    new MockResultSet.I("MOCK_DB"),
                    new MockResultSet.I("MOCK_SCHEMA_0"),
                    new MockResultSet.I("MOCK_STAGE_0"),
                    new MockResultSet.I("External Named")),
                List.of(
                    new MockResultSet.I("MOCK_DB"),
                    new MockResultSet.I("MOCK_SCHEMA_0"),
                    new MockResultSet.I("MOCK_STAGE_1"),
                    new MockResultSet.I("External Named"))),
            List.of("STAGE_CATALOG", "STAGE_SCHEMA", "STAGE_NAME", "STAGE_TYPE"));
    when(snowflakeConnectionService.connection(SnowflakeRoleType.SYSADMIN)).thenReturn(connection);
    when(connection.createStatement()).thenReturn(statement);
    when(statement.getResultSet()).thenReturn(resultSet);
    List<String> actual =
        snowflakeStageObjectsService.getContainerExternalStages(
            "MOCK_DB", SnowflakeObjectType.DATABASE);
    assertEquals(actual, expected);
    verify(statement)
        .execute(
            "select stage_catalog, stage_schema, stage_name, stage_type from MOCK_DB.INFORMATION_SCHEMA.STAGES where stage_type = 'External Named';");
  }
}
