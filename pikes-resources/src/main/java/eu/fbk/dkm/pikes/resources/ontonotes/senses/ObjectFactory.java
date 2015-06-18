//
// Questo file è stato generato dall'architettura JavaTM per XML Binding (JAXB) Reference Implementation, v2.2.8-b130911.1802 
// Vedere <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Qualsiasi modifica a questo file andrà persa durante la ricompilazione dello schema di origine. 
// Generato il: 2015.05.07 alle 12:32:30 PM CEST 
//


package eu.fbk.dkm.pikes.resources.ontonotes.senses;

import javax.xml.bind.annotation.XmlRegistry;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the eu.fbk.naftools.resources.eu.fbk.dkm.pikes.resources.ontonotes.senses package.
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {


    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: eu.fbk.naftools.resources.eu.fbk.dkm.pikes.resources.ontonotes.senses
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link SENSEMETA }
     * 
     */
    public SENSEMETA createSENSEMETA() {
        return new SENSEMETA();
    }

    /**
     * Create an instance of {@link Sense }
     * 
     */
    public Sense createSense() {
        return new Sense();
    }

    /**
     * Create an instance of {@link Mappings }
     * 
     */
    public Mappings createMappings() {
        return new Mappings();
    }

    /**
     * Create an instance of {@link Inventory }
     * 
     */
    public Inventory createInventory() {
        return new Inventory();
    }

    /**
     * Create an instance of {@link WORDMETA }
     * 
     */
    public WORDMETA createWORDMETA() {
        return new WORDMETA();
    }

    /**
     * Create an instance of {@link Wn }
     * 
     */
    public Wn createWn() {
        return new Wn();
    }

}
