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

## External Configuration

There are some additional system properties that can be used to configure the location of external configuration. These properties can be added in `$TOMCAT_HOME/conf/catalina.properties` or passed with `CATALINA_OPTS` environment variable in Docker environment.

+ `iexhub.logging.file`: The existing `log4j.properties` in classpath depends on this variable, so it **MUST** be provided for saving the log file somewhere on the file system. If a `log4j.configuration` is provided and the `log4j.properties` file that is loaded from this location has an absolute log file location specified in `log4j.appender.LOG_FILE.File` property, `iexhub.logging.file` property is omitted.
	+ *Example using `catalina.properties`:* `iexhub.logging.file=/java/iexhub/logs/InfoExchangeHub.log`
	+ *Example using Docker:* `docker run -d -e CATALINA_OPTS="-Diexhub.logging.file=/java/iexhub/logs/InfoExchangeHub.log" bhits/iexhub:latest`
+ `iexhub.config.location`: The folder location for the external configuration files. Defaults to `/java/iexhub/config`.
	+ *Example using `catalina.properties`:* `iexhub.config.location=/path/to/iexhub/config/folder`
	+ *Example using Docker:* `docker run -d -e CATALINA_OPTS="-Diexhub.config.location=/path/to/iexhub/config/folder" bhits/iexhub:latest`
+ `iexhub.config.filename`: The file name for the properties file that will be loaded from `iexhub.config.location`. Defaults to `IExHub.properties`.
	+ *Example using `catalina.properties`:* `iexhub.config.filename=Config.properties`
	+ *Example using Docker:* `docker run -d -e CATALINA_OPTS="-Diexhub.config.filename=Config.properties" bhits/iexhub:latest`