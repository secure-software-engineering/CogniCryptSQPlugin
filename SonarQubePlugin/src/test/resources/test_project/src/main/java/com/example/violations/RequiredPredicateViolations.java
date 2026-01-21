package com.example.violations;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.nio.charset.StandardCharsets;
import java.security.*;

/**
 * The methods in this class should produce either RequiredPredicateErrors or AlternativeReqPredicateErrors
 */
public class RequiredPredicateViolations {

    public RequiredPredicateViolations() throws NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException, SignatureException {
        requiredPredicate();
        alternativeReqPredicate(new Key() {
            @Override
            public String getAlgorithm() {
                return "";
            }

            @Override
            public String getFormat() {
                return "";
            }

            @Override
            public byte[] getEncoded() {
                return new byte[0];
            }
        });
    }

    public void requiredPredicate() throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        Signature sig = Signature.getInstance("SHA256withRSA");
        OtherClass util = new OtherClass();
        sig.initSign(util.getPrivate());
        sig.update("test".getBytes(StandardCharsets.UTF_8));
        sig.sign();
    }

    public void alternativeReqPredicate(Key key) throws InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException, IllegalBlockSizeException, BadPaddingException {
        Cipher c = Cipher.getInstance("AES/CTR/NoPadding");
        c.getIV();
        c.init(Cipher.ENCRYPT_MODE, key);
        c.doFinal();
    }
}
