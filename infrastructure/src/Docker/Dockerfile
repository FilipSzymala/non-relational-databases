FROM mongo:9.0.4

RUN mkdir -p /etc/mongo && \
    chown mongodb:mongodb /etc/mongo && \
    chmod 755 /etc/mongo

COPY --chown=mongodb:mongodb --chmod=400 \
    ./config/keyFile /etc/mongo/keyFile

COPY --chown=mongodb:mongodb --chmod=600 \
    ./config/mongod.conf /etc/mongod.conf