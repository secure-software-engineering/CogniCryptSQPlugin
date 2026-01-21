package com.example.violations;

import javax.crypto.spec.DHGenParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import java.net.PasswordAuthentication;
import java.security.NoSuchAlgorithmException;

public class OtherViolations {

    public OtherViolations() throws NoSuchAlgorithmException {
        forbiddenMethod();
    }

    public void impreciseValueExtraction(int prime) {
        DHGenParameterSpec dhGenParameterSpec = new DHGenParameterSpec(prime, 10);
    }

    public void forbiddenMethod() throws NoSuchAlgorithmException {
        PBEKeySpec spec = new PBEKeySpec(new char[]{'p', 'w', 'd'});
    }
}
