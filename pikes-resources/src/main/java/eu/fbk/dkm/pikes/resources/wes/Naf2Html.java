package eu.fbk.dkm.pikes.resources.wes;

import ixa.kaflib.KAFDocument;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;

/**
 * Created by alessio on 06/12/15.
 */

public class Naf2Html {

    private static final Logger LOGGER = LoggerFactory.getLogger(Naf2Html.class);

    public static void main(String[] args) {

        String nafFolder = "/Users/alessio/Documents/Resources/wes/wes2015.naf";
        String htmlFolder = "/Users/alessio/Documents/Resources/wes/html";
        String[] extensions = new String[] { "naf" };

        File nafFolderFile = new File(nafFolder);
        File htmlFolderFile = new File(htmlFolder);

        try {
            Iterator<File> fileIterator = FileUtils.iterateFiles(nafFolderFile, extensions, true);

            if (!htmlFolderFile.exists()) {
                htmlFolderFile.mkdirs();
            }

            File indexFile = new File(htmlFolder + File.separator + "index.html");
            BufferedWriter writer = new BufferedWriter(new FileWriter(indexFile));

            writer.append("<html>\n");
            writer.append("<body>\n");
            writer.append("<h1>List of files</h1>\n");
            writer.append("<ul>\n");

            fileIterator.forEachRemaining((File f) -> {
                File outputHtml = new File(htmlFolder + File.separator + f.getName() + ".html");

                try {
                    KAFDocument document = KAFDocument.createFromFile(f);
                    String title = document.getFileDesc().title;
                    String text = document.getRawText();

                    BufferedWriter insideWriter = new BufferedWriter(new FileWriter(outputHtml));
                    insideWriter.append("<html>\n");
                    insideWriter.append("<head>\n");
                    insideWriter.append(String.format("<title>%s</title>", title));
                    insideWriter.append("</head>\n");
                    insideWriter.append("<body>\n");
                    insideWriter.append(String.format("<h1>%s</h1>", title));
                    insideWriter.append("<p>\n");
                    insideWriter.append(text.replaceAll("\n", "<br />\n"));
                    insideWriter.append("</p>\n");
                    insideWriter.append("<p>\n");
                    insideWriter.append(String.format("<p><a href='index.html'>Back to index</a></p>"));
                    insideWriter.append("</p>\n");
                    insideWriter.append("</body>\n");
                    insideWriter.append("</html>\n");
                    insideWriter.close();

                    writer.append(String.format("<li><a href='%s'>%s - %s</a></li>\n", outputHtml.getName(),
                            outputHtml.getName(), title));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            writer.append("</ul>\n");
            writer.append("</body>\n");
            writer.append("</html>\n");
            writer.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
