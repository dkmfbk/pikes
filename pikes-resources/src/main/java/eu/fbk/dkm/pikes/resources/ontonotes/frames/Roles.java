//
// Questo file è stato generato dall'architettura JavaTM per XML Binding (JAXB) Reference Implementation, v2.2.8-b130911.1802 
// Vedere <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Qualsiasi modifica a questo file andrà persa durante la ricompilazione dello schema di origine. 
// Generato il: 2015.05.07 alle 12:32:49 PM CEST 
//


package eu.fbk.dkm.pikes.resources.ontonotes.frames;

import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;


/**
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "noteOrRole"
})
@XmlRootElement(name = "roles")
public class Roles {

    @XmlElements({
        @XmlElement(name = "note", type = Note.class),
        @XmlElement(name = "role", type = Role.class)
    })
    protected List<Object> noteOrRole;

    /**
     * Gets the value of the noteOrRole property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the noteOrRole property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getNoteOrRole().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Note }
     * {@link Role }
     * 
     * 
     */
    public List<Object> getNoteOrRole() {
        if (noteOrRole == null) {
            noteOrRole = new ArrayList<Object>();
        }
        return this.noteOrRole;
    }

}
