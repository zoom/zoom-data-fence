# Grant Builder Test Data

This directory contains YAML files that define test cases for `SnowflakeGrantBuilder` tests.

## Structure

Test data is organized to match the [Snowflake GRANT documentation](https://docs.snowflake.com/en/sql-reference/sql/grant-privilege) structure:

- **Object types** (TABLE, SCHEMA, DATABASE, AGENT, SEMANTIC_VIEW, etc.)
- **Privileges** (SELECT, USAGE, OWNERSHIP, MODIFY, MONITOR, CREATE X, etc.)
- **Grant types** (regular, future, with grant option, etc.)

## Files

- `grant-revoke-statements.yml` - Tests for SQL statement generation (GRANT/REVOKE)

## Adding New Tests

### Example: Adding a test for a new privilege

1. Open `grant-revoke-statements.yml`
2. Find the appropriate section (organized by object type)
3. Add a new test case:

```yaml
  - name: "DESCRIPTION of what this tests"
    grant:
      privilege: PRIVILEGE_NAME
      objectType: OBJECT_TYPE
      objectName: DATABASE.SCHEMA.OBJECT_NAME
      role: ROLE_NAME
      # Optional:
      grantOption: false  # default: false
      future: false       # default: false
      all: false          # default: false
    expectedGrantStatement: "GRANT PRIVILEGE_NAME ON OBJECT_TYPE \"DATABASE\".\"SCHEMA\".\"OBJECT_NAME\" TO ROLE ROLE_NAME;"
    expectedRevokeStatement: "REVOKE PRIVILEGE_NAME ON OBJECT_TYPE \"DATABASE\".\"SCHEMA\".\"OBJECT_NAME\" FROM ROLE ROLE_NAME;"
```

### Example: Adding a test for a future grant

```yaml
  - name: "SELECT on FUTURE TABLES"
    grant:
      privilege: SELECT
      objectType: TABLE
      objectName: MOCK_DB.MOCK_SCHEMA.<TABLE>
      role: MOCK_ROLE_1
      future: true
    expectedGrantStatement: "GRANT SELECT ON FUTURE TABLES IN SCHEMA \"MOCK_DB\".\"MOCK_SCHEMA\" TO ROLE MOCK_ROLE_1;"
    expectedRevokeStatement: "REVOKE SELECT ON FUTURE TABLES IN SCHEMA \"MOCK_DB\".\"MOCK_SCHEMA\" FROM ROLE MOCK_ROLE_1;"
```

### Example: Adding a test for a schema-level privilege

```yaml
  - name: "CREATE OBJECT_TYPE on SCHEMA"
    grant:
      privilege: CREATE OBJECT_TYPE
      objectType: SCHEMA
      objectName: MOCK_DB.MOCK_SCHEMA
      role: MOCK_ROLE_1
    expectedGrantStatement: "GRANT CREATE OBJECT_TYPE ON SCHEMA \"MOCK_DB\".\"MOCK_SCHEMA\" TO ROLE MOCK_ROLE_1;"
    expectedRevokeStatement: "REVOKE CREATE OBJECT_TYPE ON SCHEMA \"MOCK_DB\".\"MOCK_SCHEMA\" FROM ROLE MOCK_ROLE_1;"
```

## Reference Documentation

- [Snowflake GRANT Privilege Documentation](https://docs.snowflake.com/en/sql-reference/sql/grant-privilege)
- [Schema Object Privileges](https://docs.snowflake.com/en/sql-reference/sql/grant-privilege#schema-object-privileges)
- [Schema Privileges](https://docs.snowflake.com/en/sql-reference/sql/grant-privilege#schema-privileges)
- [Future Grants](https://docs.snowflake.com/en/sql-reference/sql/grant-privilege#future-grants-on-database-or-schema-objects)

## Benefits

- ✅ Easy to add new test cases - just edit YAML
- ✅ Clear organization matching Snowflake documentation
- ✅ Self-documenting test cases with descriptive names
- ✅ No Java code changes needed for new tests
- ✅ Can be reviewed and edited by non-Java developers

