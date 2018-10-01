FROM clojure as clojure

COPY spacedoc /usr/src/app

RUN cd /usr/src/app && lein uberjar


FROM ubuntu as graalvm

ENV GRAALVM_V=1.0.0-rc6

WORKDIR /tmp

COPY --from=clojure /usr/src/app/target/uberjar/spacedoc.jar ./

RUN apt-get update && apt-get install -y wget gcc libz-dev

RUN wget --quiet https://github.com/oracle/graal/releases/download/vm-${GRAALVM_V}/graalvm-ce-${GRAALVM_V}-linux-amd64.tar.gz \
    && tar -xvzf graalvm-ce-${GRAALVM_V}-linux-amd64.tar.gz

RUN graalvm-ce-${GRAALVM_V}/bin/native-image \
    --no-server \
    -H:+ReportUnsupportedElementsAtRuntime \
    -jar /tmp/spacedoc.jar


FROM jare/emacs

COPY --from=graalvm /tmp/spacedoc /usr/local/bin

COPY . /opt/spacetools

WORKDIR  /opt/spacetools

RUN chmod 775 /usr/local/bin/spacedoc \
              ./run \
              ./spacedoc/prefmt/prefmt.el \
              ./spacedoc/sdnize/sdnize.el \
    && chmod 777 ./spacedoc/docfmt ./spacedoc/sdnize

ENTRYPOINT ["/opt/spacetools/run"]

CMD ["--help"]
