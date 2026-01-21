package com.example.locationtests;

import javax.crypto.spec.DHGenParameterSpec;

/**
 * This class contains examples to test the locating of the last change to a variable.
 */
public class OriginLocations {

    public int prime = 1;

    public OriginLocations() {
        globalVar();
        methodParameter(5);
        methodParameterLastChange(5);
        lastChange();
    }

    /**
     * Here, the global variable in line 10 should be highlighted
     */
    public void globalVar() {
        DHGenParameterSpec spec = new DHGenParameterSpec(prime, 15);
    }

    /**
     * Here, the method parameter (in the header in line 30) should be highlighted and not the global variable
     * @param prime
     */
    public void methodParameter(int prime) {
        DHGenParameterSpec spec = new DHGenParameterSpec(prime, 15);
    }

    /**
     * Here, the location should be line 39 as the method parameter was changed later
     * @param prime
     */
    public void methodParameterLastChange(int prime) {
        prime = 3;
        DHGenParameterSpec spec = new DHGenParameterSpec(prime, 15);
    }

    /**
     * Here, the last change to the variable should be marked (line 49)
     */
    public void lastChange() {
        int prime = 1;
        prime = 2;
        prime = 3;
        DHGenParameterSpec spec = new DHGenParameterSpec(prime, 15);
    }
}
