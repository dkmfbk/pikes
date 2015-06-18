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
import java.util.ArrayList;
import java.util.List;


/**
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "commentary",
    "sense",
    "wordmeta"
})
@XmlRootElement(name = "inventory")
public class Inventory {

    @XmlAttribute(name = "lemma", required = true)
    @XmlJavaTypeAdapter(NormalizedStringAdapter.class)
    protected String lemma;
    protected String commentary;
    @XmlElement(required = true)
    protected List<Sense> sense;
    @XmlElement(name = "WORD_META", required = true)
    protected WORDMETA wordmeta;

    /**
     * Recupera il valore della proprietà lemma.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getLemma() {
        return lemma;
    }

    /**
     * Imposta il valore della proprietà lemma.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setLemma(String value) {
        this.lemma = value;
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
     * Gets the value of the sense property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the sense property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getSense().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Sense }
     * 
     * 
     */
    public List<Sense> getSense() {
        if (sense == null) {
            sense = new ArrayList<Sense>();
        }
        return this.sense;
    }

    /**
     * Recupera il valore della proprietà wordmeta.
     * 
     * @return
     *     possible object is
     *     {@link WORDMETA }
     *     
     */
    public WORDMETA getWORDMETA() {
        return wordmeta;
    }

    /**
     * Imposta il valore della proprietà wordmeta.
     * 
     * @param value
     *     allowed object is
     *     {@link WORDMETA }
     *     
     */
    public void setWORDMETA(WORDMETA value) {
        this.wordmeta = value;
    }

}
