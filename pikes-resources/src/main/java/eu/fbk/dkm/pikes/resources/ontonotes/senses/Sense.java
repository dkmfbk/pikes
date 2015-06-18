//
// Questo file è stato generato dall'architettura JavaTM per XML Binding (JAXB) Reference Implementation, v2.2.8-b130911.1802 
// Vedere <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Qualsiasi modifica a questo file andrà persa durante la ricompilazione dello schema di origine. 
// Generato il: 2015.05.07 alle 12:32:30 PM CEST 
//


package eu.fbk.dkm.pikes.resources.ontonotes.senses;

import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.NormalizedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;


/**
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "commentary",
    "examples",
    "mappings",
    "sensemeta"
})
@XmlRootElement(name = "sense")
public class Sense {

    @XmlAttribute(name = "n", required = true)
    @XmlJavaTypeAdapter(NormalizedStringAdapter.class)
    protected String n;
    @XmlAttribute(name = "type")
    @XmlJavaTypeAdapter(NormalizedStringAdapter.class)
    protected String type;
    @XmlAttribute(name = "name", required = true)
    @XmlJavaTypeAdapter(NormalizedStringAdapter.class)
    protected String name;
    @XmlAttribute(name = "group", required = true)
    @XmlJavaTypeAdapter(NormalizedStringAdapter.class)
    protected String group;
    protected String commentary;
    @XmlElement(required = true)
    protected String examples;
    @XmlElement(required = true)
    protected Mappings mappings;
    @XmlElement(name = "SENSE_META", required = true)
    protected SENSEMETA sensemeta;

    /**
     * Recupera il valore della proprietà n.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getN() {
        return n;
    }

    /**
     * Imposta il valore della proprietà n.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setN(String value) {
        this.n = value;
    }

    /**
     * Recupera il valore della proprietà type.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getType() {
        return type;
    }

    /**
     * Imposta il valore della proprietà type.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setType(String value) {
        this.type = value;
    }

    /**
     * Recupera il valore della proprietà name.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getName() {
        return name;
    }

    /**
     * Imposta il valore della proprietà name.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setName(String value) {
        this.name = value;
    }

    /**
     * Recupera il valore della proprietà group.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getGroup() {
        return group;
    }

    /**
     * Imposta il valore della proprietà group.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setGroup(String value) {
        this.group = value;
    }

    /**
     * Recupera il valore della proprietà commentary.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getCommentary() {
        return commentary;
    }

    /**
     * Imposta il valore della proprietà commentary.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setCommentary(String value) {
        this.commentary = value;
    }

    /**
     * Recupera il valore della proprietà examples.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getExamples() {
        return examples;
    }

    /**
     * Imposta il valore della proprietà examples.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setExamples(String value) {
        this.examples = value;
    }

    /**
     * Recupera il valore della proprietà mappings.
     * 
     * @return
     *     possible object is
     *     {@link Mappings }
     *     
     */
    public Mappings getMappings() {
        return mappings;
    }

    /**
     * Imposta il valore della proprietà mappings.
     * 
     * @param value
     *     allowed object is
     *     {@link Mappings }
     *     
     */
    public void setMappings(Mappings value) {
        this.mappings = value;
    }

    /**
     * Recupera il valore della proprietà sensemeta.
     * 
     * @return
     *     possible object is
     *     {@link SENSEMETA }
     *     
     */
    public SENSEMETA getSENSEMETA() {
        return sensemeta;
    }

    /**
     * Imposta il valore della proprietà sensemeta.
     * 
     * @param value
     *     allowed object is
     *     {@link SENSEMETA }
     *     
     */
    public void setSENSEMETA(SENSEMETA value) {
        this.sensemeta = value;
    }

}
