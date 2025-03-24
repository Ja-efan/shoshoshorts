FROM jenkins/jenkins:lts

USER root

#컨테이너 내에서 필요한 도커 설치
RUN curl -fsSL https://get.docker.com -o get-docker.sh
RUN sh get-docker.sh

#설치 후 docker 그룹의 jenkins 계정 생성 후 해당 계정으로 변경
RUN groupadd -f docker
RUN usermod -aG docker jenkins
USER jenkins