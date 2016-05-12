package eu.fbk.dkm.pikes.tintopclient;

import org.apache.log4j.Logger;

import java.io.IOException;
import java.net.URL;

/**
 * Created by alessio on 25/02/15.
 */

public class TintopServer {

    static Logger logger = Logger.getLogger(TintopServer.class.getName());

    private String protocol;
    private String host;
    private String path;
    private int port;

    private int id;

    public TintopServer(URL url) {
        this(url, (int) (Math.random() * 32000 + 1));
    }

    public TintopServer(URL url, int id) {
        this.protocol = url.getProtocol();
        this.host = url.getHost();
        this.port = url.getPort();
        this.path = url.getPath();
        this.id = id;
    }

    public TintopServer(String line) throws IOException {
        String[] parts = line.split("\\s+");
        if (parts.length != 4) {
            throw new IOException("Invalid line");
        }
        this.protocol = parts[0].trim();
        this.host = parts[1].trim();
        this.port = Integer.parseInt(parts[2].trim());
        this.path = parts[3].trim();
    }

    public TintopServer(String protocol, String host, String path, int port) {
        this.protocol = protocol;
        this.host = host;
        this.path = path;
        this.port = port;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getShortName() {
        return String.format("%s://%s:%d%s", protocol, host, port, path);

    }

    @Override
    public String toString() {
        return "TintopServer{" +
                "protocol='" + protocol + '\'' +
                ", host='" + host + '\'' +
                ", path='" + path + '\'' +
                ", port=" + port +
                '}';
    }
}
