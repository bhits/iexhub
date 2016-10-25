FROM tomcat:8-jre8
# Copy war to container, argument is defined in pom.xml
ARG finalName
ADD $finalName /usr/local/tomcat/webapps/iexhub.war