package eu.fbk.shell.mdfsa.data.structures;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class Graph implements Serializable {

  private static final long serialVersionUID = 1L;
  
  private int nextNodeId;
  private int numberOfEdges;
  private HashMap<String, Long> labels;
  private HashMap<Long, String> ids;
  private HashMap<Long, Integer> markers;
  private HashMap<Long, ArrayList<DomainEdge>> nodes;
  
  /* Maps dedicated to particular knowledge bases where a further level of semantic representation is requested */
  private HashMap<String, ArrayList<Long>> wnWordSynsets;
  
  
  public Graph() {
    this.nextNodeId = 0;
    this.labels = new HashMap<String, Long>();
    this.ids = new HashMap<Long, String>();
    this.nodes = new HashMap<Long, ArrayList<DomainEdge>>();
    this.wnWordSynsets = new HashMap<String, ArrayList<Long>>();
    this.markers = new HashMap<Long, Integer>();
  }
  
  public HashMap<String, Long> getLabels() {
    return this.labels;
  }
  public HashMap<Long, String> getIds() {
    return this.ids;
  }
  public void setLabels(HashMap<String, Long> labels) {
    this.labels = labels;
  }
  public HashMap<Long, ArrayList<DomainEdge>> getNodes() {
    return this.nodes;
  }
  public void setNodes(HashMap<Long, ArrayList<DomainEdge>> nodes) {
    this.nodes = nodes;
  }
  public HashMap<String, ArrayList<Long>> getWnWordSynsets() {
    return wnWordSynsets;
  }
  public void setWnWordSynsets(HashMap<String, ArrayList<Long>> wnWordSynsets) {
    this.wnWordSynsets = wnWordSynsets;
  }

  
  
  public ArrayList<Long> getFeatureIds(String label) {
    ArrayList<Long> feature = new ArrayList<Long>();
    
    /* Gets the id of the SenticNet concept */
    Long currentFeatureId = this.labels.get(label);
    if(currentFeatureId != null) {
      feature.add(currentFeatureId);
    }
    
    /* Gets the synsetIds from the WordNet knowledge */
    ArrayList<Long> synsets = this.wnWordSynsets.get(label);
    if(synsets != null) {
      for(Long currentSynset: synsets) {
        currentFeatureId = this.labels.get(String.valueOf(currentSynset));
        if(currentFeatureId != null) {
          feature.add(currentFeatureId);
        }
      }
      
    }
    
    return feature;
  }
  
  
  
  /**
   * Adds a new edge to the main knowledge graph
   * @param source the label of the source concept
   * @param target the label of the target concept
   * @param wSource the weight of the edge target->source
   * @param wTarget the weight of the edge source->target
   */
  public void addEdge(String source, String target, double wSource, double wTarget, int creator) {
    
    /* Checks if source and target concepts exist */
    Long sourceId = this.labels.get(source);
    Long targetId = this.labels.get(target);
    
    if(sourceId == null) {
      sourceId = new Long(this.nextNodeId);
      this.labels.put(source, sourceId);
      this.ids.put(sourceId, source);
      this.markers.put(sourceId, creator);
      this.nextNodeId++;
    }
    
    if(targetId == null) {
      targetId = new Long(this.nextNodeId);
      this.labels.put(target, targetId);
      this.ids.put(targetId, target);
      this.markers.put(targetId, creator);
      this.nextNodeId++;
    }
    
    /* Updates edges */
    DomainEdge de1 = new DomainEdge(targetId, wTarget, creator);
    this.updateEdge(sourceId, de1);
    this.numberOfEdges++;
    
    /* The opposite edge is created only if the inverse weight is provided */
    if(wSource != Double.MAX_VALUE) {
      DomainEdge de2 = new DomainEdge(sourceId, wSource, creator);
      this.updateEdge(targetId, de2);
      this.numberOfEdges++;
    }
  }
  
  
  /**
   * Updates the edges map with the new data read during the training phase or inferred during the convergence phase
   * @param nodeId the id of the node to be updated
   * @param newEdge the edge containing the new information
   */
  private void updateEdge(long nodeId, DomainEdge newEdge) {
    
    boolean check = false;
    DomainEdge e = null;
    ArrayList<DomainEdge> edges = this.nodes.get(nodeId);
    if(edges != null) {
      for(int i = 0; i < edges.size(); i++) {
        e = edges.get(i);
        if(e.getNodeId() == newEdge.getNodeId()) {
          check = true;
          break;
        }
      }
    }
    
    if(edges == null) {
      edges = new ArrayList<DomainEdge>();
    }
    
    if(check == false) {
      edges.add(newEdge);
    } else {
      e.setWeight(newEdge.getWeight());
    }
    this.nodes.put(nodeId, edges);
  }
  
  
  /**
   * Creates the relations between SenticNet and WordNet.
   * Relations are created when only one WN synset is associated with a SN term.
   * This, for avoiding the creation of ambiguous links.
   */
  public void createSenticNetWordNetRelations() {
    Iterator markers = this.markers.keySet().iterator();
    while(markers.hasNext()) {
      long currentId = (Long) markers.next();
      int currentMarker = (Integer) this.markers.get(currentId);
      
      /* Checks if the Id is marked with the SenticNet source (0) */
      if(currentMarker == 0) {
        String currentLabel = this.ids.get(currentId);
        
        /* Gets WN synsets associatd to the currentLabel and it checks if there is only one synset associated to the
         * currentLabel in order to create the new relation. */
        ArrayList<Long> synsets = this.wnWordSynsets.get(currentLabel);
        //if(synsets != null && synsets.size() == 1) {
        if(synsets != null) {
          for(Long currentSynset: synsets) {
            Long synsetIdGraph = (Long) this.labels.get(String.valueOf(currentSynset));
            if(synsetIdGraph == null) {
              continue;
            }
            
            /* Updates edges */
            DomainEdge de1 = new DomainEdge(synsetIdGraph, 1.0, 2);
            this.updateEdge(currentId, de1);
            this.numberOfEdges++;
            DomainEdge de2 = new DomainEdge(currentId, 1.0, 2);
            this.updateEdge(synsetIdGraph, de2);
            this.numberOfEdges++;
          }
        }
      }
    }
  }
  
  
  
  public int getNumberOfNodes() {
    return nextNodeId;
  }

  public int getNumberOfEdges() {
    return numberOfEdges;
  }
  
  
  
  /**
   * Prints some statistics about the graph
   */
  public void printGraphStatistics() {
    System.out.println("********************************************");
    System.out.println("Number of node: " + this.labels.size());
    System.out.println("Number of edges: " + this.numberOfEdges);
  }
}
