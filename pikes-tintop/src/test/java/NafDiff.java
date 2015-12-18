import ixa.kaflib.KAFDocument;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Iterator;

/**
 * Created by alessio on 16/12/15.
 */

public class NafDiff {

    private static final Logger LOGGER = LoggerFactory.getLogger(NafDiff.class);

    public static void main(String[] args) {
        String folder1 = "/Users/alessio/Desktop/elastic/yovisto/naf";
        String folder2 = "/Users/alessio/Desktop/elastic/yovisto/naf-new";

        try {
            Iterator<File> fileIterator = FileUtils.iterateFiles(new File(folder1), null, true);
            while (fileIterator.hasNext()) {
                File file1 = fileIterator.next();
                File file2 = new File(folder2 + File.separator + file1.getName());

                KAFDocument document1 = KAFDocument.createFromFile(file1);
                KAFDocument document2 = KAFDocument.createFromFile(file2);

                String title1 = document1.getFileDesc().title.toLowerCase();
                String title2 = document2.getFileDesc().title.toLowerCase();

                if (title1.replaceAll("[^0-9a-zA-Z]", "").equals(title2.replaceAll("[^0-9a-zA-Z]", ""))) {
                    continue;
                }

                System.out.println(document1.getPublic().publicId);
                System.out.println(document1.getFileDesc().title);
                System.out.println(document2.getFileDesc().title);
                System.out.println();
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }
    }
}
