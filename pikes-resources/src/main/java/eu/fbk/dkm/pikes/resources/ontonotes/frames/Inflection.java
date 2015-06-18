//
// Questo file è stato generato dall'architettura JavaTM per XML Binding (JAXB) Reference Implementation, v2.2.8-b130911.1802 
// Vedere <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Qualsiasi modifica a questo file andrà persa durante la ricompilazione dello schema di origine. 
// Generato il: 2015.05.07 alle 12:32:49 PM CEST 
//


package eu.fbk.dkm.pikes.resources.ontonotes.frames;

import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;


/**
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "")
@XmlRootElement(name = "inflection")
public class Inflection {

    @XmlAttribute(name = "person")
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    protected String person;
    @XmlAttribute(name = "tense")
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    protected String tense;
    @XmlAttribute(name = "aspect")
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    protected String aspect;
    @XmlAttribute(name = "voice")
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    protected String voice;
    @XmlAttribute(name = "form")
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    protected String form;

    /**
     * Recupera il valore della proprietà person.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getPerson() {
        if (person == null) {
            return "ns";
        } else {
            return person;
        }
    }

    /**
     * Imposta il valore della proprietà person.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setPerson(String value) {
        this.person = value;
    }

    /**
     * Recupera il valore della proprietà tense.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getTense() {
        if (tense == null) {
            return "ns";
        } else {
            return tense;
        }
    }

    /**
     * Imposta il valore della proprietà tense.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setTense(String value) {
        this.tense = value;
    }

    /**
     * Recupera il valore della proprietà aspect.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getAspect() {
        if (aspect == null) {
            return "ns";
        } else {
            return aspect;
        }
    }

    /**
     * Imposta il valore della proprietà aspect.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setAspect(String value) {
        this.aspect = value;
    }

    /**
     * Recupera il valore della proprietà voice.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getVoice() {
        if (voice == null) {
            return "ns";
        } else {
            return voice;
        }
    }

    /**
     * Imposta il valore della proprietà voice.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setVoice(String value) {
        this.voice = value;
    }

    /**
     * Recupera il valore della proprietà form.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getForm() {
        if (form == null) {
            return "ns";
        } else {
            return form;
        }
    }

    /**
     * Imposta il valore della proprietà form.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setForm(String value) {
        this.form = value;
    }

}
