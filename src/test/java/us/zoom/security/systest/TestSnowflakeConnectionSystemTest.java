package us.zoom.security.systest;

import lombok.extern.slf4j.Slf4j;
import net.snowflake.client.jdbc.internal.apache.commons.io.FileUtils;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemWriter;
import org.testng.ITestContext;
import org.testng.annotations.AfterGroups;
import org.testng.annotations.BeforeGroups;
import org.testng.annotations.Test;
import us.zoom.security.dfence.providers.snowflake.SnowflakeConnectionProperties;
import us.zoom.security.dfence.providers.snowflake.SnowflakeConnectionService;
import us.zoom.security.dfence.providers.snowflake.SnowflakeProviderConfigModel;
import us.zoom.security.dfence.test.fixtures.DirectoryLifecycleObject;
import us.zoom.security.dfence.test.fixtures.LifecycleManager;
import us.zoom.security.dfence.test.fixtures.SnowflakeLifecycleObject;

import java.io.*;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.sql.*;
import java.util.Base64;
import java.util.Objects;
import java.util.UUID;

@Slf4j
public class TestSnowflakeConnectionSystemTest extends SnowflakeSysTestBase {

    @BeforeGroups(groups = {"authTest"})
    public void beforeGroupKeyTest(ITestContext ctx)
            throws NoSuchAlgorithmException, IOException, SQLException, InterruptedException {
        LifecycleManager lifecycleManager = new LifecycleManager();

        String userName = String.format("TEST_USER_%s", UUID.randomUUID().toString().replace("-", "")).toUpperCase();
        String networkPolicy = getCurrentUserNetworkPolicy();
        String password = UUID.randomUUID().toString().replace("-", "");
        KeyPair keyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();

        String systemTmpDirLocation = System.getProperty("java.io.tmpdir");
        if (!Path.of(systemTmpDirLocation).toFile().isDirectory()) {
            throw new FileNotFoundException(String.format("System tmp dir %s does not exist", systemTmpDirLocation));
        }
        Path tmpDirPath = Path.of(systemTmpDirLocation).resolve("key-test-" + UUID.randomUUID());

        DirectoryLifecycleObject directoryLifecycleObject = new DirectoryLifecycleObject(tmpDirPath);
        directoryLifecycleObject.setup();
        lifecycleManager.getLifecycleObjects().add(directoryLifecycleObject);

        Path privateKeyPath = writePrivateKeyToPemFile(keyPair, tmpDirPath);

        lifecycleManager.getLifecycleObjects()
                .add(SnowflakeLifecycleObject.user(
                        securityadminSnowflakeConnectionProvider, 
                        userName,
                        networkPolicy,
                        password,
                        keyPairPublicKeyString(keyPair)));

        log.info("Setting up lifecycleManager for {}", this.getClass().getSimpleName());
        lifecycleManager.setup();
        UserAttributes userAttributes = new UserAttributes(userName, networkPolicy, password, privateKeyPath, keyPair);
        ctx.setAttribute("lifecycle-manager", lifecycleManager);
        ctx.setAttribute("user-attributes", userAttributes);

        log.info("User access set up.");
    }

    public String getCurrentUserNetworkPolicy() {
        String currentUser;
        try (Connection connection = this.sysadminSnowflakeConnectionProvider.getConnection()) {
            Statement statement = connection.createStatement();
            statement.executeQuery("select current_user() USER");
            ResultSet resultSet = statement.getResultSet();
            resultSet.next();
            currentUser = resultSet.getString("USER");
            PreparedStatement getNetworkPolicyStatement = connection.prepareStatement(
                    "show parameters like 'NETWORK_POLICY' in user identifier(?);");
            getNetworkPolicyStatement.setString(1, currentUser);
            getNetworkPolicyStatement.executeQuery();
            ResultSet getNetworkPolicyResultSet = getNetworkPolicyStatement.getResultSet();
            getNetworkPolicyResultSet.next();
            return getNetworkPolicyResultSet.getString(2);
        } catch (SQLException e) {
            throw new RuntimeException("Unable to get network policy.", e);
        }
    }

    public String keyPairPublicKeyString(KeyPair keyPair) {
        return Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
    }

    public Path writePrivateKeyToPemFile(KeyPair keyPair, Path tmpDir) {
        PrivateKey privateKeyExpected = keyPair.getPrivate();
        byte[] privateKeyExpectedBytes = privateKeyExpected.getEncoded();
        PemObject pemObject = new PemObject("PRIVATE KEY", privateKeyExpectedBytes);
        Path outputPath = tmpDir.resolve("private-key.pem");
        File outputFile = outputPath.toFile();
        try {
            FileOutputStream outputStream = new FileOutputStream(outputFile);
            PemWriter pemWriter = new PemWriter(new OutputStreamWriter(outputStream));
            pemWriter.writeObject(pemObject);
            pemWriter.close();
        } catch (IOException e) {
            throw new RuntimeException(String.format("Unable to write key to pem file %s.", outputFile), e);
        }
        return outputPath;
    }

    @Test(groups = {"authTest"})
    public void testPasswordAuth(ITestContext ctx) throws SQLException {
        UserAttributes userAttributes = (UserAttributes) Objects.requireNonNull(ctx.getAttribute("user-attributes"));
        String userName = userAttributes.userName();
        String password = userAttributes.password();
        SnowflakeConnectionProperties snowflakeConnectionProperties = new SnowflakeConnectionProperties();
        snowflakeConnectionProperties.setUser(userName);
        snowflakeConnectionProperties.setPassword(password);
        snowflakeConnectionProperties.setSecurityAdminRole("PUBLIC");
        snowflakeConnectionProperties.setSysAdminRole("PUBLIC");
        SnowflakeProviderConfigModel snowflakeProviderConfigModel = new SnowflakeProviderConfigModel();
        snowflakeProviderConfigModel.setConnectionProperties(snowflakeConnectionProperties);
        snowflakeProviderConfigModel.setConnectionString(String.format(
                "jdbc:snowflake://%s.snowflakecomputing.com/",
                snowflakeAccount));
        SnowflakeConnectionService snowflakeConnectionService = new SnowflakeConnectionService(
                snowflakeProviderConfigModel);
        try (Connection connection = snowflakeConnectionService.connection()) {
            connection.createStatement().execute("SELECT 1");
        }

    }

    @Test(groups = {"authTest"})
    public void testPrivateKeyFileAuth(ITestContext ctx) throws SQLException, IOException {
        UserAttributes userAttributes = (UserAttributes) Objects.requireNonNull(ctx.getAttribute("user-attributes"));
        SnowflakeConnectionProperties snowflakeConnectionProperties = new SnowflakeConnectionProperties();
        snowflakeConnectionProperties.setUser(userAttributes.userName());
        snowflakeConnectionProperties.setSecurityAdminRole("PUBLIC");
        snowflakeConnectionProperties.setSysAdminRole("PUBLIC");
        snowflakeConnectionProperties.setPrivateKeyFile(userAttributes.privateKeyPath.toAbsolutePath().toString());
        SnowflakeProviderConfigModel snowflakeProviderConfigModel = new SnowflakeProviderConfigModel();
        snowflakeProviderConfigModel.setConnectionProperties(snowflakeConnectionProperties);
        snowflakeProviderConfigModel.setConnectionString(String.format(
                "jdbc:snowflake://%s.snowflakecomputing.com/",
                snowflakeAccount));
        SnowflakeConnectionService snowflakeConnectionService = new SnowflakeConnectionService(
                snowflakeProviderConfigModel);

        try (Connection connection = snowflakeConnectionService.connection()) {
            connection.createStatement().execute("SELECT 1");
        }

    }

    @Test(groups = {"authTest"})
    public void testPrivateKeyB64Auth(ITestContext ctx) throws SQLException, IOException {
        UserAttributes userAttributes = (UserAttributes) Objects.requireNonNull(ctx.getAttribute("user-attributes"));
        String privateKeyBase64 = Base64.getEncoder()
                .encodeToString(FileUtils.readFileToByteArray(userAttributes.privateKeyPath.toFile()));
        SnowflakeConnectionProperties snowflakeConnectionProperties = new SnowflakeConnectionProperties();
        snowflakeConnectionProperties.setUser(userAttributes.userName());
        snowflakeConnectionProperties.setSecurityAdminRole("PUBLIC");
        snowflakeConnectionProperties.setSysAdminRole("PUBLIC");
        snowflakeConnectionProperties.setPrivateKeyBase64(privateKeyBase64);
        SnowflakeProviderConfigModel snowflakeProviderConfigModel = new SnowflakeProviderConfigModel();
        snowflakeProviderConfigModel.setConnectionProperties(snowflakeConnectionProperties);
        snowflakeProviderConfigModel.setConnectionString(String.format(
                "jdbc:snowflake://%s.snowflakecomputing.com/",
                snowflakeAccount));
        SnowflakeConnectionService snowflakeConnectionService = new SnowflakeConnectionService(
                snowflakeProviderConfigModel);
        try (Connection connection = snowflakeConnectionService.connection()) {
            connection.createStatement().execute("SELECT 1");
        }
    }

    @AfterGroups(groups = {"authTest"})
    public void afterGroupKeyTest(ITestContext ctx) throws SQLException {
        LifecycleManager lifecycleManager = (LifecycleManager) ctx.getAttribute("lifecycle-manager");
        log.info("Tearing down lifecycleManager for {}", this.getClass().getSimpleName());
        lifecycleManager.teardown();
    }

    public record UserAttributes(String userName, String networkPolicy, String password, Path privateKeyPath, KeyPair keyPair) {}
}
