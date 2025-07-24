package us.zoom.data.dfence.cryptography;

import jakarta.validation.constraints.NotEmpty;
import lombok.NonNull;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.openssl.PEMException;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JceOpenSSLPKCS8DecryptorProviderBuilder;
import org.bouncycastle.operator.InputDecryptorProvider;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.pkcs.PKCS8EncryptedPrivateKeyInfo;
import org.bouncycastle.pkcs.PKCSException;
import us.zoom.data.dfence.exception.PrivateKeyParseException;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Paths;
import java.security.PrivateKey;
import java.util.Base64;

public class PrivateKeyReader {

    public static PrivateKey privateKey(@NonNull PEMParser pemParser, String passphrase)
            throws PrivateKeyParseException {
        Object pemObject = null;
        try {
            pemObject = pemParser.readObject();
        } catch (IOException e) {
            throw new PrivateKeyParseException("Failed to read the private key object.", e);
        }
        PrivateKeyInfo privateKeyInfo;
        if (pemObject instanceof PKCS8EncryptedPrivateKeyInfo encryptedPrivateKeyInfo) {
            // Handle the case where the private key is encrypted.
            if (passphrase == null) {
                throw new PrivateKeyParseException("A passphrase is required when " + "decoding an encrypted private key. Either decrypt the key " + "ahead of time or provide a private key password.");
            }
            InputDecryptorProvider pkcs8Prov;
            try {
                pkcs8Prov = new JceOpenSSLPKCS8DecryptorProviderBuilder().build(passphrase.toCharArray());
            } catch (OperatorCreationException e) {
                throw new PrivateKeyParseException("Failed to build input decryptor" + " provider.", e);
            }
            try {
                privateKeyInfo = encryptedPrivateKeyInfo.decryptPrivateKeyInfo(pkcs8Prov);
            } catch (PKCSException e) {
                throw new PrivateKeyParseException("Failed to decrypt private key " + "info.", e);
            }
        } else if (pemObject instanceof PrivateKeyInfo) {
            // Handle the case where the private key is unencrypted.
            privateKeyInfo = (PrivateKeyInfo) pemObject;
        } else if (pemObject == null) {
            throw new PrivateKeyParseException("No PEM object created.");
        } else {
            throw new PrivateKeyParseException(String.format(
                    "Unsupported class %s for pemObject",
                    pemObject.getClass().getName()));
        }
        JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
        try {
            return converter.getPrivateKey(privateKeyInfo);
        } catch (PEMException e) {
            throw new PrivateKeyParseException("Failed to get private key information.", e);
        }
    }

    public static PrivateKey privateKey(@NotEmpty byte[] value, String passphrase) throws PrivateKeyParseException {
        String privateKeyString = new String(value);
        try (PEMParser pemParser = new PEMParser(new StringReader(privateKeyString))) {
            return privateKey(pemParser, passphrase);
        } catch (IOException e) {
            throw new PrivateKeyParseException("An error occurred while reading the private key " + "string", e);
        }

    }

    public static PrivateKey privateKeyFromB64(@NotEmpty String value, String passphrase)
            throws PrivateKeyParseException {
        byte[] privateKeyBytes = Base64.getDecoder().decode(value);
        return privateKey(privateKeyBytes, passphrase);
    }

    public static PrivateKey privateKeyFromFile(@NotEmpty String filename, String passphrase)
            throws PrivateKeyParseException, FileNotFoundException {
        PEMParser pemParser = new PEMParser(new FileReader(Paths.get(filename).toFile()));
        return privateKey(pemParser, passphrase);
    }
}
