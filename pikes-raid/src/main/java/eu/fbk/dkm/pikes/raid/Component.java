package eu.fbk.dkm.pikes.raid;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

public enum Component {

    POLARITY('p'),

    EXPRESSION('e', "NN", "VB", "JJ", "R"),

    HOLDER('h', "NN", "PRP", "JJP", "DTP", "WP"),

    TARGET('t', "NN", "PRP", "JJP", "DTP", "WP", "VB");

    private char letter;

    private String[] headPos;

    private Component(final char letter, final String... headPos) {
        this.letter = letter;
        this.headPos = headPos;
    }

    public char getLetter() {
        return this.letter;
    }

    public String[] getHeadPos() {
        return this.headPos;
    }

    public static Component forLetter(final char letter) {
        final char c = Character.toLowerCase(letter);
        for (final Component component : values()) {
            if (component.letter == c) {
                return component;
            }
        }
        throw new IllegalArgumentException("No component for letter '" + letter + "'");
    }

    public static Set<Component> forLetters(final String letters) {
        final EnumSet<Component> result = EnumSet.noneOf(Component.class);
        for (int i = 0; i < letters.length(); ++i) {
            result.add(forLetter(letters.charAt(i)));
        }
        return result;
    }

    static final EnumSet<Component> UNIVERSE = EnumSet.allOf(Component.class);

    static EnumSet<Component> toSet(@Nullable final Component... components) {
        return components == null || components.length == 0 ? UNIVERSE : EnumSet.copyOf(Arrays
                .asList(components));
    }

}