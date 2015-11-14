//
// Questo file è stato generato dall'architettura JavaTM per XML Binding (JAXB) Reference Implementation, v2.2.8-b130911.1802 
// Vedere <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Qualsiasi modifica a questo file andrà persa durante la ricompilazione dello schema di origine. 
// Generato il: 2015.05.07 alle 12:32:49 PM CEST 
//

package eu.fbk.dkm.pikes.resources.util.propbank;

import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.NormalizedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
@XmlAccessorType(XmlAccessType.FIELD) @XmlType(name = "", propOrder = {
        "vnrole" }) @XmlRootElement(name = "role") public class Role {

    @XmlAttribute(name = "n", required = true) @XmlJavaTypeAdapter(NormalizedStringAdapter.class) protected String n;
    @XmlAttribute(name = "f") @XmlJavaTypeAdapter(NormalizedStringAdapter.class) protected String f;
    @XmlAttribute(name = "descr", required = true) @XmlJavaTypeAdapter(NormalizedStringAdapter.class) protected String descr;
    @XmlAttribute(name = "source") @XmlJavaTypeAdapter(NormalizedStringAdapter.class) protected String source;
    protected List<Vnrole> vnrole;

    /**
     * Recupera il valore della proprietà n.
     *
     * @return possible object is
     * {@link String }
     */
    public String getN() {
        return n;
    }

    /**
     * Imposta il valore della proprietà n.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setN(String value) {
        this.n = value;
    }

    /**
     * Recupera il valore della proprietà f.
     *
     * @return possible object is
     * {@link String }
     */
    public String getF() {
        return f;
    }

    /**
     * Imposta il valore della proprietà f.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setF(String value) {
        this.f = value;
    }

    /**
     * Recupera il valore della proprietà descr.
     *
     * @return possible object is
     * {@link String }
     */
    public String getDescr() {
        return descr;
    }

    /**
     * Imposta il valore della proprietà descr.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setDescr(String value) {
        this.descr = value;
    }

    /**
     * Recupera il valore della proprietà source.
     *
     * @return possible object is
     * {@link String }
     */
    public String getSource() {
        return source;
    }

    /**
     * Imposta il valore della proprietà source.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setSource(String value) {
        this.source = value;
    }

    /**
     * Gets the value of the vnrole property.
     * <p>
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the vnrole property.
     * <p>
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getVnrole().add(newItem);
     * </pre>
     * <p>
     * <p>
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Vnrole }
     */
    public List<Vnrole> getVnrole() {
        if (vnrole == null) {
            vnrole = new ArrayList<Vnrole>();
        }
        return this.vnrole;
    }

}
