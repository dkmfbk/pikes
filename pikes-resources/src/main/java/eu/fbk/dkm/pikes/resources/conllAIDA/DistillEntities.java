package eu.fbk.dkm.pikes.resources.conllAIDA;

import com.google.common.io.Files;
import eu.fbk.rdfpro.util.IO;
import eu.fbk.utils.core.CommandLine;
import ixa.kaflib.Entity;
import ixa.kaflib.ExternalRef;
import ixa.kaflib.KAFDocument;
import ixa.kaflib.Term;
import org.apache.commons.lang.StringUtils;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by marcorospocher on 19/07/16.
 */
public class DistillEntities {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(DistillEntities.class);
    private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");


    public enum removeLayer {
        deps, chunks, entities, properties, categories, coreferences, opinions, relations, srl, constituency, timeExpressions, linkedEntities, constituencyStrings;
    }

    public static void main(String[] args) {
        try {
            final CommandLine cmd = CommandLine
                    .parser()
                    .withName("stripNAF")
                    .withHeader("Strip NAF files of unnecessary layers")
                    .withOption("i", "input-folder", "the folder of the input NAF corpus", "DIR", CommandLine.Type.DIRECTORY_EXISTING, true, false, true)
                    .withOption("o", "output-folder", "the folder of the input NAF corpus", "DIR", CommandLine.Type.DIRECTORY, true, false, true)
                    .withOption("m", "mode", "modality (1=entity-centric, 2=term-centric)", "INT", CommandLine.Type.NON_NEGATIVE_INTEGER, true, false, false)
                    .withLogger(LoggerFactory.getLogger("eu.fbk")).parse(args);

            File inputFolder = cmd.getOptionValue("input-folder", File.class);
            File outputFolder = cmd.getOptionValue("output-folder", File.class);
            Integer modality = cmd.getOptionValue("mode", Integer.class,1);
//
            for (final File file : Files.fileTreeTraverser().preOrderTraversal(inputFolder)) {
                if (!file.isFile()) {
                    continue;
                }
                if (file.getName().startsWith(".")) {
                    continue;
                }

                if (!file.getName().endsWith(".naf.gz")) {
                    continue;
                }

                System.out.print("Processing: "+file.getAbsoluteFile().toString());

                String inputfilenamelocal = file.getName();
                String outputfilenamelocal = StringUtils.leftPad(inputfilenamelocal.replace(".naf",".tsv").replace("conll-",""), 11, '0');
                //System.out.println(outputfilenamelocal);

                String outputfilename = file.getAbsoluteFile().toString().replace(inputFolder.getAbsolutePath(),outputFolder.getAbsolutePath()).replace(inputfilenamelocal,outputfilenamelocal);

                File outputFile = new File(outputfilename);

                if (!outputFile.exists()) {

                    try (Reader reader = IO.utf8Reader(IO.buffer(IO.read(file.getAbsoluteFile().toString())))) {
                        try {

                            //System.out.print(" WORKING");

                            KAFDocument document = KAFDocument.createFromStream(reader);
                            reader.close();

                            HashMap<Integer,String> toPrint = new HashMap();
                            String ID = document.getPublic().publicId;

                            if (modality==1) {

                                //System.out.println("Processing: "+file.getAbsoluteFile().toString());
                                final List<Entity> entities = document.getEntities();

                                for (Entity entity : entities
                                        ) {
                                    String type = entity.getType();
                                    List<ExternalRef> externalRefs = entity.getExternalRefs();
                                    HashMap<String, Float> refs = new HashMap();
                                    for (ExternalRef exref : externalRefs
                                            ) {
                                        if (exref.getResource().equalsIgnoreCase("dbpedia-candidates")) {
                                            String ref = exref.getReference();
                                            float confidence = exref.getConfidence();
                                            refs.put(ref, confidence);
                                        } else refs.put("null", 0.0f);
                                    }

                                    String span = entity.getStr();
                                    String IDs = "";
                                    Integer spanID = Integer.parseInt(entity.getTerms().get(0).getId().replace("t", "")) - 1;
                                    for (Term t : entity.getTerms()
                                            ) {
                                        IDs += String.valueOf(Integer.parseInt(t.getId().replace("t", "")) - 1) + " ";
                                    }
                                    //System.out.println(span);
                                    //System.out.print(type);
                                    String references = refs.entrySet().stream()
                                            .sorted(Map.Entry.<String, Float>comparingByValue().reversed()).map(e -> e.getKey().replace("http://dbpedia.org/resource/", "") + " [" + e.getValue() + "]").collect(Collectors.joining("  "));
                                    String line = ID.replace("conll-", "") + "\t" + IDs + "\t" + span + "\t" + type + "\t" + references;
                                    //System.out.println(line);
                                    toPrint.put(spanID, line);
                                }
                            } else if (modality==2) {

                                final List<Term> terms = document.getTerms();
                                Collections.sort(terms, new Comparator<Term>() {
                                    @Override
                                    public int compare(Term o1, Term o2) {
                                        return Integer.compare(o1.getOffset(),o2.getOffset());
                                    }
                                });
                                for (Term t : terms
                                        ) {

                                    Integer t_ID = Integer.parseInt(t.getId().replace("t",""))-1;

                                    String line = t.getForm() + "\t" + t_ID;

                                    //System.out.println(t.getStr());
                                    List<Entity> entities = document.getEntitiesByTerm(t);



                                    if (entities.size()>0) {
                                        Entity entity = entities.get(0);
                                        String type = entity.getType();

                                        List<ExternalRef> externalRefs = entity.getExternalRefs();
                                        HashMap<String, Float> refs = new HashMap();
                                        for (ExternalRef exref : externalRefs
                                                ) {
                                            if (exref.getResource().equalsIgnoreCase("dbpedia-candidates")) {
                                                String ref = exref.getReference();
                                                float confidence = exref.getConfidence();
                                                refs.put(ref, confidence);
                                            } else refs.put("null", 0.0f);
                                        }
                                        String span = entity.getStr();
                                        String IDs = "";
                                        Integer spanID = Integer.parseInt(entity.getTerms().get(0).getId().replace("t", "")) - 1;
                                        for (Term tt : entity.getTerms()
                                                ) {
                                            IDs += String.valueOf(Integer.parseInt(t.getId().replace("t", "")) - 1) + " ";
                                        }
                                        //System.out.println(span);
                                        //System.out.print(type);
                                        String references = refs.entrySet().stream()
                                                .sorted(Map.Entry.<String, Float>comparingByValue().reversed()).map(e -> e.getKey().replace("http://dbpedia.org/resource/", "") + " [" + e.getValue() + "]").collect(Collectors.joining("  "));
                                        //String line = ID.replace("conll-", "") + "\t" + IDs + "\t" + span + "\t" + type + "\t" + references;
                                        line += "\t" + type + "\t" + references;
                                        //System.out.println(line);

                                    }
                                    toPrint.put(t_ID, line);
                                }

                            }




                            Files.createParentDirs(outputFile);
                            try (Writer w = IO.utf8Writer(IO.buffer(IO.write(outputFile.getAbsolutePath())))) {
                                toPrint.entrySet().stream()
                                        .sorted(Map.Entry.<Integer, String>comparingByKey()).map(e -> e.getValue()).forEach(e -> {
                                    try {
                                        w.write(e+"\n");
                                    } catch (IOException e1) {
                                        e1.printStackTrace();
                                    }
                                });

                                w.close();
                                //System.out.print(" SAVED");

                            } catch (IOException ee) {
                                ee.printStackTrace();
                            }

                            System.out.println(" DONE!");


                        } catch (Exception e) {

                        }

                    }


                } //else System.out.println(" SKIPPED");

            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
