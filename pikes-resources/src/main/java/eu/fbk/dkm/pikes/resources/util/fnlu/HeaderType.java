//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2015.11.12 at 10:43:37 PM CET 
//


package eu.fbk.dkm.pikes.resources.util.fnlu;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for headerType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="headerType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="corpus" type="{http://framenet.icsi.berkeley.edu}corpDocType" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="frame" minOccurs="0">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;sequence>
 *                   &lt;element name="FE" maxOccurs="unbounded">
 *                     &lt;complexType>
 *                       &lt;complexContent>
 *                         &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                           &lt;attribute name="name" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *                           &lt;attribute name="abbrev" type="{http://www.w3.org/2001/XMLSchema}string" />
 *                           &lt;attribute name="type" use="required" type="{http://framenet.icsi.berkeley.edu}coreType" />
 *                           &lt;attribute name="bgColor" use="required" type="{http://framenet.icsi.berkeley.edu}RGBColorType" />
 *                           &lt;attribute name="fgColor" use="required" type="{http://framenet.icsi.berkeley.edu}RGBColorType" />
 *                         &lt;/restriction>
 *                       &lt;/complexContent>
 *                     &lt;/complexType>
 *                   &lt;/element>
 *                 &lt;/sequence>
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "headerType", propOrder = {
    "corpus",
    "frame"
})
public class HeaderType {

    protected List<CorpDocType> corpus;
    protected HeaderType.Frame frame;

    /**
     * Gets the value of the corpus property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the corpus property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getCorpus().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link CorpDocType }
     * 
     * 
     */
    public List<CorpDocType> getCorpus() {
        if (corpus == null) {
            corpus = new ArrayList<CorpDocType>();
        }
        return this.corpus;
    }

    /**
     * Gets the value of the frame property.
     * 
     * @return
     *     possible object is
     *     {@link HeaderType.Frame }
     *     
     */
    public HeaderType.Frame getFrame() {
        return frame;
    }

    /**
     * Sets the value of the frame property.
     * 
     * @param value
     *     allowed object is
     *     {@link HeaderType.Frame }
     *     
     */
    public void setFrame(HeaderType.Frame value) {
        this.frame = value;
    }


    /**
     * <p>Java class for anonymous complex type.
     * 
     * <p>The following schema fragment specifies the expected content contained within this class.
     * 
     * <pre>
     * &lt;complexType>
     *   &lt;complexContent>
     *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
     *       &lt;sequence>
     *         &lt;element name="FE" maxOccurs="unbounded">
     *           &lt;complexType>
     *             &lt;complexContent>
     *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
     *                 &lt;attribute name="name" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
     *                 &lt;attribute name="abbrev" type="{http://www.w3.org/2001/XMLSchema}string" />
     *                 &lt;attribute name="type" use="required" type="{http://framenet.icsi.berkeley.edu}coreType" />
     *                 &lt;attribute name="bgColor" use="required" type="{http://framenet.icsi.berkeley.edu}RGBColorType" />
     *                 &lt;attribute name="fgColor" use="required" type="{http://framenet.icsi.berkeley.edu}RGBColorType" />
     *               &lt;/restriction>
     *             &lt;/complexContent>
     *           &lt;/complexType>
     *         &lt;/element>
     *       &lt;/sequence>
     *     &lt;/restriction>
     *   &lt;/complexContent>
     * &lt;/complexType>
     * </pre>
     * 
     * 
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {
        "fe"
    })
    public static class Frame {

        @XmlElement(name = "FE", required = true)
        protected List<HeaderType.Frame.FE> fe;

        /**
         * Gets the value of the fe property.
         * 
         * <p>
         * This accessor method returns a reference to the live list,
         * not a snapshot. Therefore any modification you make to the
         * returned list will be present inside the JAXB object.
         * This is why there is not a <CODE>set</CODE> method for the fe property.
         * 
         * <p>
         * For example, to add a new item, do as follows:
         * <pre>
         *    getFE().add(newItem);
         * </pre>
         * 
         * 
         * <p>
         * Objects of the following type(s) are allowed in the list
         * {@link HeaderType.Frame.FE }
         * 
         * 
         */
        public List<HeaderType.Frame.FE> getFE() {
            if (fe == null) {
                fe = new ArrayList<HeaderType.Frame.FE>();
            }
            return this.fe;
        }


        /**
         * <p>Java class for anonymous complex type.
         * 
         * <p>The following schema fragment specifies the expected content contained within this class.
         * 
         * <pre>
         * &lt;complexType>
         *   &lt;complexContent>
         *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
         *       &lt;attribute name="name" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
         *       &lt;attribute name="abbrev" type="{http://www.w3.org/2001/XMLSchema}string" />
         *       &lt;attribute name="type" use="required" type="{http://framenet.icsi.berkeley.edu}coreType" />
         *       &lt;attribute name="bgColor" use="required" type="{http://framenet.icsi.berkeley.edu}RGBColorType" />
         *       &lt;attribute name="fgColor" use="required" type="{http://framenet.icsi.berkeley.edu}RGBColorType" />
         *     &lt;/restriction>
         *   &lt;/complexContent>
         * &lt;/complexType>
         * </pre>
         * 
         * 
         */
        @XmlAccessorType(XmlAccessType.FIELD)
        @XmlType(name = "")
        public static class FE {

            @XmlAttribute(name = "name", required = true)
            protected String name;
            @XmlAttribute(name = "abbrev")
            protected String abbrev;
            @XmlAttribute(name = "type", required = true)
            protected CoreType type;
            @XmlAttribute(name = "bgColor", required = true)
            protected String bgColor;
            @XmlAttribute(name = "fgColor", required = true)
            protected String fgColor;

            /**
             * Gets the value of the name property.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            public String getName() {
                return name;
            }

            /**
             * Sets the value of the name property.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            public void setName(String value) {
                this.name = value;
            }

            /**
             * Gets the value of the abbrev property.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            public String getAbbrev() {
                return abbrev;
            }

            /**
             * Sets the value of the abbrev property.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            public void setAbbrev(String value) {
                this.abbrev = value;
            }

            /**
             * Gets the value of the type property.
             * 
             * @return
             *     possible object is
             *     {@link CoreType }
             *     
             */
            public CoreType getType() {
                return type;
            }

            /**
             * Sets the value of the type property.
             * 
             * @param value
             *     allowed object is
             *     {@link CoreType }
             *     
             */
            public void setType(CoreType value) {
                this.type = value;
            }

            /**
             * Gets the value of the bgColor property.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            public String getBgColor() {
                return bgColor;
            }

            /**
             * Sets the value of the bgColor property.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            public void setBgColor(String value) {
                this.bgColor = value;
            }

            /**
             * Gets the value of the fgColor property.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            public String getFgColor() {
                return fgColor;
            }

            /**
             * Sets the value of the fgColor property.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            public void setFgColor(String value) {
                this.fgColor = value;
            }

        }

    }

}