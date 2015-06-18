//
// Questo file è stato generato dall'architettura JavaTM per XML Binding (JAXB) Reference Implementation, v2.2.8-b130911.1802 
// Vedere <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Qualsiasi modifica a questo file andrà persa durante la ricompilazione dello schema di origine. 
// Generato il: 2015.05.07 alle 12:32:49 PM CEST 
//


package eu.fbk.dkm.pikes.resources.ontonotes.frames;

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
    "noteOrRoleset"
})
@XmlRootElement(name = "predicate")
public class Predicate {

    @XmlAttribute(name = "lemma", required = true)
    @XmlJavaTypeAdapter(NormalizedStringAdapter.class)
    protected String lemma;
    @XmlElements({
        @XmlElement(name = "note", type = Note.class),
        @XmlElement(name = "roleset", type = Roleset.class)
    })
    protected List<Object> noteOrRoleset;

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
     * Gets the value of the noteOrRoleset property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the noteOrRoleset property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getNoteOrRoleset().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Note }
     * {@link Roleset }
     * 
     * 
     */
    public List<Object> getNoteOrRoleset() {
        if (noteOrRoleset == null) {
            noteOrRoleset = new ArrayList<Object>();
        }
        return this.noteOrRoleset;
    }

}
