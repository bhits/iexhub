
package PDQSupplier.org.hl7.v3;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for QUQI_IN000003UV01_Type complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="QUQI_IN000003UV01_Type">
 *   &lt;complexContent>
 *     &lt;extension base="{urn:hl7-org:v3}QUQI_IN000003UV01.MCCI_MT000300UV01.Message">
 *       &lt;attribute name="ITSVersion" use="required" type="{http://www.w3.org/2001/XMLSchema}string" fixed="XML_1.0" />
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "QUQI_IN000003UV01_Type")
public class QUQIIN000003UV01Type
    extends QUQIIN000003UV01MCCIMT000300UV01Message
{

    @XmlAttribute(name = "ITSVersion", required = true)
    protected String itsVersion;

    /**
     * Gets the value of the itsVersion property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getITSVersion() {
        if (itsVersion == null) {
            return "XML_1.0";
        } else {
            return itsVersion;
        }
    }

    /**
     * Sets the value of the itsVersion property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setITSVersion(String value) {
        this.itsVersion = value;
    }

}
