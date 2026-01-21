package com.example;

import com.example.locationtests.ExactLocations;
import com.example.locationtests.OriginLocations;
import com.example.violations.ConstraintViolations;
import com.example.violations.OrderViolations;
import com.example.violations.OtherViolations;
import com.example.violations.RequiredPredicateViolations;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.File;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;

public class Main {

    public static void main(String[] args) {
        ///*
        try {
            // while all classes test that the entire error was analyzed correctly,
            // the examples are laid out to cover different areas

            // cover possible messages and data extraction
            new ConstraintViolations();
            new OrderViolations();
            new RequiredPredicateViolations();
            new OtherViolations();

            // cover possible locations
            new OriginLocations();
            new ExactLocations().nestedMethodCall();
        } catch (NoSuchPaddingException | BadPaddingException | InvalidKeyException | InvalidKeySpecException |
                 NoSuchAlgorithmException | IllegalBlockSizeException | SignatureException |
                 InvalidAlgorithmParameterException e) {
            throw new RuntimeException(e);
        }//*/

    }
}