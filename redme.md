docker run -itd -p 6379:6379 --name redis_container -d redis redis-server --requirepass Eason9379

docker run -itd -p 8088:8080 --add-host=host.docker.internal:host-gateway --name pushservice_container pushservice

docker build -t pushservice . --platform linux/amd64
docker save -o pushservice.tar pushservice
