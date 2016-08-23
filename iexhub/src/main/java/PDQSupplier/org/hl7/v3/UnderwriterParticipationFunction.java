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
 * <p>Java class for UnderwriterParticipationFunction.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="UnderwriterParticipationFunction">
 *   &lt;restriction base="{urn:hl7-org:v3}cs">
 *     &lt;enumeration value="PAYORCNTR"/>
 *     &lt;enumeration value="REINS"/>
 *     &lt;enumeration value="RETROCES"/>
 *     &lt;enumeration value="SUBCTRT"/>
 *     &lt;enumeration value="UNDERWRTNG"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "UnderwriterParticipationFunction")
@XmlEnum
public enum UnderwriterParticipationFunction {

    PAYORCNTR,
    REINS,
    RETROCES,
    SUBCTRT,
    UNDERWRTNG;

    public String value() {
        return name();
    }

    public static UnderwriterParticipationFunction fromValue(String v) {
        return valueOf(v);
    }

}
