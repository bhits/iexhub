
package PDQSupplier.org.hl7.v3;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlMixed;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for adxp.streetName complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="adxp.streetName">
 *   &lt;complexContent>
 *     &lt;restriction base="{urn:hl7-org:v3}ADXP">
 *       &lt;attribute name="partType" type="{urn:hl7-org:v3}AddressPartType" fixed="STR" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "adxp.streetName")
public class AdxpStreetName
    extends ADXP
{
    @XmlMixed
    protected List<Serializable> content;

    public List<Serializable> getContent() {
        if (content == null) {
            content = new ArrayList<Serializable>();
        }
        return this.content;
    }
}
