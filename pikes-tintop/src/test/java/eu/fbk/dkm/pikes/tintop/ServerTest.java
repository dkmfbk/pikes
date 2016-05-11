package eu.fbk.dkm.pikes.tintop;

import org.glassfish.grizzly.http.server.CLStaticHttpHandler;
import org.glassfish.grizzly.http.server.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Created by alessio on 11/05/16.
 */

public class ServerTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServerTest.class);

    public static void main(String[] args) {

        HttpServer httpServer = HttpServer.createSimpleServer("localhost", 8080);

        httpServer.getServerConfiguration().addHttpHandler(
                new CLStaticHttpHandler(HttpServer.class.getClassLoader(), "webdemo/"), "/demo/");

        // Fix
        // see: http://stackoverflow.com/questions/35123194/jersey-2-render-swagger-static-content-correctly-without-trailing-slash
        httpServer.getServerConfiguration().addHttpHandler(
                new CLStaticHttpHandler(HttpServer.class.getClassLoader(), "webdemo/static/"), "/static/");

        try {
            httpServer.start();
            Thread.currentThread().join();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }
}
