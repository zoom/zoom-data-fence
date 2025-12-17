# Test Refactoring Suggestions for SnowflakeGrantBuilderTest

## Current Issues

1. **835 lines** of test code, with ~750 lines being test data
2. **Three large parameterized test streams** with repetitive, verbose data structures
3. **Hard to scan** - lots of nested constructor calls
4. **Difficult to add new test cases** - requires understanding the full structure
5. **No clear grouping** - all test cases mixed together

## Recommended Approaches (Ranked)

### Option 1: **Builder Pattern with Helper Methods** ⭐ (Recommended)

**Best balance of type safety and readability**

Create builder classes and helper methods to make test data construction more readable:

```java
// Helper class for building test data
class GrantTestDataBuilder {
    static GrantModelExpectedBuilder grantModel(String privilege, String objectType, String name) {
        return new GrantModelExpectedBuilder(
            grantModel(privilege, objectType, name, "MY_ROLE", false, false, false),
            SnowflakePermissionGrantBuilder.class
        );
    }
    
    static GrantModelExpectedBuilder ownershipGrant(String objectType, String name) {
        return new GrantModelExpectedBuilder(
            grantModel("OWNERSHIP", objectType, name, "MY_ROLE", false, false, false),
            SnowflakeOwnershipGrantBuilder.class
        );
    }
    
    static SnowflakeGrantModel grantModel(String privilege, String objectType, String name, 
                                          String role, boolean grantOption, boolean future, boolean all) {
        return new SnowflakeGrantModel(privilege, objectType, name, "ROLE", role, grantOption, future, all);
    }
}

// Usage in tests:
static Stream<GrantModelExpectedBuilder> fixtureGrants() {
    return Stream.of(
        GrantTestDataBuilder.grantModel("SELECT", "TABLE", "FOO.BAR.STAR"),
        GrantTestDataBuilder.grantModel("SELECT", "TABLE", "\"foO\".\"bar-1@\".STAR"),
        GrantTestDataBuilder.ownershipGrant("TABLE", "FOO.BAR.ZAR"),
        GrantTestDataBuilder.grantModel("USAGE", "SCHEMA", "FOO.BAR"),
        // ... much more readable
    );
}
```

**Pros:**
- ✅ Type-safe (IDE autocomplete works)
- ✅ Reduces verbosity by ~60-70%
- ✅ Clear, readable test cases
- ✅ Easy to add new test cases
- ✅ Can group related tests together

**Cons:**
- ⚠️ Still in Java code (but much cleaner)
- ⚠️ Requires creating builder classes

---

### Option 2: **Split into Focused Test Classes**

Break the large test into smaller, focused test classes:

```
SnowflakeGrantBuilderTest.java (main test class)
├── SnowflakeGrantBuilderFixtureTest.java (fixtureGrants tests)
├── SnowflakeGrantBuilderStatementsTest.java (grantRevokeStatements tests)
└── SnowflakeGrantBuilderPlaybookTest.java (playbookPrivilegeGrant tests)
```

Each class can have its own data structure optimized for that test type.

**Pros:**
- ✅ Better organization
- ✅ Easier to find specific tests
- ✅ Can optimize data structure per test type

**Cons:**
- ⚠️ Still has verbose inline data
- ⚠️ More files to manage

---

### Option 3: **Simple YAML with Java Loaders** (Hybrid)

Use simple YAML files for test data, but keep type-safe Java loaders:

**YAML structure (simple, flat):**
```yaml
# src/test/resources/grant-builder-fixtures.yml
fixtures:
  - privilege: SELECT
    objectType: TABLE
    name: FOO.BAR.STAR
    expectedBuilder: SnowflakePermissionGrantBuilder
  - privilege: OWNERSHIP
    objectType: TABLE
    name: FOO.BAR.ZAR
    expectedBuilder: SnowflakeOwnershipGrantBuilder
```

**Java loader (type-safe):**
```java
class GrantTestDataLoader {
    record FixtureData(String privilege, String objectType, String name, String expectedBuilder) {}
    
    static Stream<GrantModelExpectedBuilder> loadFixtures() {
        // Load YAML, map to Java records (type-safe)
        // Convert to test parameters
    }
}
```

**Pros:**
- ✅ Clear separation of data and code
- ✅ Easy to add/edit test cases in YAML
- ✅ Type-safe Java layer for validation
- ✅ Follows existing codebase pattern (YAML test data)

**Cons:**
- ⚠️ No IDE autocomplete in YAML
- ⚠️ Requires YAML parsing code
- ⚠️ Two places to maintain (YAML + loader)

---

### Option 4: **Table-Driven Tests with CSV/TSV**

Use simple CSV/TSV files for tabular test data:

```csv
privilege,objectType,name,role,grantOption,future,all,expectedBuilder
SELECT,TABLE,FOO.BAR.STAR,MY_ROLE,false,false,false,SnowflakePermissionGrantBuilder
OWNERSHIP,TABLE,FOO.BAR.ZAR,MY_ROLE,false,false,false,SnowflakeOwnershipGrantBuilder
```

**Pros:**
- ✅ Very easy to add test cases
- ✅ Can edit in Excel/Google Sheets
- ✅ Clear tabular structure

**Cons:**
- ⚠️ No type safety
- ⚠️ Harder to represent complex nested structures
- ⚠️ Less readable for complex cases

---

## Recommendation: **Option 1 (Builder Pattern) + Option 2 (Split Classes)**

Combine both approaches for maximum benefit:

1. **Split into 3 focused test classes** (better organization)
2. **Use builder pattern** within each class (reduced verbosity)
3. **Group related test cases** (easier to understand)

### Example Structure:

```java
// SnowflakeGrantBuilderFixtureTest.java
class SnowflakeGrantBuilderFixtureTest {
    private static final GrantTestDataBuilder builder = new GrantTestDataBuilder();
    
    static Stream<GrantModelExpectedBuilder> fixtureGrants() {
        return Stream.of(
            // Basic grants
            builder.permissionGrant("SELECT", "TABLE", "FOO.BAR.STAR"),
            builder.permissionGrant("SELECT", "TABLE", "\"foO\".\"bar-1@\".STAR"),
            builder.permissionGrant("USAGE", "SCHEMA", "FOO.BAR"),
            builder.permissionGrant("USAGE", "DATABASE", "FOO"),
            
            // Ownership grants
            builder.ownershipGrant("SCHEMA", "FOO.BAR"),
            builder.ownershipGrant("DATABASE", "FOO"),
            builder.ownershipGrant("TABLE", "FOO.BAR.ZAR"),
            
            // Future grants
            builder.futurePermissionGrant("SELECT", "EXTERNAL_TABLE", "FOO.BAR.<EXTERNAL TABLE>"),
            
            // Agent grants
            builder.permissionGrant("USAGE", "AGENT", "FOO.BAR.ZAR"),
            builder.ownershipGrant("AGENT", "FOO.BAR.ZAR"),
            
            // Semantic view grants
            builder.permissionGrant("SELECT", "SEMANTIC_VIEW", "FOO.BAR.ZAR"),
            builder.permissionGrant("REFERENCES", "SEMANTIC_VIEW", "FOO.BAR.ZAR")
        );
    }
}
```

This reduces the file from **835 lines to ~200-300 lines** while maintaining full type safety.

---

## Implementation Steps

1. Create `GrantTestDataBuilder` helper class
2. Create `GrantStatementsTestDataBuilder` for statement tests
3. Create `PlaybookGrantTestDataBuilder` for playbook tests
4. Split into 3 test classes
5. Refactor existing tests to use builders
6. Group related test cases with comments

---

## Alternative: Keep Current Structure but Add Helpers

If you prefer not to split files, at minimum add builder helper methods to reduce verbosity by 50-60% while keeping everything in one file.

