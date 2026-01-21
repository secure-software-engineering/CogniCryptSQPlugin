package org.sonarsource.plugins.secai.reporting;

import java.util.Objects;

public class Parameter {

    private final Location location;
    private final String value;
    private final int index;

    private String jimpleVar = null;

    private Location secondaryLoc = null;
    private boolean fromMethodParameter = false;

    public Parameter(Location location, String value, int index) {
        this.location = location;
        this.value = value;
        this.index = index;
    }

    public void addJimpleVar(String jimpleVar) {
        this.jimpleVar = jimpleVar;
    }

    public String getJimpleVar() {
        return jimpleVar;
    }

    public boolean isVariable() {
        return value.matches("[\\w&&\\D]\\w*");
    }

    public void addSecondaryLoc(Location location) {
        secondaryLoc = location;
    }

    public Location getSecondaryLoc() {
        return secondaryLoc;
    }

    public void setFromMethodParameter(boolean b) {
        fromMethodParameter = b;
    }

    public boolean isFromMethodParameter() {
        return fromMethodParameter;
    }

    public Location getLocation() {
        return location;
    }

    public String getValue() {
        return value;
    }

    public int getIndex() {
        return index;
    }

    @Override
    public String toString() {
        return "Parameter{" +
                "location=" + location +
                ", value='" + value + '\'' +
                ", index=" + index +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Parameter parameter)) return false;
        return index == parameter.index && Objects.equals(location, parameter.location) && Objects.equals(value, parameter.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(location, value, index);
    }
}
