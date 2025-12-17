package us.zoom.data.systemtest;

import org.testng.Assert;
import us.zoom.data.dfence.test.fixtures.SnowflakeConnectionProvider;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Base class for grant system tests providing common functionality.
 * Makes it easy to identify which test class failed by having clear class names.
 */
public abstract class BaseGrantSystemTest extends SnowflakeSysTestBase {

    /**
     * Retrieves all grants for a given role.
     * 
     * @param roleName The role name to query grants for
     * @param connectionProvider The connection provider to use
     * @return List of grants for the role
     * @throws SQLException If there's an error querying grants
     */
    protected static List<Grant> getRoleGrants(String roleName, SnowflakeConnectionProvider connectionProvider)
            throws SQLException {
        try (Connection connection = connectionProvider.getConnection()) {
            Statement statement = connection.createStatement();
            statement.execute(String.format("SHOW GRANTS TO ROLE %s", roleName));
            ResultSet resultSet = statement.getResultSet();
            List<Grant> grants = new ArrayList<>();
            while (resultSet.next()) {
                // Normalize granted_on: convert spaces to underscores to match enum naming convention
                // (e.g., "SEMANTIC VIEW" -> "SEMANTIC_VIEW", "CORTEX AGENT" -> "CORTEX_AGENT")
                String grantedOn = resultSet.getString("granted_on").toUpperCase().replace(" ", "_");
                grants.add(new Grant(
                        resultSet.getString("privilege"),
                        grantedOn,
                        resultSet.getString("name"),
                        resultSet.getString("granted_to"),
                        resultSet.getString("grantee_name")));
            }
            return grants;
        }
    }

    /**
     * Asserts that the actual grants match the expected grants.
     * Sorts both lists before comparison to ensure order doesn't matter.
     * 
     * @param actualGrants The actual grants from Snowflake
     * @param expectedGrants The expected grants
     */
    protected static void assertGrantsMatch(List<Grant> actualGrants, List<Grant> expectedGrants) {
        // Sort using a deterministic comparator based on grant fields
        Comparator<Grant> grantComparator = Comparator
                .comparing(Grant::privilege)
                .thenComparing(Grant::grantedOn)
                .thenComparing(Grant::name)
                .thenComparing(Grant::grantedTo)
                .thenComparing(Grant::granteeName);
        
        List<Grant> sortedActual = new ArrayList<>(actualGrants);
        sortedActual.sort(grantComparator);
        
        List<Grant> sortedExpected = new ArrayList<>(expectedGrants);
        sortedExpected.sort(grantComparator);

        Assert.assertEquals(sortedActual, sortedExpected);
    }

    /**
     * Record representing a grant in Snowflake.
     * Note: The "granted_on" field is normalized to use underscores (e.g., "SEMANTIC_VIEW", "CORTEX_AGENT")
     * to match the enum naming convention, even though SHOW GRANTS returns spaces.
     */
    public record Grant(String privilege, String grantedOn, String name, String grantedTo, String granteeName) {
        // Constructor that accepts the ResultSet field name
        public Grant {
            // Record compact constructor - validates but doesn't transform
        }
    }
}

