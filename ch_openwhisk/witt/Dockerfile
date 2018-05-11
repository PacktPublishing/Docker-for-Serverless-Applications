FROM node:8-alpine

RUN npm install -g witt

EXPOSE 8080
ENV NODE_TLS_REJECT_UNAUTHORIZED 0
CMD ["node", "--harmony", "/usr/local/lib/node_modules/witt/witt.js"]
