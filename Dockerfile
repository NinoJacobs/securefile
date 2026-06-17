FROM postgres:16

ENV POSTGRES_DB=securefile
ENV POSTGRES_USER=admin
ENV POSTGRES_PASSWORD=admin

COPY src/main/resources/db/data/001_create_all_tables.sql /docker-entrypoint-initdb.d/001_create_all_tables.sql
