FROM jenkins/jenkins:lts

USER root

# 컨테이너 내에서 Docker 설치
RUN curl -fsSL https://get.docker.com -o get-docker.sh
RUN sh get-docker.sh

# 호스트의 docker 그룹 GID (예: 122)를 컨테이너 내 docker 그룹에 적용
ARG DOCKER_GID=122

# 이미 docker 그룹이 존재할 수 있으므로 GID를 변경하거나 재생성하지 않고 GID만 수정
RUN groupmod -g ${DOCKER_GID} docker \
    && usermod -aG docker jenkins

USER jenkins