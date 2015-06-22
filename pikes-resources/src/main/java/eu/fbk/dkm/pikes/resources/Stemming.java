package eu.fbk.dkm.pikes.resources;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import org.tartarus.snowball.SnowballStemmer;

import javax.annotation.Nullable;
import java.util.Map;

public final class Stemming {

    private static final Map<String, Class<? extends SnowballStemmer>> CLASSES = Maps.newHashMap();

    private static final Map<String, String> LANGUAGES = buildAliasesMap();

    private static Map<String, String> buildAliasesMap() {
        final ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();
        addAliases(builder, "danish", "da", "dan");
        addAliases(builder, "dutch", "nl", "nld", "dut");
        addAliases(builder, "english", "en", "eng");
        addAliases(builder, "finnish", "fi", "fin");
        addAliases(builder, "french", "fr", "fra", "fre");
        addAliases(builder, "german", "de", "deu", "ger");
        addAliases(builder, "hungarian", "hu", "hun");
        addAliases(builder, "italian", "it", "ita");
        addAliases(builder, "norwegian", "nb", "nob");
        addAliases(builder, "portuguese", "pt", "por");
        addAliases(builder, "romanian", "ro", "ron", "rum");
        addAliases(builder, "russian", "ru", "rus");
        addAliases(builder, "spanish", "es", "spa");
        addAliases(builder, "swedish", "sw", "swe");
        addAliases(builder, "turkish", "tr", "tur");
        return builder.build();
    }

    private static void addAliases(final ImmutableMap.Builder<String, String> builder,
            final String snowballName, final String... aliases) {
        builder.put(snowballName, snowballName);
        for (final String alias : aliases) {
            builder.put(alias, snowballName);
        }
    }

    private static SnowballStemmer getStemmer(@Nullable final String lang) {
        final String actualLang = Objects.firstNonNull(lang, "en").toLowerCase();
        synchronized (CLASSES) {
            Class<? extends SnowballStemmer> stemmerClass = CLASSES.get(actualLang);
            if (stemmerClass == null) {
                final String snowballName = Objects.firstNonNull(LANGUAGES.get(actualLang),
                        actualLang);
                final String className = "org.tartarus.snowball.ext." + snowballName + "Stemmer";
                try {
                    stemmerClass = Class.forName(className).asSubclass(SnowballStemmer.class);
                } catch (final Throwable ex) {
                    throw new IllegalArgumentException("Unsupported language '" + lang + "'");
                }
                CLASSES.put(actualLang, stemmerClass);
            }
            try {
                return stemmerClass.newInstance();
            } catch (final Throwable ex) {
                throw new Error("Could not instantiate stemmer " + stemmerClass.getName());
            }
        }
    }

    public static String stem(@Nullable final String lang, final String string) {
        final SnowballStemmer stemmer = getStemmer(lang);
        stemmer.setCurrent(string);
        stemmer.stem();
        return stemmer.getCurrent();
    }

}
