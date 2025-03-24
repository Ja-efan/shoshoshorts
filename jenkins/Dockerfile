FROM jenkins/jenkins:lts

USER root

ARG DOCKER_GID=122

RUN apt-get update && \
    apt-get install -y \
        apt-transport-https \
        ca-certificates \
        curl \
        gnupg2 \
        software-properties-common && \
    curl -fsSL https://download.docker.com/linux/debian/gpg | gpg --dearmor -o /usr/share/keyrings/docker-archive-keyring.gpg && \
    echo "deb [arch=amd64 signed-by=/usr/share/keyrings/docker-archive-keyring.gpg] https://download.docker.com/linux/debian bookworm stable" > /etc/apt/sources.list.d/docker.list && \
    apt-get update && \
    apt-get install -y docker-ce-cli && \
    # GID 999이 이미 존재하면 해당 그룹명을 가져오고, 없으면 docker 그룹 생성
    if getent group ${DOCKER_GID} > /dev/null; then \
        EXISTING_GROUP=$(getent group ${DOCKER_GID} | cut -d: -f1); \
        echo "GID ${DOCKER_GID} already used by group '${EXISTING_GROUP}'"; \
    else \
        groupadd -g ${DOCKER_GID} docker; \
        EXISTING_GROUP=docker; \
    fi && \
    # jenkins를 해당 그룹에 추가
    usermod -aG ${EXISTING_GROUP:-docker} jenkins

USER jenkins