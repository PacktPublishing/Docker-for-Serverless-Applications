FROM ubuntu:16.04

RUN  apt-get update && apt-get install -y sudo git tzdata

WORKDIR /root
RUN  git clone --depth=1 https://github.com/apache/incubator-openwhisk.git openwhisk

WORKDIR /root/openwhisk
RUN cd tools/ubuntu-setup && ./all.sh
