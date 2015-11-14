//
// Questo file è stato generato dall'architettura JavaTM per XML Binding (JAXB) Reference Implementation, v2.2.8-b130911.1802 
// Vedere <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Qualsiasi modifica a questo file andrà persa durante la ricompilazione dello schema di origine. 
// Generato il: 2015.05.07 alle 12:32:49 PM CEST 
//

package eu.fbk.dkm.pikes.resources.util.propbank;

import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.NormalizedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
@XmlAccessorType(XmlAccessType.FIELD) @XmlType(name = "", propOrder = {
        "noteOrRolesOrExample" }) @XmlRootElement(name = "roleset") public class Roleset {

    @XmlAttribute(name = "id", required = true) @XmlJavaTypeAdapter(CollapsedStringAdapter.class) @XmlID protected String id;
    @XmlAttribute(name = "name") @XmlJavaTypeAdapter(NormalizedStringAdapter.class) protected String name;
    @XmlAttribute(name = "vncls") @XmlJavaTypeAdapter(NormalizedStringAdapter.class) protected String vncls;
    @XmlAttribute(name = "framnet") @XmlJavaTypeAdapter(NormalizedStringAdapter.class) protected String framnet;
    @XmlAttribute(name = "source") @XmlJavaTypeAdapter(NormalizedStringAdapter.class) protected String source;
    @XmlElements({ @XmlElement(name = "note", type = Note.class),
            @XmlElement(name = "roles", type = Roles.class),
            @XmlElement(name = "example", type = Example.class) }) protected List<Object> noteOrRolesOrExample;

    /**
     * Recupera il valore della proprietà id.
     *
     * @return possible object is
     * {@link String }
     */
    public String getId() {
        return id;
    }

    /**
     * Imposta il valore della proprietà id.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setId(String value) {
        this.id = value;
    }

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
     * Recupera il valore della proprietà vncls.
     *
     * @return possible object is
     * {@link String }
     */
    public String getVncls() {
        return vncls;
    }

    /**
     * Imposta il valore della proprietà vncls.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setVncls(String value) {
        this.vncls = value;
    }

    /**
     * Recupera il valore della proprietà framnet.
     *
     * @return possible object is
     * {@link String }
     */
    public String getFramnet() {
        return framnet;
    }

    /**
     * Imposta il valore della proprietà framnet.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setFramnet(String value) {
        this.framnet = value;
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
     * Gets the value of the noteOrRolesOrExample property.
     * <p>
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the noteOrRolesOrExample property.
     * <p>
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getNoteOrRolesOrExample().add(newItem);
     * </pre>
     * <p>
     * <p>
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Note }
     * {@link Roles }
     * {@link Example }
     */
    public List<Object> getNoteOrRolesOrExample() {
        if (noteOrRolesOrExample == null) {
            noteOrRolesOrExample = new ArrayList<Object>();
        }
        return this.noteOrRolesOrExample;
    }

}
