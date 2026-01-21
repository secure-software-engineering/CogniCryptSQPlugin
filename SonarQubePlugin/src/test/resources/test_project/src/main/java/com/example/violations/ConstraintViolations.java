package com.example.violations;

import javax.crypto.*;
import javax.crypto.spec.DHGenParameterSpec;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.net.PasswordAuthentication;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAKeyGenParameterSpec;

/**
 * All methods in this class should produce one ConstraintError
 */
public class ConstraintViolations {

    public ConstraintViolations() throws NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, InvalidKeySpecException, BadPaddingException, InvalidKeyException {
        setConstraint();
        comparisonConstraint();
        comparisonConstraint2();
        notHardCoded();
        neverTypeOf();
        noCallTo();
        callTo();
        instanceOf();
    }

    /**
     * An error should be detected because encmode in init should be in {1,2,3,4} and here it is 5.
     * @throws NoSuchPaddingException
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeySpecException
     * @throws InvalidKeyException
     * @throws IllegalBlockSizeException
     * @throws BadPaddingException
     */
    public void setConstraint() throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        SecretKey secretKey = KeyGenerator.getInstance("AES").generateKey();

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(5//, secretKey);
        , secretKey);
        cipher.doFinal();

        int keySize = 2048;
        RSAKeyGenParameterSpec rsaKeyGenParameterSpec = new RSAKeyGenParameterSpec(keySize, RSAKeyGenParameterSpec.F0);
    }

    /**
     * An error should be detected because the first parameter should be greater than the second
     */
    public void comparisonConstraint() {
        DHGenParameterSpec dhGenParameterSpec = new DHGenParameterSpec(5,
                10);
    }

    /**
     * An error should be detected because the first parameter should be greater than the second
     */
    public void comparisonConstraint2() {
        GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(128, "src".getBytes(), 0, 10);

        // different constraint in same rule
        GCMParameterSpec gcmParameterSpec2 = new GCMParameterSpec(128, "src".getBytes(), -1, 2);
    }

    /**
     * An error should be detected because the second parameter should not be hard coded
     */
    public void notHardCoded() {
        char[] password = new char[]{'p', 'a', 's', 's', 'w', 'o', 'r', 'd'}; /*writing unnecessary
        stuff*/PasswordAuthentication passwordAuthentication = new PasswordAuthentication(/*comment*/"user", /*nonsense*/password);
    }

    /**
     * An error will be detected because the char[] was created from a String.
     * An additional error will be reported because the password is hard coded
     */
    public void neverTypeOf() {
        char[] password = "password".toCharArray();
        PasswordAuthentication passwordAuthentication = new PasswordAuthentication("user", password);
    }

    /**
     * An error should be detected because the final method call is not allowed.
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeyException
     */
    public void noCallTo() throws NoSuchAlgorithmException, InvalidKeyException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("DH");
        keyPairGenerator.initialize(3072);

        KeyPair keyPair = keyPairGenerator.generateKeyPair();

        KeyAgreement keyAgreement = KeyAgreement.getInstance("DH");
        keyAgreement.init(keyPair.getPrivate());
        keyAgreement.doPhase(keyPair.getPublic(), true);
        //keyAgreement.generateSecret();
        keyAgreement.generateSecret("Algorithm");
    }

    /**
     * An error should be detected because a call to getIV is missing //removed for current version of CogniCrypt
     * @throws NoSuchAlgorithmException
     * @throws NoSuchPaddingException
     * @throws InvalidKeyException
     * @throws IllegalBlockSizeException
     * @throws BadPaddingException
     * @throws InvalidKeySpecException
     */
    public void callTo() throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, InvalidKeySpecException {
        SecretKey secretKey = KeyGenerator.getInstance("AES").generateKey();

        Cipher cipher = Cipher.getInstance("AES/CTR/NoPadding");
        cipher.init(1, secretKey);
        cipher.doFinal();
    }

    public void instanceOf() {
        // there is no case where this would cause an error
    }
}
