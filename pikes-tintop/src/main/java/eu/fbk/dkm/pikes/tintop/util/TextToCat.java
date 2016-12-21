package eu.fbk.dkm.pikes.tintop.util;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;
import eu.fbk.utils.core.IO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.List;
import java.util.Properties;

/**
 * Created by alessio on 24/09/16.
 */

public class TextToCat {

    private static final Logger LOGGER = LoggerFactory.getLogger(TextToCat.class);

    public static void main(String[] args) throws IOException {
        String inputFolder = args[0];
        String outputFolder = args[1];

        File inputFile = new File(inputFolder);
        File outputFile = new File(outputFolder);

        if (!inputFile.exists()) {
            LOGGER.error("Folder {} does not exist", inputFolder);
            System.exit(1);
        }
        if (!inputFile.isDirectory()) {
            LOGGER.error("Folder {} is not a valid folder", inputFolder);
            System.exit(1);
        }
        if (!outputFile.exists()) {
            if (!outputFile.mkdirs()) {
                LOGGER.error("Unable to create folder {}", outputFolder);
                System.exit(1);
            }
        } else {
            if (outputFile.isFile()) {
                LOGGER.error("Folder {} is a file", outputFolder);
                System.exit(1);
            }
        }

        Properties properties = new Properties();
        properties.setProperty("annotators", "tokenize, ssplit");
        properties.setProperty("ssplit.newlineIsSentenceBreak", "always");

        StanfordCoreNLP pipeline = new StanfordCoreNLP(properties);

        for (File file : inputFile.listFiles()) {
            InputStream stream = IO.read(file.getAbsolutePath());
            Reader reader = new InputStreamReader(stream);
            StringBuilder inputText = new StringBuilder();
            int i;
            while ((i = reader.read()) != -1) {
                inputText.append((char) i);
            }
            reader.close();
            String text = inputText.toString();

            Annotation document = new Annotation(text);
            pipeline.annotate(document);

            File output = new File(outputFile.getAbsolutePath() + File.separator + file.getName());
            OutputStream write = IO.write(output.getAbsolutePath());
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(write));

            List<CoreMap> sentences = document.get(CoreAnnotations.SentencesAnnotation.class);
            for (CoreMap sentence : sentences) {
                List<CoreLabel> tokens = sentence.get(CoreAnnotations.TokensAnnotation.class);
                for (CoreLabel token : tokens) {
                    writer.append(token.originalText()).append("\n");
                }
                writer.append("<eos>\n");
            }

            writer.close();
            write.close();

//            System.out.println(file.getName());
//            System.out.println(text);
//            System.out.println();
        }

    }
}
