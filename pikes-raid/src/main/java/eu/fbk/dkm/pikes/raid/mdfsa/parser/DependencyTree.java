package eu.fbk.dkm.pikes.raid.mdfsa.parser;

import java.io.Serializable;
import java.util.ArrayList;

public class DependencyTree implements Serializable {
  
  private static final long serialVersionUID = 1L;

  private ArrayList<String> dependecies;
  
  public DependencyTree() {
    this.dependecies = new ArrayList<String>();
  }
  
  public void addDependency(String dep) {
    this.dependecies.add(dep);
  }
  
  public ArrayList<String> getDependecies() {
    return this.dependecies;
  }
  
}
