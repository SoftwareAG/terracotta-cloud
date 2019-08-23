FROM openjdk:8u181-jdk AS builder
ADD https://github.com/SoftwareAG/terracotta-db-samples/archive/master.zip /root/master.zip
RUN mkdir -p /terracotta-db-samples/websessions-cart-example/ \
    &&  unzip /root/master.zip 'terracotta-db-samples-master/websessions-cart-example/*' -d /

WORKDIR /terracotta-db-samples-master/websessions-cart-example/
RUN ./mvnw package

FROM tomcat:8.5.35-jre8
# add webapp previously built
COPY --from=builder  /terracotta-db-samples-master/websessions-cart-example/target/*.war /usr/local/tomcat/webapps/cart.war
# add the terracotta web sessions jars
COPY websessions/terracotta-websessions-*-tomcat85.jar /usr/local/tomcat/lib/
COPY client/lib/terracotta-common-client-*.jar /usr/local/tomcat/lib/
COPY client/logging/slf4j-api-*.jar /usr/local/tomcat/lib/
COPY client/store/terracotta-store-client-*.jar /usr/local/tomcat/lib/
ADD http://central.maven.org/maven2/org/slf4j/slf4j-jdk14/1.7.25/slf4j-jdk14-1.7.25.jar /usr/local/tomcat/lib/

# add conf
COPY docker/images/websessions-cart-example/context.xml /usr/local/tomcat/conf/context.xml

ENV JAVA_OPTS \
"-Dterracotta.websessions.store.tcstore.uri=terracotta://localhost:9410/my-company-websessions \
 -Dterracotta.websessions.store.tcstore.offheapResource=offheap-1 \
 -Dterracotta.websessions.store.tcstore.diskResource=dataroot-1"

CMD ["catalina.sh", "run"]
