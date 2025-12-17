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
import java.util.Set;

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
                grants.add(new Grant(
                        resultSet.getString("privilege"),
                        resultSet.getString("granted_on"),
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
        List<Grant> sortedActual = new ArrayList<>(actualGrants);
        sortedActual.sort(Comparator.comparing(x -> x.toString().hashCode()));
        
        List<Grant> sortedExpected = new ArrayList<>(expectedGrants);
        sortedExpected.sort(Comparator.comparing(x -> x.toString().hashCode()));

        Assert.assertEquals(Set.copyOf(sortedActual), Set.copyOf(sortedExpected));
    }

    /**
     * Record representing a grant in Snowflake.
     * Note: The "granted_on" field from SHOW GRANTS uses spaces (e.g., "SEMANTIC VIEW", "AGENT").
     */
    public record Grant(String privilege, String grantedOn, String name, String grantedTo, String granteeName) {
        // Constructor that accepts the ResultSet field name
        public Grant {
            // Record compact constructor - validates but doesn't transform
        }
    }
}

