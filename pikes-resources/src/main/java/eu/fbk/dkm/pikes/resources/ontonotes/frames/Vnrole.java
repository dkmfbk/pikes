//
// Questo file è stato generato dall'architettura JavaTM per XML Binding (JAXB) Reference Implementation, v2.2.8-b130911.1802 
// Vedere <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Qualsiasi modifica a questo file andrà persa durante la ricompilazione dello schema di origine. 
// Generato il: 2015.05.07 alle 12:32:49 PM CEST 
//


package eu.fbk.dkm.pikes.resources.ontonotes.frames;

import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.NormalizedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;


/**
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "")
@XmlRootElement(name = "vnrole")
public class Vnrole {

    @XmlAttribute(name = "vncls", required = true)
    @XmlJavaTypeAdapter(NormalizedStringAdapter.class)
    protected String vncls;
    @XmlAttribute(name = "vntheta", required = true)
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    protected String vntheta;

    /**
     * Recupera il valore della proprietà vncls.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getVncls() {
        return vncls;
    }

    /**
     * Imposta il valore della proprietà vncls.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setVncls(String value) {
        this.vncls = value;
    }

    /**
     * Recupera il valore della proprietà vntheta.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getVntheta() {
        return vntheta;
    }

    /**
     * Imposta il valore della proprietà vntheta.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setVntheta(String value) {
        this.vntheta = value;
    }

}
