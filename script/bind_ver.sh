#!/bin/bash

## download config.yaml from infrastructure git 

curl -k --header "PRIVATE-TOKEN: ${INFRA_TOKEN}" \
    "${CI_GITLAB}/api/v4/projects/${INFRA_PROJECT_ID}/repository/files/vars%2Fpushservice%2Fconfig.yaml/raw?ref=master" \
    -o config.yaml


## patch VERSION & push to infrastructure git

awk -v new_ver="$BUILD_VERSION" -f "$PACK_ENV.awk" config.yaml | tee new_config.yaml

content=$(base64 -i new_config.yaml)

CURL_DATA='{
    "branch": "master",
    "author_email": "cicd@nanshan.com.tw",
    "author_name": "CICD",
    "content": "'$content'",
    "encoding": "base64",
    "commit_message": "pushservice: '$PACK_ENV':'$BUILD_VERSION'"
}'

curl -k --request PUT --header 'PRIVATE-TOKEN: '$INFRA_TOKEN \
     --header "Content-Type: application/json" \
     --data "$CURL_DATA" \
    "${CI_GITLAB}/api/v4/projects/${INFRA_PROJECT_ID}/repository/files/vars%2Fpushservice%2Fconfig.yaml"


## copy to deploy_%env folder

# cp new_config.yaml /nfs/src/${PACK_FOLDER}/vars/nasa/config.yaml
