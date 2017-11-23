package eu.fbk.dkm.pikes.eval;

import java.io.Writer;

import org.eclipse.rdf4j.rio.RDFHandler;

import eu.fbk.rdfpro.RDFSources;
import eu.fbk.rdfpro.util.IO;

public class Prettifier {

    public static void main(final String[] args) throws Throwable {
        try (Writer out = IO.utf8Writer(IO.buffer(IO.write(args[1])))) {
            final RDFHandler sink = new PrettyTurtle().getWriter(out);
            RDFSources.read(false, true, null, null, null,true, args[0]).emit(sink, 1);
        }
    }

}
