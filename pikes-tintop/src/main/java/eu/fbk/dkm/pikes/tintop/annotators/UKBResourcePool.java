package eu.fbk.dkm.pikes.tintop.annotators;

import eu.fbk.dkm.pikes.tintop.server.ResourcePool;
import eu.fbk.rdfpro.util.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Created by alessio on 16/12/15.
 */

public class UKBResourcePool extends ResourcePool<Process> {

    private static final Logger LOGGER = LoggerFactory.getLogger(UKBResourcePool.class);
    private String model, dict, baseDir;

    public UKBResourcePool(String model, String dict, String baseDir, Integer numResources) {
        super(numResources);
        this.model = model;
        this.dict = dict;
        this.baseDir = baseDir;
    }

    @Override public Process createResource() {
        String[] command = { "./ukb_wsd", "--ppr", "-K", model, "-D", dict, "--allranks", "-" };
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(new File(baseDir));
//        pb.redirectError(ProcessBuilder.Redirect.INHERIT);

        try {
            Process process = pb.start();
            BufferedReader brCleanUp = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = brCleanUp.readLine();

            final BufferedReader err = new BufferedReader(new InputStreamReader(
                    process.getErrorStream()));

            Environment.getPool().submit(new Runnable() {

                @Override
                public void run() {
                    String line;
                    try {
                        while ((line = err.readLine()) != null) {
                            LOGGER.debug("[UKB] " + line);
                        }
                    } catch (final IOException ex) {
                        LOGGER.error("[UKB] " + ex.getMessage(), ex);
                    }
                }

            });
            return process;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
