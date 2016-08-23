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
 * <p>Java class for Grandparent.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="Grandparent">
 *   &lt;restriction base="{urn:hl7-org:v3}cs">
 *     &lt;enumeration value="GRPRN"/>
 *     &lt;enumeration value="GRFTH"/>
 *     &lt;enumeration value="GRMTH"/>
 *     &lt;enumeration value="MGRFTH"/>
 *     &lt;enumeration value="MGRMTH"/>
 *     &lt;enumeration value="MGRPRN"/>
 *     &lt;enumeration value="PGRFTH"/>
 *     &lt;enumeration value="PGRMTH"/>
 *     &lt;enumeration value="PGRPRN"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "Grandparent")
@XmlEnum
public enum Grandparent {

    GRPRN,
    GRFTH,
    GRMTH,
    MGRFTH,
    MGRMTH,
    MGRPRN,
    PGRFTH,
    PGRMTH,
    PGRPRN;

    public String value() {
        return name();
    }

    public static Grandparent fromValue(String v) {
        return valueOf(v);
    }

}
