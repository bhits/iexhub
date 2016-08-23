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

package org.hl7.v3;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for ActNonObservationIndicationCode.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="ActNonObservationIndicationCode">
 *   &lt;restriction base="{urn:hl7-org:v3}cs">
 *     &lt;enumeration value="IND02"/>
 *     &lt;enumeration value="IND01"/>
 *     &lt;enumeration value="IND05"/>
 *     &lt;enumeration value="IND03"/>
 *     &lt;enumeration value="IND04"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "ActNonObservationIndicationCode")
@XmlEnum
public enum ActNonObservationIndicationCode {

    @XmlEnumValue("IND02")
    IND_02("IND02"),
    @XmlEnumValue("IND01")
    IND_01("IND01"),
    @XmlEnumValue("IND05")
    IND_05("IND05"),
    @XmlEnumValue("IND03")
    IND_03("IND03"),
    @XmlEnumValue("IND04")
    IND_04("IND04");
    private final String value;

    ActNonObservationIndicationCode(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static ActNonObservationIndicationCode fromValue(String v) {
        for (ActNonObservationIndicationCode c: ActNonObservationIndicationCode.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
