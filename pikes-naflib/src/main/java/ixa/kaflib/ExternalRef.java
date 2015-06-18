package ixa.kaflib;

import java.io.Serializable;
import java.util.Objects;


public class ExternalRef implements Serializable {
    private String resource;
    private String reference;
    private float confidence = -1.0f;
    private String source = null;
    private ExternalRef externalRef;

    ExternalRef(String resource, String reference) {
	this.resource = resource;
	this.reference = reference;
    }

    ExternalRef(ExternalRef externalReference) {
	this.resource = externalReference.resource;
	this.reference = externalReference.reference;
	this.confidence = externalReference.confidence;
        this.source = externalReference.source;
	if (externalReference.externalRef != null) {
	    this.externalRef = new ExternalRef(externalReference.externalRef);
	}
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getResource() {
	return resource;
    }

    public void setResource(String val) {
	resource = val;
    }

    public String getReference() {
	return reference;
    }

    public void setReference(String val) {
	reference = val;
    }

    public boolean hasConfidence() {
	return confidence != -1.0;
    }

    public float getConfidence() {
	return confidence;
    }

    public void setConfidence(float val) {
	confidence = val;
    }

    public boolean hasExternalRef() {
	return this.externalRef != null;
    }

    public ExternalRef getExternalRef() {
	return externalRef;
    }

    public void setExternalRef(ExternalRef externalRef) {
	this.externalRef = externalRef;
    }
    
    @Override
    public boolean equals(Object object) {
        if (object == this) {
            return true;
        }
        if (!(object instanceof ExternalRef)) {
            return false;
        }
        ExternalRef other = (ExternalRef) object;
        return Objects.equals(resource, other.resource)
                && Objects.equals(reference, other.reference) && confidence == other.confidence
                && Objects.equals(source, other.source)
                && Objects.equals(externalRef, other.externalRef);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(resource, reference, confidence, source, externalRef);
    }

    @Override
    public String toString() {
        return resource + "=" + reference + " (conf: " + confidence + ", source: " + source + ")";
    }
    
}
