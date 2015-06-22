/*
 *    Copyright (c) Sematext International
 *    All Rights Reserved
 *
 *    THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF Sematext International
 *    The copyright notice above does not evidence any
 *    actual or intended publication of such source code.
 */

package eu.fbk.dkm.pikes.raid.mdfsa;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.util.ArrayList;


public class FileManager {

  private String fileName;
  private File currentFile;
  private String absolutePath;
  private PrintWriter outputStream;
  private BufferedReader inputStream;
  private ArrayList rowsList;

  /**
   * 0: input 1: output
   */
  public enum Mode {
    READ, WRITE, APPEND
  };

  private Mode mode;

  /**
   * Initializes the object
   * 
   * @param FileAbsolutePath
   */
  @SuppressWarnings("static-access")
  public FileManager(String fileAbsolutePath, Mode m) {

    this.absolutePath = fileAbsolutePath;
    this.currentFile = new File(fileAbsolutePath);
    this.fileName = this.currentFile.getName();
    this.mode = m;

    try {
      if (this.mode == Mode.READ) {
        InputStream iS = ClassLoader.getSystemClassLoader().getSystemResourceAsStream(fileAbsolutePath);
        if(iS == null) {
          this.inputStream = new BufferedReader(new FileReader(fileAbsolutePath));
        } else {
          Reader reader = new InputStreamReader(iS);
          this.inputStream  = new BufferedReader(reader);
        }
      } else if (this.mode == Mode.WRITE) {
        this.outputStream = new PrintWriter(new BufferedWriter(new FileWriter(fileAbsolutePath)));
      } else if (this.mode == Mode.APPEND) {
        this.outputStream = new PrintWriter(new BufferedWriter(new FileWriter(fileAbsolutePath, true)));
      }

    } catch (IOException e) {
      e.printStackTrace();
      return;
    }
  }

  /**
   * Writes a string in the file.
   * 
   * @param DataStream
   */
  public void write(String dataStream) {
    if (this.mode == Mode.WRITE || this.mode == Mode.APPEND) {
      this.outputStream.println(dataStream);
    }
  }

  /**
   * Reads a line from the file
   * 
   * @return
   * @throws IOException
   */
  public String read() {
    try {
      if (this.mode == Mode.READ) {
        String dataStream;
        dataStream = this.inputStream.readLine();
        return dataStream;
      }
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
    return null;
  }

  /**
   * Closes the file
   */
  public void close() {
    try {
      if (this.mode == Mode.READ) {
        this.inputStream.close(); 
      } else if (this.mode == Mode.WRITE || this.mode == Mode.APPEND) {
        this.outputStream.flush();
        this.outputStream.close();
      }
    } catch (IOException e) {
      e.printStackTrace();
    }

  }

  
  /**
   * Methods for managing different kinds of file
   */
  public ArrayList<String> importSimpleTextContent() {
    this.rowsList = new ArrayList<String>();
    String rowString = this.read();
    while (rowString != null) {
      this.rowsList.add(rowString);
      rowString = this.read();
    }
    return this.rowsList;
  }

  public String importFullTextContent() {
    String content = new String();
    String dataStream = this.read();
    while (dataStream != null) {
      content = content.concat(dataStream + "\n");
      dataStream = this.read();
    }
    return content;
  }

  
  public String importFullTextContent(String rowDelimiter) {
    String content = new String();
    String dataStream = this.read();
    while (dataStream != null) {
      content = content.concat(dataStream + rowDelimiter);
      dataStream = this.read();
    }
    return content;
  }
  
  
  
  
  public void importTabSeparatedContent() {

  }

  
  public ArrayList<String[]> importStringSeparatedContent(String splitter) {
    
    ArrayList<String[]> splittedData = new ArrayList<String[]>();
    String dataStream = this.read();
    while (dataStream != null) {
      String[] content = dataStream.split(splitter);
      splittedData.add(content);
      dataStream = this.read();
    }
    
    return splittedData;
  }

  public Document importXMLContent() {
    Document doc = null;
    try {
      DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
      DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
      doc = dBuilder.parse(this.currentFile);
    } catch (Exception e) {
      e.printStackTrace();
    }
    return doc;
  }

  
  public Model importRDFContent() {
    // Create an empty model
    Model model = ModelFactory.createDefaultModel();

    // Use the FileManager to find the input file
    InputStream in = com.hp.hpl.jena.util.FileManager.get().open(this.absolutePath);
    if (in == null) {
      throw new IllegalArgumentException("File: " + this.absolutePath + " not found");
    }

    // Reads the RDF/XML file
    model.read(in, null);    
    return model;
  }
  
  
  public String getFileName() {
    return this.fileName;
  }
}
