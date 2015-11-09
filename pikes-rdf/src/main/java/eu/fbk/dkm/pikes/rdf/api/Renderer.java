package eu.fbk.dkm.pikes.rdf.api;

import java.util.Map;

import javax.annotation.Nullable;

import com.github.mustachejava.Mustache;

public interface Renderer {

    void render(Document document, Appendable out) throws Exception;

    static Renderer newTemplateRenderer(@Nullable final Mustache template,
            @Nullable final Map<String, Object> templateArgs,
            @Nullable final Map<Object, String> colorMap) {
        // TODO
        return null;
    }

}
