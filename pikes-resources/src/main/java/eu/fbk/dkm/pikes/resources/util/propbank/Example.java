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
        "inflectionOrNoteOrTextOrArgOrRel" }) @XmlRootElement(name = "example") public class Example {

    @XmlAttribute(name = "name") @XmlJavaTypeAdapter(NormalizedStringAdapter.class) protected String name;
    @XmlAttribute(name = "type") @XmlJavaTypeAdapter(NormalizedStringAdapter.class) protected String type;
    @XmlAttribute(name = "src") @XmlJavaTypeAdapter(NormalizedStringAdapter.class) protected String src;
    @XmlElements({ @XmlElement(name = "inflection", type = Inflection.class),
            @XmlElement(name = "note", type = Note.class),
            @XmlElement(name = "text", type = Text.class),
            @XmlElement(name = "arg", type = Arg.class),
            @XmlElement(name = "rel", type = Rel.class) }) protected List<Object> inflectionOrNoteOrTextOrArgOrRel;

    /**
     * Recupera il valore della proprietà name.
     *
     * @return possible object is
     * {@link String }
     */
    public String getName() {
        return name;
    }

    /**
     * Imposta il valore della proprietà name.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setName(String value) {
        this.name = value;
    }

    /**
     * Recupera il valore della proprietà type.
     *
     * @return possible object is
     * {@link String }
     */
    public String getType() {
        return type;
    }

    /**
     * Imposta il valore della proprietà type.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setType(String value) {
        this.type = value;
    }

    /**
     * Recupera il valore della proprietà src.
     *
     * @return possible object is
     * {@link String }
     */
    public String getSrc() {
        return src;
    }

    /**
     * Imposta il valore della proprietà src.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setSrc(String value) {
        this.src = value;
    }

    /**
     * Gets the value of the inflectionOrNoteOrTextOrArgOrRel property.
     * <p>
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the inflectionOrNoteOrTextOrArgOrRel property.
     * <p>
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getInflectionOrNoteOrTextOrArgOrRel().add(newItem);
     * </pre>
     * <p>
     * <p>
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Inflection }
     * {@link Note }
     * {@link Text }
     * {@link Arg }
     * {@link Rel }
     */
    public List<Object> getInflectionOrNoteOrTextOrArgOrRel() {
        if (inflectionOrNoteOrTextOrArgOrRel == null) {
            inflectionOrNoteOrTextOrArgOrRel = new ArrayList<Object>();
        }
        return this.inflectionOrNoteOrTextOrArgOrRel;
    }

}
