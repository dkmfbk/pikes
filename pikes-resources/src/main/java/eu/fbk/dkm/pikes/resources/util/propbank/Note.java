//
// Questo file è stato generato dall'architettura JavaTM per XML Binding (JAXB) Reference Implementation, v2.2.8-b130911.1802 
// Vedere <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Qualsiasi modifica a questo file andrà persa durante la ricompilazione dello schema di origine. 
// Generato il: 2015.05.07 alle 12:32:49 PM CEST 
//

package eu.fbk.dkm.pikes.resources.util.propbank;

import javax.xml.bind.annotation.*;

/**
 *
 */
@XmlAccessorType(XmlAccessType.FIELD) @XmlType(name = "", propOrder = {
        "value" }) @XmlRootElement(name = "note") public class Note {

    @XmlValue protected String value;

    /**
     * Recupera il valore della proprietà value.
     *
     * @return possible object is
     * {@link String }
     */
    public String getvalue() {
        return value;
    }

    /**
     * Imposta il valore della proprietà value.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setvalue(String value) {
        this.value = value;
    }

}
