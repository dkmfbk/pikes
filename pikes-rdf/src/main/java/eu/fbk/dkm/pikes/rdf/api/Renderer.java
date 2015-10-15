package eu.fbk.dkm.pikes.rdf.api;

import java.util.Map;

import javax.annotation.Nullable;

import com.github.mustachejava.Mustache;

import eu.fbk.rdfpro.util.QuadModel;

public interface Renderer {

    void render(QuadModel model, @Nullable Annotation annotation, Appendable out) throws Exception;

    static Renderer newTemplateRenderer(@Nullable final Mustache template,
            @Nullable final Map<String, Object> templateArgs,
            @Nullable final Map<Object, String> colorMap) {
        // TODO
        return null;
    }

}
