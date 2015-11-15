//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2015.11.12 at 10:43:37 PM CET 
//


package eu.fbk.dkm.pikes.resources.util.fnlu;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for labelType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="labelType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;attribute name="name" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="start" type="{http://framenet.icsi.berkeley.edu}labelSpanType" />
 *       &lt;attribute name="end" type="{http://framenet.icsi.berkeley.edu}labelSpanType" />
 *       &lt;attribute name="fgColor" type="{http://framenet.icsi.berkeley.edu}RGBColorType" />
 *       &lt;attribute name="bgColor" type="{http://framenet.icsi.berkeley.edu}RGBColorType" />
 *       &lt;attribute name="itype">
 *         &lt;simpleType>
 *           &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *             &lt;enumeration value="APos"/>
 *             &lt;enumeration value="CNI"/>
 *             &lt;enumeration value="INI"/>
 *             &lt;enumeration value="DNI"/>
 *             &lt;enumeration value="INC"/>
 *           &lt;/restriction>
 *         &lt;/simpleType>
 *       &lt;/attribute>
 *       &lt;attribute name="feID" type="{http://www.w3.org/2001/XMLSchema}int" />
 *       &lt;attribute name="cBy" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "labelType")
public class LabelType {

    @XmlAttribute(name = "name", required = true)
    protected String name;
    @XmlAttribute(name = "start")
    protected Integer start;
    @XmlAttribute(name = "end")
    protected Integer end;
    @XmlAttribute(name = "fgColor")
    protected String fgColor;
    @XmlAttribute(name = "bgColor")
    protected String bgColor;
    @XmlAttribute(name = "itype")
    protected String itype;
    @XmlAttribute(name = "feID")
    protected Integer feID;
    @XmlAttribute(name = "cBy")
    protected String cBy;

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
     * Gets the value of the start property.
     * 
     * @return
     *     possible object is
     *     {@link Integer }
     *     
     */
    public Integer getStart() {
        return start;
    }

    /**
     * Sets the value of the start property.
     * 
     * @param value
     *     allowed object is
     *     {@link Integer }
     *     
     */
    public void setStart(Integer value) {
        this.start = value;
    }

    /**
     * Gets the value of the end property.
     * 
     * @return
     *     possible object is
     *     {@link Integer }
     *     
     */
    public Integer getEnd() {
        return end;
    }

    /**
     * Sets the value of the end property.
     * 
     * @param value
     *     allowed object is
     *     {@link Integer }
     *     
     */
    public void setEnd(Integer value) {
        this.end = value;
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
     * Gets the value of the itype property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getItype() {
        return itype;
    }

    /**
     * Sets the value of the itype property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setItype(String value) {
        this.itype = value;
    }

    /**
     * Gets the value of the feID property.
     * 
     * @return
     *     possible object is
     *     {@link Integer }
     *     
     */
    public Integer getFeID() {
        return feID;
    }

    /**
     * Sets the value of the feID property.
     * 
     * @param value
     *     allowed object is
     *     {@link Integer }
     *     
     */
    public void setFeID(Integer value) {
        this.feID = value;
    }

    /**
     * Gets the value of the cBy property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getCBy() {
        return cBy;
    }

    /**
     * Sets the value of the cBy property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setCBy(String value) {
        this.cBy = value;
    }

    @Override public String toString() {
        return "LabelType{" +
                "name='" + name + '\'' +
                ", start=" + start +
                ", end=" + end +
                ", fgColor='" + fgColor + '\'' +
                ", bgColor='" + bgColor + '\'' +
                ", itype='" + itype + '\'' +
                ", feID=" + feID +
                ", cBy='" + cBy + '\'' +
                '}';
    }
}
