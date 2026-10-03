package io.github.brainage04.genshininminecraft.client;

import io.github.brainage04.genshininminecraft.rules.Element;
import io.github.brainage04.genshininminecraft.rules.Reaction;

/** Original text/shape palette, not extracted game artwork. Colours are opaque ARGB. */
public final class ElementPalette {
    private ElementPalette() {}
    public static int color(Element element) {
        return switch (element) {
            case ANEMO -> 0xff72e2c3;
            case PYRO -> 0xffff905b;
            case CRYO -> 0xffa2e8f5;
            case ELECTRO -> 0xffcf9bff;
            case HYDRO -> 0xff69baff;
            case PHYSICAL -> 0xfff5f2e9;
        };
    }
    public static String symbol(Element element) {
        return switch (element) {
            case ANEMO -> "A";
            case PYRO -> "P";
            case CRYO -> "C";
            case ELECTRO -> "E";
            case HYDRO -> "H";
            case PHYSICAL -> "-";
        };
    }
    public static String reaction(Reaction.Type reaction) {
        return switch (reaction) {
            case SWIRL -> "Swirl";
            case MELT -> "Melt";
            case VAPORIZE -> "Vaporize";
            case OVERLOADED -> "Overloaded";
            case SUPERCONDUCT -> "Superconduct";
            case ELECTRO_CHARGED -> "Electro-Charged";
            case FROZEN -> "Frozen";
            case SHATTER -> "Shatter";
        };
    }
}
