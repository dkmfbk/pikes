import ch.qos.logback.classic.Level;
import com.google.common.base.Charsets;
import eu.fbk.dkm.pikes.rdf.vocab.KS;
import eu.fbk.dkm.utils.CommandLine;
import eu.fbk.rdfpro.RDFHandlers;
import eu.fbk.rdfpro.RDFProcessors;
import eu.fbk.rdfpro.RDFSource;
import eu.fbk.rdfpro.RDFSources;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.model.vocabulary.XMLSchema;
import org.openrdf.rio.RDFHandler;
import org.openrdf.rio.RDFHandlerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by alessio on 27/11/15.
 */

public class ConvertTsvToRdf {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConvertTsvToRdf.class);
    private static String PIKES_PREFIX = "http://pikes.fbk.eu/mappings/";
    private static String KS_PREFIX = "http://dkm.fbk.eu/ontologies/knowledgestore#";
    private static String PREMON_PREFIX = "http://premon.fbk.eu/resource/";

    public static void main(String[] args) {
        try {
            final CommandLine cmd = CommandLine
                    .parser()
                    .withName("./converter")
                    .withHeader("Convert Pikes TSVs in RDF")
                    .withOption("p", "predicates", "TSV file with predicate mappings", "FILE",
                            CommandLine.Type.FILE_EXISTING, true, false, false)
                    .withOption("r", "roles", "TSV file with role mappings", "FILE",
                            CommandLine.Type.FILE_EXISTING, true, false, false)
                    .withOption("O", "output", "Output file prefix", "PREFIX",
                            CommandLine.Type.STRING, true, false, true)
                    .withLogger(LoggerFactory.getLogger("eu.fbk")).parse(args);

            ((ch.qos.logback.classic.Logger) LOGGER).setLevel(Level.INFO);

            ValueFactoryImpl valueFactory = ValueFactoryImpl.getInstance();
            Statement statement;

            Map<String, String> prefixes = new HashMap<>();
            prefixes.put("pikes", PIKES_PREFIX);
            prefixes.put("ks", KS_PREFIX);
            prefixes.put("pm", PREMON_PREFIX);
            prefixes.put("xs", XMLSchema.NAMESPACE);

            File predicatesFile = cmd.getOptionValue("predicates", File.class);
            File rolesFile = cmd.getOptionValue("roles", File.class);

            String outputPattern = cmd.getOptionValue("output", String.class);

            URI ksPredicate = valueFactory.createURI(KS_PREFIX + "predicate");
            URI ksLemma = valueFactory.createURI(KS_PREFIX + "lemma");
            URI ksPos = valueFactory.createURI(KS_PREFIX + "pos");
            URI ksClass = valueFactory.createURI(KS_PREFIX + "class");
            URI ksRole = valueFactory.createURI(KS_PREFIX + "role");
            URI ksProperty = valueFactory.createURI(KS_PREFIX + "property");

            if (predicatesFile != null) {
                List<String> lines = Files.readAllLines(predicatesFile.toPath(), Charsets.UTF_8);
                List<Statement> statements = new ArrayList<>();
                File outputFile = new File(outputPattern + "-predicates.ttl.gz");

                for (String line : lines) {
                    line = line.trim();
                    if (line.length() == 0) {
                        continue;
                    }

                    String[] parts = line.split("\\s+");
                    if (parts.length < 4) {
                        LOGGER.error("{} is not a valid line", line);
                        continue;
                    }

                    StringBuilder mappingBuffer = new StringBuilder();
                    mappingBuffer.append(PIKES_PREFIX);
                    mappingBuffer.append(parts[0].replace(':', '-')).append("-");
                    mappingBuffer.append(parts[1]).append("-");
                    mappingBuffer.append(parts[2]);

                    // Predicate
                    String[] pParts = parts[0].split(":");
                    if (pParts.length < 2) {
                        LOGGER.error("{} is not a valid predicate", parts[0]);
                        continue;
                    }

                    StringBuilder predicateBuffer = new StringBuilder();
                    predicateBuffer.append(PREMON_PREFIX);
                    if (pParts[0].equals("fn")) {
                        predicateBuffer.append("fn15-");
                    } else if (pParts[0].equals("pb")) {
                        predicateBuffer.append("pbon5-");
                    } else if (pParts[0].equals("nb")) {
                        predicateBuffer.append("nb10-");
                    } else {
                        LOGGER.error("{} is not a valid prefix", pParts[0]);
                        continue;
                    }
                    predicateBuffer.append(parts[0].substring(3));

                    URI mappingURI = valueFactory.createURI(mappingBuffer.toString());
                    URI predicateURI = valueFactory.createURI(predicateBuffer.toString());

                    statement = valueFactory.createStatement(mappingURI, ksPredicate, predicateURI);
                    statements.add(statement);
                    statement = valueFactory.createStatement(mappingURI, ksLemma, valueFactory.createLiteral(parts[1]));
                    statements.add(statement);
                    statement = valueFactory.createStatement(mappingURI, ksPos, valueFactory.createLiteral(parts[2]));
                    statements.add(statement);
                    statement = valueFactory.createStatement(mappingURI, ksClass, valueFactory.createURI(parts[3]));
                    statements.add(statement);
                }

                RDFSource source = RDFSources.wrap(statements, prefixes);

                try {
                    RDFHandler rdfHandler = RDFHandlers.write(null, 1000, outputFile.getAbsolutePath());
                    source.emit(rdfHandler, 1);
                } catch (Exception e) {
                    LOGGER.error("Input/output error, the file {} has not been saved ({})",
                            outputFile.getAbsolutePath(), e.getMessage());
                    throw new RDFHandlerException(e);
                }

            }

            if (rolesFile!= null) {
                List<String> lines = Files.readAllLines(rolesFile.toPath(), Charsets.UTF_8);
                List<Statement> statements = new ArrayList<>();
                File outputFile = new File(outputPattern + "-roles.ttl.gz");

                for (String line : lines) {
                    line = line.trim();
                    if (line.length() == 0) {
                        continue;
                    }

                    String[] parts = line.split("\\s+");
                    if (parts.length < 2) {
                        LOGGER.error("{} is not a valid line", line);
                        continue;
                    }

                    StringBuilder mappingBuffer = new StringBuilder();
                    mappingBuffer.append(PIKES_PREFIX);
                    mappingBuffer.append(parts[0].replace(':', '-').replace('@', '-'));

                    // Predicate
                    String[] pParts = parts[0].split(":");
                    if (pParts.length < 2) {
                        LOGGER.error("{} is not a valid role", parts[0]);
                        continue;
                    }

                    StringBuilder roleBuffer = new StringBuilder();
                    roleBuffer.append(PREMON_PREFIX);
                    if (pParts[0].equals("fn")) {
                        roleBuffer.append("fn15-");
                    } else if (pParts[0].equals("pb")) {
                        roleBuffer.append("pbon5-");
                    } else if (pParts[0].equals("nb")) {
                        roleBuffer.append("nb10-");
                    } else {
                        LOGGER.error("{} is not a valid prefix", pParts[0]);
                        continue;
                    }
                    roleBuffer.append(parts[0].substring(3).replace('@', '-'));

                    URI mappingURI = valueFactory.createURI(mappingBuffer.toString());
                    URI roleURI = valueFactory.createURI(roleBuffer.toString());

                    statement = valueFactory.createStatement(mappingURI, ksRole, roleURI);
                    statements.add(statement);
                    statement = valueFactory.createStatement(mappingURI, ksProperty, valueFactory.createURI(parts[1]));
                    statements.add(statement);
                }

                RDFSource source = RDFSources.wrap(statements, prefixes);

                try {
                    RDFHandler rdfHandler = RDFHandlers.write(null, 1000, outputFile.getAbsolutePath());
                    source.emit(rdfHandler, 1);
                } catch (Exception e) {
                    LOGGER.error("Input/output error, the file {} has not been saved ({})",
                            outputFile.getAbsolutePath(), e.getMessage());
                    throw new RDFHandlerException(e);
                }
            }
        } catch (Exception e) {
            CommandLine.fail(e);
        }
    }
}
