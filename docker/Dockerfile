FROM jetty:9.4.50-jre11-alpine

WORKDIR /usr/local

USER root
RUN chown -R jetty:jetty /usr/local
USER jetty

EXPOSE 8080
VOLUME /clearth/persistentFiles/

COPY --chown=jetty cfg cfg/
COPY --chown=jetty jetty/webapps jetty/webapps/
COPY --chown=jetty docker/log.properties cfg/log.properties
COPY --chown=jetty docker/clearth.cfg cfg/clearth.cfg
COPY --chown=jetty docker/jetty.xml jetty/etc/jetty.xml
COPY --chown=jetty docker/start.ini jetty/start.ini

WORKDIR /usr/local/jetty
CMD ["sh", "-c", "cp -u -p ../cfg/users.xml /clearth/persistentFiles/users.xml; exec java -jar start.jar"]