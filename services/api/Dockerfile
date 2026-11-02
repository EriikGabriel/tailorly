# syntax=docker/dockerfile:1.7
# Imagem da API para produção no Render e desenvolvimento local.
# O build Maven roda na arquitetura de quem compila ($BUILDPLATFORM): o .jar é independente de
# arquitetura, então só o estágio final é montado (ou emulado) para a plataforma alvo.

FROM --platform=$BUILDPLATFORM eclipse-temurin:21-jdk AS build
WORKDIR /src
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -q dependency:go-offline
COPY src src
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -q -DskipTests package \
    && cp target/api-*.jar /src/app.jar

FROM eclipse-temurin:21-jre AS runtime
ARG TARGETARCH
# Versão fixa do Typst: atualizar aqui (e no CI) depois de conferir os templates.
ARG TYPST_VERSION=0.15.1

# Tesseract (OCR de páginas sem texto, por+eng) e Typst (compilação do PDF do currículo).
RUN apt-get update \
    && apt-get install -y --no-install-recommends \
       tesseract-ocr tesseract-ocr-por tesseract-ocr-eng curl xz-utils ca-certificates \
    && case "$TARGETARCH" in \
         arm64) typst_arch=aarch64-unknown-linux-musl ;; \
         amd64) typst_arch=x86_64-unknown-linux-musl ;; \
         *) echo "Unsupported architecture: $TARGETARCH" >&2; exit 1 ;; \
       esac \
    && curl -fsSL "https://github.com/typst/typst/releases/download/v${TYPST_VERSION}/typst-${typst_arch}.tar.xz" \
       | tar -xJ --strip-components=1 -C /usr/local/bin "typst-${typst_arch}/typst" \
    && typst --version && tesseract --version \
    && apt-get purge -y xz-utils && apt-get autoremove -y && rm -rf /var/lib/apt/lists/*

RUN useradd --system --uid 10001 --home /app tailorly
WORKDIR /app
COPY --from=build /src/app.jar app.jar
USER tailorly

# Padrões para máquinas com pouca RAM. O render.yaml sobrescreve JAVA_TOOL_OPTIONS para o free tier
# (512 MB), deixando folga para o Tesseract e o Typst, que rodam como processos filhos.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=70 -XX:+ExitOnOutOfMemoryError" \
    TAILORLY_TYPST_COMMAND=typst \
    TAILORLY_TESSERACT_COMMAND=tesseract \
    OMP_THREAD_LIMIT=1
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=3 \
  CMD curl -fsS "http://127.0.0.1:${PORT:-8080}/actuator/health/liveness" || exit 1
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
