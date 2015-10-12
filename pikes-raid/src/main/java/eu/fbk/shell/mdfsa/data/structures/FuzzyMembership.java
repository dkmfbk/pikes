package eu.fbk.shell.mdfsa.data.structures;

import java.io.Serializable;

public class FuzzyMembership implements Serializable {

  private static final long serialVersionUID = 1L;
  
  private double a;
  private double b;
  private double c;
  private double d;
  
  public FuzzyMembership(double a, double b, double c, double d) {
    this.a = a;
    this.b = b;
    this.c = c;
    this.d = d;
  }

  
  public double getA() {
    return a;
  }

  public void setA(double a) {
    this.a = a;
  }

  public double getB() {
    return b;
  }

  public void setB(double b) {
    this.b = b;
  }

  public double getC() {
    return c;
  }

  public void setC(double c) {
    this.c = c;
  }

  public double getD() {
    return d;
  }

  public void setD(double d) {
    this.d = d;
  }
  
  public double getCentroid(double oldFlag) {
    double centroid = (this.b + this.c) / 2.0;
    //if(this.b < 0 && this.c > 0) {
    //  centroid = 0.0;
    //}
    return centroid;
  }
  
  public double getCentroid() {
    double centroid = 0.0;
    
    if(this.b == this.d) {
      centroid = this.b;
    } else if(this.a == this.c) {
      centroid = this.a;
    } else {
      double xCoeffR1 = 1 / (this.b - this.d);
      double yCoeffR1 = 1.0;
      double cCoeffR1 = (this.d / (this.b - this.d)) * -1;
      double xCoeffR2 = 1 / (this.c - this.a);
      double yCoeffR2 = 1.0;
      double cCoeffR2 = (this.a / (this.c - this.a)) * -1;
      double x = (cCoeffR1 - cCoeffR2) / (xCoeffR1 - xCoeffR2);
      centroid = x;
    }
    
    return centroid;
  }
  
  
  
  public double getCentroidXAxis() {
    double centroid = 0.0;
    
    double num = (c * c) + (d * d) + (c * d) - (a * a) - (b * b) - (a * b);
    double den = 3 * (c + d - a - b);
    centroid = num / den;
    
    return centroid;
  }
  
}
