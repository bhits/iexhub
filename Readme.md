# ReadMe for Information Exchange Hub (IexHub)



#Release Notes

##HIMSS 2016 Demo
 Release for HIMSS 2016 Demo
##NA-Connectathon-2016 
North American Connectathon

#Overview
Information Exchange Hub consists of connectors, services, and transformation components intended to allow applications to interoperate with standards-based Health Information Exchange (HIE) organizations.

# VM Runtime Arguments

In order to configure commons logging with Tomcat use the following runtime arguments:

## To set up log4j.properties
-Dlog4j.configuration={path to file} if the file is not available in WEB-INF/classes
-Dlog4j.configuration=file:C:/[git_checkout_folder]/iexhub/iexhub/src/main/esourceslog4j.properties if the properties file is elsewhere on the filesystem
## To specify the logger (log4j):
-Dorg.apache.commons.logging.Log=org.apache.commons.logging.impl.Log4JLogger 
## To set "debug" level: 
-Dlog4j.debug

