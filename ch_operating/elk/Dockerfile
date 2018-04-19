FROM sebp/elk

WORKDIR ${LOGSTASH_HOME}

COPY ./logstash-input-syslog-3.2.4.gem /tmp/logstash-input-syslog-3.2.4.gem
RUN gosu logstash bin/logstash-plugin install --no-verify /tmp/logstash-input-syslog-3.2.4.gem

