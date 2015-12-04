package eu.fbk.dkm.pikes.query;

import java.io.File;
import java.io.Writer;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;
import com.google.common.xml.XmlEscapers;

import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.vocabulary.DC;
import org.openrdf.model.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.fbk.dkm.utils.CommandLine;
import eu.fbk.dkm.utils.vocab.NIF;
import eu.fbk.rdfpro.RDFSources;
import eu.fbk.rdfpro.util.IO;
import eu.fbk.rdfpro.util.QuadModel;
import eu.fbk.rdfpro.util.Statements;

public final class Yovisto {

    private static final Logger LOGGER = LoggerFactory.getLogger(Yovisto.class);

    private static final ValueFactory VF = Statements.VALUE_FACTORY;

    private static final URI SI_QUERY = VF.createURI("http://sindice.com/vocab/search#Query");

    private static final URI SI_RESULT = VF.createURI("http://sindice.com/vocab/search#result");

    private static final URI SI_RANK = VF.createURI("http://sindice.com/vocab/search#rank");

    private static final URI YV_QUERY_ID = VF.createURI("http://yovisto.com/eval#queryId");

    private static final URI YV_DOCUMENT_ID = VF.createURI("http://yovisto.com/eval#documentId");

    private static final URI YV_DOCUMENT = VF.createURI("http://yovisto.com/eval#Document");

    private static final URI ITSRDF_TA_IDENT_REF = VF
            .createURI("http://www.w3.org/2005/11/its/rdf#taIdentRef");

    private static final Pattern SPLIT_PATTERN = Pattern
            .compile("[^ ][ ]([ ]+[A-Z]|The |This |That |These |Those |My |Your |His |Her |Its "
                    + "|Our |Their |Whose |A |An |Some |Any |Much |Many |Little |Few |More |Most "
                    + "|Less |Fewer |Least |Fewest |Very |Too |So |Not |Lots of |Plenty of "
                    + "|Half of |Twice |All |Both |Enough |No |Almost |Over |More than "
                    + "|Less than |Each |Every |Either |Neither |You |He [a-zA-Z]|She |We |They "
                    + "|Such |What |On |In |At |Since |For |After |Before |To |Until |By |Beside "
                    + "|Under |Below |Over |Above |Across |Through |Into |Towards |Onto |From "
                    + "|Off |Out of |About |But |And |Or |Although |As |Even |If |Now |Once "
                    + "|Rather |Since |That |Though |Unless |When |Whenever |Where |Whereas "
                    + "|Wherever |While |Whether |However |Moreover |Nevertheless |Consequently "
                    + "|Already |Throughout |Further |Back |Also |Because |Finally )");

    private static final Pattern REMOVE_PATTERN = Pattern.compile("\\[[0-9]+(,[0-9]+)*\\]");

    public static void main(final String[] args) {
        try {
            // Parse command line
            final CommandLine cmd = CommandLine
                    .parser()
                    .withName("yovisto")
                    .withOption("i", "input", "the input RDF file with the Yovisto dataset",
                            "PATH", CommandLine.Type.FILE_EXISTING, true, false, true)
                    .withOption("o", "output", "output base name", "PATH",
                            CommandLine.Type.STRING, true, false, true)
                    .withHeader("parses the Yovisto file and emits NAF files for each document")
                    .parse(args);

            // Extract options
            final File input = cmd.getOptionValue("i", File.class);
            final String output = cmd.getOptionValue("o", String.class);

            // Read RDF file
            final QuadModel model = QuadModel.create();
            for (final Statement stmt : RDFSources.read(false, true, null, null,
                    input.getAbsolutePath())) {
                try {
                    model.add(stmt);
                } catch (final Throwable ex) {
                    LOGGER.error("Ignoring wrong statement: " + stmt);
                }
            }

            // Define a URI -> ID map
            final Map<URI, String> ids = Maps.newHashMap();

            // Emit queries
            int numResults = 0;
            final List<String> queryLines = Lists.newArrayList();
            for (final Resource query : model.filter(null, RDF.TYPE, SI_QUERY).subjects()) {
                final String id = String.format("q%02d", model.filter(query, YV_QUERY_ID, null)
                        .objectLiteral().intValue());
                ids.put((URI) query, id);
                final String text = fixQuery(model.filter(query, NIF.IS_STRING, null)
                        .objectLiteral().stringValue());
                final Map<Integer, String> resultMap = Maps.newHashMap();
                final Map<String, Integer> rankMap = Maps.newHashMap();
                for (final Value result : model.filter(query, SI_RESULT, null).objects()) {
                    final URI uri = (URI) result;
                    final int num = Integer.parseInt(uri.getLocalName());
                    final int rank = model.filter(uri, SI_RANK, null).objectLiteral().intValue();
                    final String documentId = String.format("d%03d",
                            model.filter(uri, YV_DOCUMENT_ID, null).objectLiteral().intValue());
                    resultMap.put(num, documentId);
                    rankMap.put(documentId, rank);
                }
                final StringBuilder builder = new StringBuilder();
                builder.append(id).append('\t').append(text).append("\t");
                String separator = "";
                for (final Integer num : Ordering.natural().sortedCopy(resultMap.keySet())) {
                    final String documentId = resultMap.get(num);
                    final Integer rank = rankMap.get(documentId);
                    builder.append(separator).append(documentId).append(':').append(rank);
                    separator = ",";
                    ++numResults;
                }
                queryLines.add(builder.toString());
                final int index = query.stringValue().indexOf('#');
                final URI queryURI = index < 0 ? (URI) query : VF.createURI(query.stringValue()
                        .substring(0, index));
                try (Writer writer = IO
                        .utf8Writer(IO.buffer(IO.write(output + "." + id + ".naf")))) {
                    writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
                    writer.write("<NAF xml:lang=\"en\" version=\"v3\">\n");
                    writer.write("  <nafHeader>\n");
                    writer.write("    <fileDesc creationtime=\"2015-07-09T00:00:00+00:00\" />\n");
                    writer.write("    <public publicId=\""
                            + XmlEscapers.xmlAttributeEscaper().escape(id) + "\" uri=\""
                            + XmlEscapers.xmlAttributeEscaper().escape(queryURI.stringValue())
                            + "\"/>\n");
                    writer.write("  </nafHeader>\n");
                    writer.write("  <raw><![CDATA[");
                    writer.write(text);
                    writer.write("]]></raw>\n");
                    writer.write("</NAF>\n");
                }
            }
            try (Writer writer = IO.utf8Writer(IO.buffer(IO.write(output + ".queries")))) {
                for (final String line : Ordering.natural().sortedCopy(queryLines)) {
                    writer.write(line);
                    writer.write("\n");
                }
            }
            LOGGER.info("Emitted {} queries with {} results", queryLines.size(), numResults);

            // Emit NAF documents
            for (final Resource document : model.filter(null, RDF.TYPE, YV_DOCUMENT).subjects()) {
                final String id = String.format("d%03d",
                        model.filter(document, YV_DOCUMENT_ID, null).objectLiteral().intValue());
                final String title = model.filter(document, DC.TITLE, null).objectLiteral()
                        .stringValue().trim();
                final String text = fixDocument(model.filter(document, NIF.IS_STRING, null)
                        .objectLiteral().stringValue());
                final int index = document.stringValue().indexOf('#');
                final URI documentURI = index < 0 ? (URI) document : VF.createURI(document
                        .stringValue().substring(0, index));
                ids.put((URI) document, id);
                try (Writer writer = IO
                        .utf8Writer(IO.buffer(IO.write(output + "." + id + ".naf")))) {
                    writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
                    writer.write("<NAF xml:lang=\"en\" version=\"v3\">\n");
                    writer.write("  <nafHeader>\n");
                    writer.write("    <fileDesc creationtime=\"2015-07-09T00:00:00+00:00\" title=\""
                            + XmlEscapers.xmlAttributeEscaper().escape(title) + "\"/>\n");
                    writer.write("    <public publicId=\""
                            + XmlEscapers.xmlAttributeEscaper().escape(id) + "\" uri=\""
                            + XmlEscapers.xmlAttributeEscaper().escape(documentURI.stringValue())
                            + "\"/>\n");
                    writer.write("  </nafHeader>\n");
                    writer.write("  <raw><![CDATA[");
                    writer.write(title);
                    if (!title.endsWith(".")) {
                        writer.write(".");
                    }
                    writer.write("\n\n");
                    writer.write(text);
                    writer.write("]]></raw>\n");
                    writer.write("</NAF>\n");
                }
            }

            // Emit entities
            final Map<String, String> entityLines = Maps.newHashMap();
            for (final Statement stmt : model.filter(null, ITSRDF_TA_IDENT_REF, null)) {
                final URI entity = (URI) stmt.getSubject();
                final URI reference = VF.createURI(stmt.getObject().stringValue());
                final URI context = VF.createURI(model.filter(entity, NIF.REFERENCE_CONTEXT, null)
                        .objectValue().stringValue());
                final String id = ids.get(context);
                final String text = model.filter(entity, NIF.ANCHOR_OF, null).objectLiteral()
                        .stringValue();
                final int begin = getInt(model, entity, NIF.BEGIN_INDEX);
                final int end = getInt(model, entity, NIF.END_INDEX);
                entityLines.put(
                        id + String.format("%04d", begin),
                        String.format("%s\t%d\t%d\t%s\t%s", id, begin, end, text,
                                reference.stringValue()));
            }
            try (Writer writer = IO.utf8Writer(IO.buffer(IO.write(output + ".entities")))) {
                for (final String key : Ordering.natural().sortedCopy(entityLines.keySet())) {
                    writer.append(entityLines.get(key));
                    writer.append("\n");
                }
            }

        } catch (final Throwable ex) {
            // Display error information and terminate
            CommandLine.fail(ex);
        }
    }

    private static String fixQuery(String string) {
        if (string.equals("Famous German Poetry")) {
            string = "famous German poetry";
        } else if (string.equals("University of Edinburgh Research")) {
            string = "University of Edinburgh research";
        } else if (string.equals("Bridge Construction")) {
            string = "bridge construction";
        } else if (string.equals("Walk of fame stars")) {
            string = "Walk of Fame stars";
        } else if (string.equals("Invention of the internet")) {
            string = "Invention of the Internet";
        } else if (string.equals("Early Telecommunication Methods")) {
            string = "early telecommunication methods";
        } else if (string.equals("Famous Members of the Royal Navy")) {
            string = "famous members of the Royal Navy";
        } else if (string.equals("Nobel Prize Winning inventions")) {
            string = "Nobel Prize winning inventions";
        } else if (string.equals("Edward Teller &amp; Marie Curie")) {
            string = "Edward Teller and Marie Curie";
        } else if (string
                .equals("Computing Language for the programming of artificial intelligence")) {
            string = "Computing Language for the programming of Artificial Intelligence";
        } else if (string.equals("William Hearst Movie")) {
            string = "William Hearst movie";
        } else if (string.equals("Nazis confiscate / destroy art and literature")) {
            string = "Nazis confiscate or destroy art and literature";
        } else if (string.equals("Modern Physiology")) {
            string = "modern Physiology";
        } else if (string.equals("Aviation pioneers publications")) {
            string = "Aviation pioneers' publications";
        } else if (string.equals("Skinner's experiments with the Operant conditioning chamber")) {
            string = "Skinner's experiments with the operant conditioning chamber";
        } else if (string.equals("First woman who won a nobel prize")) {
            string = "First woman who won a Nobel Prize";
        }
        return string;
    }

    private static String fixDocument(final String string) {
        final StringBuilder builder = new StringBuilder();
        Matcher m = SPLIT_PATTERN.matcher(string);
        int end = 0;
        while (m.find()) {
            builder.append(string.substring(end, m.start()));
            end = m.end();
            final char c = string.charAt(m.start());
            if (c != '.' && c != ':') {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Splitted '"
                            + string.substring(Math.max(0, m.start() - 20), m.start() + 1)
                            + " | "
                            + string.substring(m.start() + 2,
                                    Math.min(string.length(), m.end() + 20)));
                }
                builder.append(string.charAt(m.start()));
                builder.append(". ");
                builder.append(m.group().substring(1));
                // builder.setCharAt(m.start() + 1, '.');
            } else {
                builder.append(m.group());
            }
        }
        builder.append(string.substring(end));
        m = REMOVE_PATTERN.matcher(builder);
        while (m.find()) {
            for (int i = m.start(); i < m.end(); ++i) {
                builder.setCharAt(i, ' ');
            }
        }
        return builder.toString();
    }

    private static int getInt(final QuadModel model, final Resource subject, final URI property) {
        for (final Value value : model.filter(subject, property, null).objects()) {
            try {
                return ((Literal) value).intValue();
            } catch (final Throwable ex) {
                LOGGER.error("Not an integer: " + value);
            }
        }
        throw new IllegalArgumentException("Missing " + property + " for " + subject);
    }

    private Yovisto() {
    }

}
