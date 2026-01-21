package com.example.locationtests;

import javax.crypto.KeyAgreement;
import javax.crypto.spec.DHGenParameterSpec;
import java.security.*;

/**
 * This class contains examples to test that the exact positions w.r.t. line and offset are found.
 */
public class ExactLocations {

    /**
     * Example to test handling of nested method calls and multiple constructors
     * @throws NoSuchAlgorithmException
     * @throws InvalidAlgorithmParameterException
     * @throws InvalidKeyException
     */
    public void nestedMethodCall() throws NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException {
        KeyPairGenerator kpGen = KeyPairGenerator.getInstance("DH");
        kpGen.initialize(2048); // insecure keysize (line 26)

        byte[] seed = "static seed".getBytes(); // insecure seed (line 30)
        KeyAgreement keyAgreement = KeyAgreement.getInstance("DH");
        keyAgreement.init(
                // this method call goes over multiple lines
                kpGen.generateKeyPair().getPrivate(),
                // we add comments to show that the location
                new DHGenParameterSpec(5, 10),
                // reported by CogniCrypt is not very accurate
                new SecureRandom(seed)
        );
    }
}
