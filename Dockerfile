FROM openjdk:17.0.2-jdk-slim
RUN apt-get update && apt-get install -y --no-install-recommends python3 python3-pip
RUN pip3 install pymysql pandas openpyxl python-dotenv
WORKDIR /app

# Elasticsearch 인증서 포함
#COPY ./certs/ca.crt /app/certs/ca.crt

# JDK cacerts에 Elasticsearch 인증서 등록
#RUN keytool -importcert -trustcacerts -alias elasticsearch \
#    -file /app/certs/ca.crt \
#    -keystore $JAVA_HOME/lib/security/cacerts \
#    -storepass changeit -noprompt

COPY entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

COPY build/libs/codin-lecture-api-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["/entrypoint.sh"]
