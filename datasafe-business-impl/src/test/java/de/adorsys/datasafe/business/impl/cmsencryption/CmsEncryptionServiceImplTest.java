package de.adorsys.datasafe.business.impl.cmsencryption;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.*;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.security.KeyStore;
import java.security.MessageDigest;

import org.apache.commons.io.IOUtils;
import org.bouncycastle.util.encoders.Hex;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import de.adorsys.datasafe.business.api.deployment.keystore.KeyStoreService;
import de.adorsys.datasafe.business.api.deployment.keystore.types.KeyStoreAccess;
import de.adorsys.datasafe.business.api.deployment.keystore.types.KeyStoreAuth;
import de.adorsys.datasafe.business.api.deployment.keystore.types.KeyStoreCreationConfig;
import de.adorsys.datasafe.business.api.deployment.keystore.types.KeyStoreType;
import de.adorsys.datasafe.business.api.deployment.keystore.types.PublicKeyIDWithPublicKey;
import de.adorsys.datasafe.business.api.deployment.keystore.types.ReadKeyPassword;
import de.adorsys.datasafe.business.api.deployment.keystore.types.ReadStorePassword;
import de.adorsys.datasafe.business.api.encryption.cmsencryption.CMSEncryptionService;
import de.adorsys.datasafe.business.impl.cmsencryption.services.CMSEncryptionServiceImpl;
import de.adorsys.datasafe.business.impl.keystore.service.KeyStoreServiceImpl;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CmsEncryptionServiceImplTest {

    private CMSEncryptionService cmsEncryptionService = new CMSEncryptionServiceImpl();
    private KeyStoreService keyStoreService = new KeyStoreServiceImpl();
    private static final String MESSAGE_CONTENT = "message content";

    @Test
    @SneakyThrows
    public void cmsStreamEnvelopeEncryptAndDecryptTest() {
        ReadKeyPassword readKeyPassword = new ReadKeyPassword("readkeypassword");
        ReadStorePassword readStorePassword = new ReadStorePassword("readstorepassword");
        KeyStoreAuth keyStoreAuth = new KeyStoreAuth(readStorePassword, readKeyPassword);

        KeyStoreCreationConfig config = new KeyStoreCreationConfig(1, 0, 1);
        KeyStore keyStore = keyStoreService.createKeyStore(keyStoreAuth, KeyStoreType.DEFAULT, config);

        KeyStoreAccess keyStoreAccess = new KeyStoreAccess(keyStore, keyStoreAuth);

        PublicKeyIDWithPublicKey publicKeyIDWithPublicKey = keyStoreService.getPublicKeys(keyStoreAccess).get(0);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        OutputStream encryptionStream = cmsEncryptionService.buildEncryptionOutputStream(outputStream,
                publicKeyIDWithPublicKey.getPublicKey(), publicKeyIDWithPublicKey.getKeyID());

        encryptionStream.write(MESSAGE_CONTENT.getBytes());
        encryptionStream.close();
        byte[] byteArray = outputStream.toByteArray();

        ByteArrayInputStream inputStream = new ByteArrayInputStream(byteArray);
        InputStream decryptionStream = cmsEncryptionService.buildDecryptionInputStream(inputStream, keyStoreAccess);
        byte[] actualResult = IOUtils.toByteArray(decryptionStream);

        assertThat(MESSAGE_CONTENT).isEqualTo(new String(actualResult));
        log.debug("en and decrypted successfully");
    }

    @Test
    @SneakyThrows
    public void cmsEnvelopeEncryptAndDecryptFileStreamTest() {
        String folderPath = "target/";
        String testFilePath = folderPath + "test.dat";
        String encryptedFilePath = folderPath + "test_encrypted.dat";
        String decryptedTestFilePath = folderPath + "test_decrypted.dat";

        int _1MbInBytes = 1024 / 1024;
        int testFileSizeInBytes = 1024 * 1024 * 128; //128Mb
        double freeSpaceThresholdCoeff = 3.1;

        log.info("For the test, needed {}Mb free space", 3.1 * testFileSizeInBytes / _1MbInBytes);
        File testFilesDirectory = new File("target");
        if(testFilesDirectory.getFreeSpace() < testFileSizeInBytes * freeSpaceThresholdCoeff) {
            log.debug("Free disk space: {}, Size of one test file: {}", testFilesDirectory.getFreeSpace(), testFileSizeInBytes);
            Assertions.fail("Free space on the disk isn't enough for test, encrypted and decrypted files");
        }
        generateTestFile(testFilePath, testFileSizeInBytes);
        log.info("Test file with size {}Mb generated: {}", testFileSizeInBytes / _1MbInBytes, testFilePath);


        ReadKeyPassword readKeyPassword = new ReadKeyPassword("readkeypassword");
        ReadStorePassword readStorePassword = new ReadStorePassword("readstorepassword");
        KeyStoreAuth keyStoreAuth = new KeyStoreAuth(readStorePassword, readKeyPassword);

        KeyStoreCreationConfig config = new KeyStoreCreationConfig(1, 0, 1);

        KeyStore keyStore = keyStoreService.createKeyStore(keyStoreAuth, KeyStoreType.DEFAULT, config);
        KeyStoreAccess keyStoreAccess = new KeyStoreAccess(keyStore, keyStoreAuth);
        PublicKeyIDWithPublicKey publicKeyIDWithPublicKey = keyStoreService.getPublicKeys(keyStoreAccess).get(0);

        File testFile = new File(testFilePath);
        FileInputStream fisTestFile = new FileInputStream(testFile);

        File encryptedFile = new File(encryptedFilePath);
        FileOutputStream fosEnFile = new FileOutputStream(encryptedFile);
        log.debug("Total file size to read (in Mb) : " + fisTestFile.available() / _1MbInBytes);

        OutputStream encryptionStream = cmsEncryptionService.buildEncryptionOutputStream(
                fosEnFile, publicKeyIDWithPublicKey.getPublicKey(), publicKeyIDWithPublicKey.getKeyID());
        transferFromInputToOutputStream(fisTestFile, encryptionStream);

        IOUtils.closeQuietly(encryptionStream);
        IOUtils.closeQuietly(fisTestFile);
        IOUtils.closeQuietly(fosEnFile);
        log.info("File encrypted");


        FileInputStream fisEnFile = new FileInputStream(encryptedFile);
        InputStream decryptionStream = cmsEncryptionService.buildDecryptionInputStream(fisEnFile, keyStoreAccess);

        File decryptedFile = new File(decryptedTestFilePath);
        OutputStream osDecrypt = new FileOutputStream(decryptedFile);

        transferFromInputToOutputStream(decryptionStream, osDecrypt);
        log.info("File decrypted");

        IOUtils.closeQuietly(osDecrypt);
        IOUtils.closeQuietly(fisEnFile);
        IOUtils.closeQuietly(decryptionStream);


        String checksumOfOriginTestFile = checksum(testFile);
        String checksumOfDecryptedTestFile = checksum(decryptedFile);

        log.info("Origin test file checksum hex: {}", checksumOfOriginTestFile);
        log.info("Decrypted test file checksum hex: {}", checksumOfDecryptedTestFile);
        Assertions.assertEquals(checksumOfOriginTestFile, checksumOfDecryptedTestFile);

        if(testFile.delete() && encryptedFile.delete() && decryptedFile.delete()) {
            log.trace("Test files were deleted");
        }
    }

    private void generateTestFile(String testFilePath, int testFileSizeInBytes) throws IOException {
        RandomAccessFile originTestFile = new RandomAccessFile(testFilePath, "rw");
        MappedByteBuffer out = originTestFile.getChannel()
                .map(FileChannel.MapMode.READ_WRITE, 0, testFileSizeInBytes);

        for (int i = 0; i < testFileSizeInBytes; i++) {
            out.put((byte) 'x');
        }
    }

    private void transferFromInputToOutputStream(InputStream fisTestFile, OutputStream encryptionStream) throws IOException {
        int content;
        byte[] buffer = new byte[8 * 1024]; //8kb
        while ((content = fisTestFile.read(buffer)) != -1) {
            encryptionStream.write(buffer, 0, content);
        }
    }

    @SneakyThrows
    public String checksum(File input) {
        try (InputStream in = new FileInputStream(input)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] block = new byte[4096];
            int length;
            while ((length = in.read(block)) > 0) {
                digest.update(block, 0, length);
            }
            return Hex.toHexString(digest.digest());
        }
    }
}
