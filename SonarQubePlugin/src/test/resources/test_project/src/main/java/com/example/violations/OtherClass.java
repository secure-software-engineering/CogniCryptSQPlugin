package com.example.violations;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;

public class OtherClass {

    public PrivateKey getPrivate() throws NoSuchAlgorithmException {
        KeyPairGenerator kpGen = KeyPairGenerator.getInstance("RSA");
        kpGen.initialize(40);
        KeyPair keyPair = kpGen.generateKeyPair();
        return keyPair.getPrivate();
    }
}
