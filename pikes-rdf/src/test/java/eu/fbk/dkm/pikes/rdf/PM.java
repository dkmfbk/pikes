package eu.fbk.dkm.pikes.rdf;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import eu.fbk.rdfpro.util.IO;

public class PM {

    public static void main(final String[] args) throws IOException {

        int cnt = 0;

        boolean first = true;
        final Set<List<String>> set = Sets.newLinkedHashSet();
        for (final String line : Files.readAllLines(Paths.get(args[0]))) {
            if (first) {
                first = false;
                continue;
            }

            final String[] cells = line.split("\t");

            final String vn = select(cells[2], cells[0]);
            // if (vn != null) {
            // vn = vn + "__" + select(cells[4]).substring(3);
            // }
            final String fn = select(cells[8]);
            // if (fn != null) {
            // final String lemma = select(cells[9]);
            // fn = lemma == null ? fn : fn + "__" + select(cells[9]).substring(3);
            // }
            final String pb = select(cells[11]);
            final String wn = select(cells[6]);

            String vnl = select(cells[4]);
            String fnl = select(cells[9]);
            vnl = vnl == null ? null : vnl.substring(3).replace('-', '_');
            fnl = fnl == null ? null : fnl.substring(3).replaceAll("\\.[a-z]", "");
            String wnl = null;
            if (wn != null) {
                final int index = wn.indexOf('%');
                wnl = wn.substring(3, index).replace("?", "").replace('-', '_');
            }
            String pbl = null;
            if (pb != null) {
                final int index = pb.indexOf('.');
                pbl = pb.substring(3, index);
            }

            if (fnl != null && wnl != null && !fnl.equalsIgnoreCase(wnl)) {
                System.out.println(fnl + " - " + wnl);
            }

            final Set<String> lemmas = Sets.newHashSet(vnl, fnl, wnl, pbl);
            lemmas.remove(null);
            // if (lemmas.size() > 1) {
            // // System.out.println(vnl + " - " + fnl + " - " + pbl + " - " + wnl);
            // ++cnt;
            // continue;
            // }

            set.add(Lists.newArrayList(vn, fn, pb, wn));
        }

        try (Writer out = IO.utf8Writer(IO.buffer(IO.write(args[1])))) {
            int i = 0;
            final String[] names = new String[] { "vn", "fn", "pb", "wn" };
            for (final List<String> alignment : set) {
                final String uri = "<pm:a" + (++i) + ">";
                for (int j = 0; j < 4; ++j) {
                    if (alignment.get(j) != null) {
                        out.write(uri + " <pm:" + names[j] + "> <" + alignment.get(j) + "> .\n");
                    }
                }
            }
        }

        System.out.println(cnt + " discarded");
    }

    private static String select(final String... values) {
        for (final String value : values) {
            if (!value.endsWith(":NULL")) {
                return value;
            }
        }
        return null;
    }

}
