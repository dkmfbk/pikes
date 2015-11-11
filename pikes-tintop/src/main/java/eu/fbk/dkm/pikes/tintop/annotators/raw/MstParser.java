package eu.fbk.dkm.pikes.tintop.annotators.raw;

import eu.fbk.dkm.pikes.tintop.annotators.DepParseInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: alessio
 * Date: 21/07/14
 * Time: 17:15
 * To change this template use File | Settings | File Templates.
 */

public class MstParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(MstParser.class);
    private int port;
    private String server;

    public MstParser(String server, int port) {
        this.port = port;
        this.server = server;
    }

    public DepParseInfo tag(List<String> tokens, List<String> poss) throws Exception {
        if (tokens.size() != poss.size()) {
            LOGGER.error("The token and pos collections must have the same size");
            return null;
        }

        StringBuffer sb = new StringBuffer();

        for (int i = 0; i < tokens.size(); i++) {
            String token = tokens.get(i);
            String pos = poss.get(i);

            sb.append(token.trim().replaceAll("\\s+", ""));
            sb.append("_");
            sb.append(pos.trim().replaceAll("\\s+", ""));
            sb.append(" ");
        }

        return tag(sb.toString().trim());
    }

    public DepParseInfo tag(String text) throws Exception {

        String modifiedSentence;

        Socket clientSocket = new Socket(server, port);
        DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
        BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        outToServer.writeBytes(text + '\n' + '*' + '\n');
        modifiedSentence = inFromServer.readLine();

        String[] parts = modifiedSentence.split("\\s+");
        HashMap<Integer, Integer> depParents = new HashMap<>();
        HashMap<Integer, String> depLabels = new HashMap<>();

//        ArrayList<HashMap<String, String>> res = new ArrayList<>();
        int i = 0;
        int id = 0;
        while (i < parts.length) {

            int unit = i % 10;
            int dec = (i - unit) / 10;

            if (unit == 0) {
                id = Integer.parseInt(parts[i]);
            }

            if (unit == 6) {
                depParents.put(id, Integer.parseInt(parts[i]));
            }
            if (unit == 7) {
                depLabels.put(id, parts[i]);
            }
            i++;
        }

        clientSocket.close();

        return new DepParseInfo(depParents, depLabels);

//        System.out.println(Arrays.toString(parts));
//        System.out.println(depLabels);
//        System.out.println(depParents);

//        SemaforResponse response = mapper.readValue(modifiedSentence, SemaforResponse.class);

//        return null;

    }

    public static void main(String[] args) {

        MstParser mstParser = new MstParser("dkm-server-1.fbk.eu", 19201);
        try {
            String text = "My_PRP$ kitchen_NN no_RB longer_RB smells_VBZ ._.";
            DepParseInfo tag = mstParser.tag(text);
            System.out.println(tag);
//            SemaforResponse tag = semafor.tag(text);
//            System.out.println(tag);

        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.error(e.getMessage());
        }
    }
}
