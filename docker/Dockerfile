FROM jetty:9.4.55-jre11-alpine

WORKDIR /var/lib

USER root
RUN chown -R jetty:jetty /var/lib
USER jetty

EXPOSE 8080
VOLUME /clearth/persistentFiles/

COPY --chown=jetty cfg cfg/
COPY --chown=jetty jetty/webapps jetty/webapps/
COPY --chown=jetty docker/log.properties cfg/log.properties
COPY --chown=jetty docker/clearth.cfg cfg/clearth.cfg

WORKDIR /var/lib/jetty
RUN java -jar "/usr/local/jetty/start.jar" --add-to-start=server,http,deploy,apache-jsp,websocket
CMD ["sh", "-c", "cp -u -p ../cfg/users.xml /clearth/persistentFiles/users.xml; exec java -jar /usr/local/jetty/start.jar"]