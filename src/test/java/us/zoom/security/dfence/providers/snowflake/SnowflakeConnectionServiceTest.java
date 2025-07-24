package us.zoom.security.dfence.providers.snowflake;

import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemWriter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.util.Base64;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SnowflakeConnectionServiceTest {

    @TempDir
    File testDir;

    @Test
    void connectionProperties() {
        SnowflakeConnectionProperties connectionProperties = new SnowflakeConnectionProperties();
        connectionProperties.setUser("mock_user");
        connectionProperties.setAuthenticator("externalbrowser");
        Properties propertiesExpected = new Properties();
        propertiesExpected.put("user", "mock_user");
        propertiesExpected.put("authenticator", "externalbrowser");
        propertiesExpected.put("queryTimeout", 0);
        propertiesExpected.put("role", "SECURITYADMIN");
        propertiesExpected.put("logInTimeout", 60);
        propertiesExpected.put("networkTimeout", 0);
        propertiesExpected.put("application", "zoom-db-rbac");
        propertiesExpected.put("maxConnectionsPerRoute", 300);
        propertiesExpected.put("maxConnections", 300);
        Properties propertiesActual = SnowflakeConnectionService.connectionProperties(
                connectionProperties,
                SnowflakeRoleType.SECURITYADMIN);
        assertEquals(propertiesExpected, propertiesActual);
    }

    @Test
    void connectionPropertiesPrivateKey() throws NoSuchAlgorithmException, IOException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        PrivateKey privateKeyExpected = keyPair.getPrivate();
        byte[] privateKeyExpectedBytes = privateKeyExpected.getEncoded();
        PemObject pemObjectExpected = new PemObject("PRIVATE KEY", privateKeyExpectedBytes);
        File outputFile = new File(Paths.get(testDir.toPath().toString(), "private-key.pem").toUri());
        FileOutputStream outputStream = new FileOutputStream(outputFile);
        PemWriter pemWriter = new PemWriter(new OutputStreamWriter(outputStream));
        pemWriter.writeObject(pemObjectExpected);
        pemWriter.close();


        SnowflakeConnectionProperties connectionProperties = new SnowflakeConnectionProperties();
        connectionProperties.setUser("mock_user");
        connectionProperties.setPrivateKeyFile(outputFile.getAbsolutePath());

        // Read the file content and encode it to base64 to verify the correct value
        byte[] privateKeyBytes = Files.readAllBytes(outputFile.toPath());
        String expectedBase64 = Base64.getEncoder().encodeToString(privateKeyBytes);

        Properties propertiesExpected = new Properties();
        propertiesExpected.put("user", "mock_user");
        propertiesExpected.put("private_key_base64", expectedBase64);
        propertiesExpected.put("queryTimeout", 0);
        propertiesExpected.put("role", "SECURITYADMIN");
        propertiesExpected.put("logInTimeout", 60);
        propertiesExpected.put("networkTimeout", 0);
        propertiesExpected.put("application", "zoom-db-rbac");
        propertiesExpected.put("maxConnectionsPerRoute", 300);
        propertiesExpected.put("maxConnections", 300);
        Properties propertiesActual = SnowflakeConnectionService.connectionProperties(
                connectionProperties,
                SnowflakeRoleType.SECURITYADMIN);
        assertEquals(propertiesExpected, propertiesActual);
    }

    @Test
    void privateKeyFileToPrivateKeyBase64() throws IOException {
        // Test with null privateKeyFile
        SnowflakeConnectionProperties properties = new SnowflakeConnectionProperties();
        assertNull(properties.privateKeyFileToPrivateKeyBase64());

        // Test with a file containing known content
        String testContent = "This is a test private key content";
        File keyFile = new File(Paths.get(testDir.toPath().toString(), "test-key.pem").toUri());
        Files.write(keyFile.toPath(), testContent.getBytes(StandardCharsets.UTF_8));

        properties.setPrivateKeyFile(keyFile.getAbsolutePath());
        String expectedBase64 = Base64.getEncoder().encodeToString(testContent.getBytes(StandardCharsets.UTF_8));
        String actualBase64 = properties.privateKeyFileToPrivateKeyBase64();

        assertEquals(expectedBase64, actualBase64, "Base64 encoded content should match");

        // Test that getPrivateKeyBase64 calls privateKeyFileToPrivateKeyBase64 when privateKeyBase64 is null
        String base64FromGetter = properties.getPrivateKeyBase64();
        assertEquals(expectedBase64, base64FromGetter, "getPrivateKeyBase64 should return the same result as privateKeyFileToPrivateKeyBase64");

        // Test that getPrivateKeyBase64 returns privateKeyBase64 when it's not null
        String directBase64 = "DirectBase64Value";
        properties.setPrivateKeyBase64(directBase64);
        assertEquals(directBase64, properties.getPrivateKeyBase64(), "getPrivateKeyBase64 should return the directly set value");
    }
}
