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
@XmlType(name = "")
@XmlRootElement(name = "WORD_META")
public class WORDMETA {

    @XmlAttribute(name = "authors", required = true)
    @XmlJavaTypeAdapter(NormalizedStringAdapter.class)
    protected String authors;
    @XmlAttribute(name = "sample_score")
    @XmlJavaTypeAdapter(NormalizedStringAdapter.class)
    protected String sampleScore;

    /**
     * Recupera il valore della proprietà authors.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getAuthors() {
        return authors;
    }

    /**
     * Imposta il valore della proprietà authors.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setAuthors(String value) {
        this.authors = value;
    }

    /**
     * Recupera il valore della proprietà sampleScore.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSampleScore() {
        return sampleScore;
    }

    /**
     * Imposta il valore della proprietà sampleScore.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSampleScore(String value) {
        this.sampleScore = value;
    }

}
