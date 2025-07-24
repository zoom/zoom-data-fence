package us.zoom.security.dfence.cryptography;

import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemWriter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.*;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.util.Base64;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

class PrivateKeyReaderTest {
    @TempDir
    File testDir;

    @Test
    void privateKeyFromB64() throws NoSuchAlgorithmException, IOException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        PrivateKey privateKeyExpected = keyPair.getPrivate();
        byte[] privateKeyExpectedBytes = privateKeyExpected.getEncoded();
        PemObject pemObjectExpected = new PemObject("PRIVATE KEY", privateKeyExpectedBytes);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        PemWriter pemWriter = new PemWriter(new OutputStreamWriter(byteArrayOutputStream));
        pemWriter.writeObject(pemObjectExpected);
        pemWriter.close();
        String pemString = byteArrayOutputStream.toString();
        String pemStringEncoded = Base64.getEncoder().encodeToString(pemString.getBytes());
        PrivateKey privateKeyActual = PrivateKeyReader.privateKeyFromB64(pemStringEncoded, null);
        assertThat(privateKeyActual, equalTo(privateKeyExpected));
    }

    @Test
    void privateKeyFromFile() throws NoSuchAlgorithmException, IOException {
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
        PrivateKey privateKeyActual = PrivateKeyReader.privateKeyFromFile(outputFile.getAbsolutePath(), null);
        assertThat(privateKeyActual, equalTo(privateKeyExpected));
    }
}