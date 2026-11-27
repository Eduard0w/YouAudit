# Imagem do YouAudit para o servidor. Duas etapas:
#  1) "build": compila o .jar com Maven (só existe durante a construção);
#  2) "final": só o Java 21 (JRE) + o .jar + os dados SIMULADOS. Imagem menor e sem o Maven.
# As imagens eclipse-temurin existem para x86 e ARM: serve na VM AMD e na Ampere da Oracle.

# ---------- Etapa 1: compilar ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /fonte
# Copia o pom.xml primeiro e baixa as dependências: se só o código mudar, o Docker reaproveita esta camada.
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
# Os testes rodam na sua máquina (mvn test) antes de subir; aqui só empacota.
RUN mvn -q -B -DskipTests package

# ---------- Etapa 2: rodar ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

# Usuário sem privilégios: se alguém explorar a aplicação, não vira root no contêiner.
RUN useradd --system --uid 10001 youaudit \
    && mkdir -p /dados/db /dados/snapshots \
    && chown -R youaudit:youaudit /dados

COPY --from=build /fonte/target/youaudit-*.jar /app/youaudit.jar
# SÓ os dados fictícios. data/real/ NUNCA entra na imagem (também bloqueado no .dockerignore).
COPY data/simulado/produtos.json /app/data/simulado/produtos.json

USER youaudit
EXPOSE 8080
# MaxRAMPercentage: o Java usa até 75% da memória que o contêiner tiver (importante na VM de 1 GB).
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"
ENTRYPOINT ["java", "-jar", "/app/youaudit.jar"]
