FROM tomcat:8-jre8
ENV JAVA_CACERTS /usr/lib/jvm/java-8-openjdk-amd64/jre/lib/security
ADD azhecTest.cer /usr/lib/jvm/java-8-openjdk-amd64/jre/lib/security
RUN keytool -import -noprompt -alias azhecTest -keystore  ${JAVA_CACERTS}/cacerts -storepass changeit -file ${JAVA_CACERTS}/azhecTest.cer
ADD server.xml /usr/local/tomcat/conf/
ARG finalName
ADD $finalName /usr/local/tomcat/webapps/iexhub.war