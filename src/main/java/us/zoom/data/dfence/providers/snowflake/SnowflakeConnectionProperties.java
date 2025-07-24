package us.zoom.data.dfence.providers.snowflake;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import java.security.PrivateKey;

/*
See https://docs.snowflake.com/en/user-guide/jdbc-parameters.html#authenticator for
 docs.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class SnowflakeConnectionProperties {
    final String application = "zoom-db-rbac";
    @NotNull
    String user;
    @Pattern(
            regexp = "^(snowflake|externalbrowser|https://.*|oauth|snowflake_jwt|snowflake_password_mfa)?$")
    String authenticator;
    String passcode;
    @Pattern(regexp = "^on|off$")
    String passcodeInPassword;
    String password;
    String token;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    String securityAdminRole = "SECURITYADMIN";
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    String sysAdminRole = "SYSADMIN";
    String warehouse;
    int logInTimeout = 60;
    int networkTimeout = 0;
    int queryTimeout = 0;
    int maxConnections = 300;
    int maxConnectionsPerRoute = 300;

    String privateKeyBase64;

    String privateKeyFile;

    String privateKeyPwd;

    PrivateKey privateKey;

    @JsonProperty("private_key_base64")
    public String getPrivateKeyBase64() {
        if (privateKeyBase64 != null) {
            return privateKeyBase64;
        }
        // Read file once so that Hikari does not try to read it over and over with every new connection.
        return privateKeyFileToPrivateKeyBase64();
    }
    public String privateKeyFileToPrivateKeyBase64() {
        if (privateKeyFile == null) {
            return null;
        }
        try {
            byte[] privateKeyBytes = java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(privateKeyFile));
            return java.util.Base64.getEncoder().encodeToString(privateKeyBytes);
        } catch (java.io.IOException e) {
            throw new RuntimeException("Failed to read private key file: " + privateKeyFile, e);
        }
    }

    @JsonProperty("private-key-base64")
    public void setPrivateKeyBase64(String privateKeyBase64) {
        this.privateKeyBase64 = privateKeyBase64;
    }

    @JsonIgnore()
    public String getPrivateKeyFile() {
        return privateKeyFile;
    }

    @JsonProperty("private-key-file")
    public void setPrivateKeyFile(String privateKeyFile) {
        this.privateKeyFile = privateKeyFile;
    }

    @JsonProperty("private_key_pwd")
    public String getPrivateKeyPwd() {
        return privateKeyPwd;
    }

    @JsonProperty("private-key-pwd")
    public void setPrivateKeyPwd(String privateKeyPwd) {
        this.privateKeyPwd = privateKeyPwd;
    }
}
