package us.zoom.security.dfence.providers.snowflake.informationschema;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import us.zoom.security.dfence.providers.snowflake.grant.builder.SnowflakeObjectType;

import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class SnowflakeObjectsServiceTest {

    @Mock
    SnowflakeTableObjectService snowflakeTableObjectService;

    @Mock
    SnowflakeDefaultObjectService snowflakeDefaultObjectService;

    @InjectMocks
    SnowflakeObjectsService snowflakeObjectsService;
    AutoCloseable mocks;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        snowflakeObjectsService.clearCache();
    }

    @AfterEach
    void tearDown() throws Exception {
        mocks.close();
    }

    @Test
    void objectExists() {
        String objectName = "MOCK_DB.MOCK_SCHEMA.MOCK_TABLE";
        SnowflakeObjectType objectType = SnowflakeObjectType.TABLE;
        when(snowflakeTableObjectService.getContainerTables(
                "MOCK_DB.MOCK_SCHEMA",
                SnowflakeObjectType.SCHEMA,
                objectType)).thenReturn(List.of(
                "MOCK_DB.MOCK_SCHEMA.OTHER_TABLE",
                objectName));
        when(snowflakeDefaultObjectService.getContainerObjectQualNamesDefault(
                SnowflakeObjectType.DATABASE,
                SnowflakeObjectType.SCHEMA,
                "MOCK_DB")).thenReturn(List.of("MOCK_DB.MOCK_SCHEMA", "MOCK_DB.OTHER_SCHEMA"));
        when(snowflakeDefaultObjectService.getContainerObjectQualNamesDefault(
                SnowflakeObjectType.ACCOUNT,
                SnowflakeObjectType.DATABASE,
                "")).thenReturn(List.of(
                "MOCK_DB",
                "OTHER_DB"));
        assertTrue(snowflakeObjectsService.objectExists(objectName, objectType));
    }

    @Test
    void objectExistsNonStandardName() {
        String objectName = "mock_db.\"mock-schema\".\"mock@table\"";
        SnowflakeObjectType objectType = SnowflakeObjectType.TABLE;
        when(snowflakeTableObjectService.getContainerTables(
                "MOCK_DB.\"mock-schema\"",
                SnowflakeObjectType.SCHEMA,
                objectType)).thenReturn(List.of(
                "MOCK_DB.\"mock-schema\".OTHER_TABLE",
                "MOCK_DB.\"mock-schema\".\"mock@table\""));
        when(snowflakeDefaultObjectService.getContainerObjectQualNamesDefault(
                SnowflakeObjectType.DATABASE,
                SnowflakeObjectType.SCHEMA,
                "MOCK_DB")).thenReturn(List.of("MOCK_DB.\"mock-schema\"", "MOCK_DB.MOCK_OTHER_SCHEMA"));
        when(snowflakeDefaultObjectService.getContainerObjectQualNamesDefault(
                SnowflakeObjectType.ACCOUNT,
                SnowflakeObjectType.DATABASE,
                "")).thenReturn(List.of(
                "MOCK_DB",
                "OTHER_DB"));
        assertTrue(snowflakeObjectsService.objectExists(objectName, objectType));
    }

    @Test
    void objectExistsDatabase() {
        String objectName = "MOCK_DB";
        SnowflakeObjectType objectType = SnowflakeObjectType.DATABASE;
        when(snowflakeDefaultObjectService.getContainerObjectQualNamesDefault(
                SnowflakeObjectType.ACCOUNT,
                SnowflakeObjectType.DATABASE,
                "")).thenReturn(List.of(
                "MOCK_DB",
                "OTHER_DB"));
        assertTrue(snowflakeObjectsService.objectExists(objectName, objectType));
    }

    @Test
    void objectExistsRole() {
        String objectName = "MOCK_ROLE";
        SnowflakeObjectType objectType = SnowflakeObjectType.ROLE;
        when(snowflakeDefaultObjectService.getContainerObjectQualNamesDefault(
                SnowflakeObjectType.ACCOUNT,
                SnowflakeObjectType.ROLE,
                "")).thenReturn(List.of(
                "MOCK_ROLE",
                "OTHER_ROLE"));
        assertTrue(snowflakeObjectsService.objectExists(objectName, objectType));
    }

    @Test
    void objectExistsThreadSafety() {
        String objectName = "MOCK_DB.MOCK_SCHEMA.MOCK_TABLE0";
        List<String> objects = IntStream.range(0, 100).mapToObj(x -> "MOCK_DB.MOCK_SCHEMA.MOCK_TABLE" + x).toList();
        List<String> databases = IntStream.range(0, 100000).mapToObj(x -> "MOCK_DB"+x).toList();
        when(snowflakeTableObjectService.getContainerTables(
                "MOCK_DB.MOCK_SCHEMA", SnowflakeObjectType.SCHEMA, SnowflakeObjectType.TABLE)).thenReturn(
                objects
        );
        when(snowflakeDefaultObjectService.getContainerObjectQualNamesDefault(
                SnowflakeObjectType.DATABASE,
                SnowflakeObjectType.SCHEMA,
                "MOCK_DB")).thenReturn(
                List.of("MOCK_DB.MOCK_SCHEMA")
        );
        when(snowflakeDefaultObjectService.getContainerObjectQualNamesDefault(
                SnowflakeObjectType.ACCOUNT,
                SnowflakeObjectType.DATABASE,
                "")).thenReturn(
                List.of("MOCK_DB")
        );
        when(snowflakeTableObjectService.getContainerTables(
                any(),
                eq(SnowflakeObjectType.DATABASE),
                eq(SnowflakeObjectType.VIEW))).thenReturn(
                List.of("OTHER_VIEW", "OTHER_VIEW_2")
        );
        //assertTrue(snowflakeObjectsService.objectExists("MOCK_DB", SnowflakeObjectType.DATABASE));
        //assertTrue(snowflakeObjectsService.objectExists("MOCK_DB.MOCK_SCHEMA", SnowflakeObjectType.SCHEMA));
        assertTrue(snowflakeObjectsService.objectExists("MOCK_DB.MOCK_SCHEMA.MOCK_TABLE0", SnowflakeObjectType.TABLE));
        // Assert that all the objects can be found with a big cache.
        databases.parallelStream().map(x -> snowflakeObjectsService.objectExists("MOCK_DB" + x + ".MOCK_SCHEMA.OTHER_VIEW_NOT_IN_DB"+x, SnowflakeObjectType.VIEW)).forEach(Assertions::assertFalse);
        objects.parallelStream().map(x -> snowflakeObjectsService.objectExists(x, SnowflakeObjectType.TABLE)).forEach(Assertions::assertTrue);
        // Assert that only one object
        Collections.singletonList(IntStream.range(0, 10000)).parallelStream().map(x -> snowflakeObjectsService.objectExists(objectName,
                        SnowflakeObjectType.TABLE))
                .forEach(Assertions::assertTrue);
        verify(snowflakeTableObjectService, times(1)).getContainerTables(
                "MOCK_DB.MOCK_SCHEMA",
                SnowflakeObjectType.SCHEMA,
                SnowflakeObjectType.TABLE
        );
    }

}