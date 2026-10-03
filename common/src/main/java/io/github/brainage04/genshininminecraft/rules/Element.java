package io.github.brainage04.genshininminecraft.rules;

public enum Element {
    ANEMO, PYRO, CRYO, ELECTRO, HYDRO, PHYSICAL;

    /** Geo/Dendro and their reactions are deliberately outside the starter rules scope. */
    public boolean canBeAura() {
        return this == PYRO || this == CRYO || this == ELECTRO || this == HYDRO;
    }
}
