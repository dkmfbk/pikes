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
@XmlRootElement(name = "SENSE_META")
public class SENSEMETA {

    @XmlAttribute(name = "clarity")
    @XmlJavaTypeAdapter(NormalizedStringAdapter.class)
    protected String clarity;

    /**
     * Recupera il valore della proprietà clarity.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getClarity() {
        return clarity;
    }

    /**
     * Imposta il valore della proprietà clarity.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setClarity(String value) {
        this.clarity = value;
    }

}
