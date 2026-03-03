FROM eclipse-temurin:17-jdk-jammy

WORKDIR /app

COPY run.sh /app/run.sh

RUN chmod +x /app/run.sh

CMD ["/app/run.sh"]