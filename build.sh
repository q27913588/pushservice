mvn package -Dmaven.test.skip=true
docker build -t pushservice . --platform linux/amd64
docker save -o pushservice.tar pushservice