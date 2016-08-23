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

package PDQSupplier.org.hl7.v3;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for MedicationObservationType.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="MedicationObservationType">
 *   &lt;restriction base="{urn:hl7-org:v3}cs">
 *     &lt;enumeration value="SPLCOATING"/>
 *     &lt;enumeration value="SPLCOLOR"/>
 *     &lt;enumeration value="SPLIMAGE"/>
 *     &lt;enumeration value="SPLIMPRINT"/>
 *     &lt;enumeration value="REP_HALF_LIFE"/>
 *     &lt;enumeration value="SPLSCORING"/>
 *     &lt;enumeration value="SPLSHAPE"/>
 *     &lt;enumeration value="SPLSIZE"/>
 *     &lt;enumeration value="SPLSYMBOL"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "MedicationObservationType")
@XmlEnum
public enum MedicationObservationType {

    SPLCOATING,
    SPLCOLOR,
    SPLIMAGE,
    SPLIMPRINT,
    REP_HALF_LIFE,
    SPLSCORING,
    SPLSHAPE,
    SPLSIZE,
    SPLSYMBOL;

    public String value() {
        return name();
    }

    public static MedicationObservationType fromValue(String v) {
        return valueOf(v);
    }

}
