package eu.fbk.dkm.pikes.tintop.annotators.raw;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

/**
 * Created with IntelliJ IDEA.
 * User: alessio
 * Date: 21/07/14
 * Time: 17:15
 * To change this template use File | Settings | File Templates.
 */

public class Semafor {

    public static HashMap<String, String> conversionMap = new HashMap<>();
    static {
        conversionMap.put("TMP", "ADV");
        conversionMap.put("CONJ", "CC");
        conversionMap.put("NAME", "NMOD");
    }

    public static class SemaforSpan {
        int start, end;
        String text;

        public int getStart() {
            return start;
        }

        public int getEnd() {
            return end;
        }

        public String getText() {
            return text;
        }

        @Override public String toString() {
            return "SemaforSpan{" +
                    "start=" + start +
                    ", end=" + end +
                    ", text='" + text + '\'' +
                    '}';
        }
    }

    public static class SemaforSet {
        int rank;
        double score;
        List<SemaforAnnotation> frameElements;

        public int getRank() {
            return rank;
        }

        public double getScore() {
            return score;
        }

        public List<SemaforAnnotation> getFrameElements() {
            return frameElements;
        }

        @Override public String toString() {
            return "SemaforSet{" +
                    "rank=" + rank +
                    ", score=" + score +
                    ", frameElements=" + frameElements +
                    '}';
        }
    }

    public static class SemaforAnnotation {
        String name;
        List<SemaforSpan> spans;

        public String getName() {
            return name;
        }

        public List<SemaforSpan> getSpans() {
            return spans;
        }

        @Override public String toString() {
            return "SemaforAnnotation{" +
                    "name='" + name + '\'' +
                    ", spans=" + spans +
                    '}';
        }
    }

    public static class SemaforFrame {
        SemaforAnnotation target;
        List<SemaforSet> annotationSets;

        public SemaforAnnotation getTarget() {
            return target;
        }

        public List<SemaforSet> getAnnotationSets() {
            return annotationSets;
        }

        @Override public String toString() {
            return "SemaforFrame{" +
                    "target=" + target +
                    ", annotationSets=" + annotationSets +
                    '}';
        }
    }

    @JsonIgnoreProperties({ "tokens" })
    public static class SemaforResponse {
        List<SemaforFrame> frames;

        public List<SemaforFrame> getFrames() {
            return frames;
        }

        @Override public String toString() {
            return "SemaforResponse{" +
                    "frames=" + frames +
                    '}';
        }
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(Semafor.class);
    private int port;
    private String server;

    public Semafor(String server, int port) {
        this.port = port;
        this.server = server;
    }

    public SemaforResponse tag(String text) throws Exception {

        String modifiedSentence;
        ObjectMapper mapper = new ObjectMapper();

        Socket clientSocket = new Socket(server, port);
        DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
        BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        outToServer.writeBytes(text + '\n');
        modifiedSentence = inFromServer.readLine();

        SemaforResponse response = mapper.readValue(modifiedSentence, SemaforResponse.class);
        clientSocket.close();

        return response;

    }

    public static void main(String[] args) {

        Semafor semafor = new Semafor("dkm-server-1.fbk.eu", 11200);
        try {
            String text = new String(Files.readAllBytes((new File("/Users/alessio/Desktop/semafor.conll")).toPath()));
            SemaforResponse tag = semafor.tag(text);

            System.out.println(tag);

        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }
    }
}
