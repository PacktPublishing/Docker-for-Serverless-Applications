FROM ubuntu
RUN apt-get update && apt-get install -y nginx
EXPOSE 80
ENTRYPOINT ["nginx", "-g", "daemon off;"]
