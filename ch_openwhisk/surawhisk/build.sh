grails prod war

rm docker/*.war
cp build/libs/surawhisk*.war docker/surawhisk.war
(cd docker && docker build -t surawhisk/ui .)

docker push surawhisk/ui
