package us.zoom.data.systemtest;

import org.testng.annotations.*;
import us.zoom.data.dfence.test.fixtures.SnowflakeConnectionProvider;

import java.util.Properties;

public class SnowflakeSysTestBase {
    protected String snowflakeUser;
    protected String snowflakePassword;
    protected String snowflakeAuthenticator;
    protected String snowflakeAccount;
    protected String snowflakeSysadminRole = "SYSADMIN";
    protected String snowflakeSecurityAdminRole = "SECURITYADMIN";
    protected SnowflakeConnectionProvider sysadminSnowflakeConnectionProvider;
    protected SnowflakeConnectionProvider securityadminSnowflakeConnectionProvider;

    @BeforeClass()
    @Parameters(
            {
                    "snowflake-user",
                    "snowflake-account",
                    "snowflake-authenticator",
                    "snowflake-password",
                    "snowflake-sysadmin-role",
                    "snowflake-securityadmin-role"})
    public void beforeClass(
            String snowflakeUser,
            String snowflakeAccount,
            @Optional String snowflakeAuthenticator,
            @Optional String snowflakePassword,
            @Optional String snowflakeSysadminRole,
            @Optional String snowflakeSecurityAdminRole
    ) {
        this.snowflakeUser = snowflakeUser;
        this.snowflakeAccount = snowflakeAccount;
        this.snowflakeAuthenticator = snowflakeAuthenticator;
        this.snowflakePassword = snowflakePassword;
        if (snowflakeSysadminRole != null) {
            this.snowflakeSysadminRole = snowflakeSysadminRole;
        }
        if (snowflakeSecurityAdminRole != null) {
            this.snowflakeSecurityAdminRole = snowflakeSecurityAdminRole;
        }

        Properties sysadminConnectionProperties = new Properties();
        sysadminConnectionProperties.put("user", snowflakeUser);
        if (snowflakePassword != null) {
            sysadminConnectionProperties.put("password", snowflakePassword);
        }
        if (snowflakeAuthenticator != null) {
            sysadminConnectionProperties.put("authenticator", snowflakeAuthenticator);
        }
        sysadminConnectionProperties.put("role", this.snowflakeSysadminRole);
        Properties securityadminConnectionProperties = new Properties();
        securityadminConnectionProperties.putAll(sysadminConnectionProperties);
        securityadminConnectionProperties.put("role", this.snowflakeSecurityAdminRole);
        sysadminSnowflakeConnectionProvider = new SnowflakeConnectionProvider(
                sysadminConnectionProperties,
                snowflakeAccount);
        securityadminSnowflakeConnectionProvider = new SnowflakeConnectionProvider(
                securityadminConnectionProperties,
                snowflakeAccount);
    }

}
