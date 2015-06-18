//
// Questo file è stato generato dall'architettura JavaTM per XML Binding (JAXB) Reference Implementation, v2.2.8-b130911.1802 
// Vedere <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Qualsiasi modifica a questo file andrà persa durante la ricompilazione dello schema di origine. 
// Generato il: 2015.05.07 alle 12:32:30 PM CEST 
//


package eu.fbk.dkm.pikes.resources.ontonotes.senses;

import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;


/**
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "grSense",
    "wn",
    "omega",
    "pb",
    "vn",
    "fn"
})
@XmlRootElement(name = "mappings")
public class Mappings {

    @XmlElement(name = "gr_sense")
    protected String grSense;
    @XmlElement(required = true)
    protected List<Wn> wn;
    @XmlElement(required = true)
    protected String omega;
    @XmlElement(required = true)
    protected String pb;
    protected String vn;
    protected String fn;

    /**
     * Recupera il valore della proprietà grSense.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getGrSense() {
        return grSense;
    }

    /**
     * Imposta il valore della proprietà grSense.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setGrSense(String value) {
        this.grSense = value;
    }

    /**
     * Gets the value of the wn property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the wn property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getWn().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Wn }
     * 
     * 
     */
    public List<Wn> getWn() {
        if (wn == null) {
            wn = new ArrayList<Wn>();
        }
        return this.wn;
    }

    /**
     * Recupera il valore della proprietà omega.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getOmega() {
        return omega;
    }

    /**
     * Imposta il valore della proprietà omega.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setOmega(String value) {
        this.omega = value;
    }

    /**
     * Recupera il valore della proprietà pb.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getPb() {
        return pb;
    }

    /**
     * Imposta il valore della proprietà pb.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setPb(String value) {
        this.pb = value;
    }

    /**
     * Recupera il valore della proprietà vn.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getVn() {
        return vn;
    }

    /**
     * Imposta il valore della proprietà vn.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setVn(String value) {
        this.vn = value;
    }

    /**
     * Recupera il valore della proprietà fn.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getFn() {
        return fn;
    }

    /**
     * Imposta il valore della proprietà fn.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setFn(String value) {
        this.fn = value;
    }

}
