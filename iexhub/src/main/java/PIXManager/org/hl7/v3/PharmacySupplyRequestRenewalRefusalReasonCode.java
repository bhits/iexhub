/*******************************************************************************
 * Copyright (c) 2015, 2016 Substance Abuse and Mental Health Services Administration (SAMHSA)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Eversolve, LLC - initial IExHub implementation for Health Information Exchange (HIE) integration
 *     Anthony Sute, Ioana Singureanu
 *******************************************************************************/

package PIXManager.org.hl7.v3;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for PharmacySupplyRequestRenewalRefusalReasonCode.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="PharmacySupplyRequestRenewalRefusalReasonCode">
 *   &lt;restriction base="{urn:hl7-org:v3}cs">
 *     &lt;enumeration value="FAMPHYS"/>
 *     &lt;enumeration value="ONHOLD"/>
 *     &lt;enumeration value="MODIFY"/>
 *     &lt;enumeration value="ALREADYRX"/>
 *     &lt;enumeration value="NEEDAPMT"/>
 *     &lt;enumeration value="NOTPAT"/>
 *     &lt;enumeration value="NOTAVAIL"/>
 *     &lt;enumeration value="DISCONT"/>
 *     &lt;enumeration value="TOOEARLY"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "PharmacySupplyRequestRenewalRefusalReasonCode")
@XmlEnum
public enum PharmacySupplyRequestRenewalRefusalReasonCode {

    FAMPHYS,
    ONHOLD,
    MODIFY,
    ALREADYRX,
    NEEDAPMT,
    NOTPAT,
    NOTAVAIL,
    DISCONT,
    TOOEARLY;

    public String value() {
        return name();
    }

    public static PharmacySupplyRequestRenewalRefusalReasonCode fromValue(String v) {
        return valueOf(v);
    }

}
