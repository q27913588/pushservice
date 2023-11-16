#!/bin/bash

which jq
if [ $? -ne 0 ] 
then
    echo "jq not found" >&2 
    exit $?
fi


## create MR to master 

NEW_MR_DATA='{
    "source_branch": "release/'$RELEASE_VERSION'",
    "target_branch": "master",
    "title": "PROD Released"
}'

curl -k --request POST \
    --header "PRIVATE-TOKEN: ${CI_TOKEN}" \
    --header "Content-Type: application/json" \
    --data "$NEW_MR_DATA" \
    "${CI_GITLAB}/api/v4/projects/${CI_PROJECT_ID}/merge_requests" \
    -o mr.json

mr_iid=`jq '.["iid"]' mr.json`
if [[ "$mr_iid" = "null" ]]
then
    echo "create Merge Request fail: `jq '.["message"]' mr.json`" 
    exit -1
fi


## checking merge status

echo "sleep 60sec for GitLab checking merge request..."
sleep 60

curl -k --request GET \
    --header "PRIVATE-TOKEN: ${CI_TOKEN}" \
    "${CI_GITLAB}/api/v4/projects/${CI_PROJECT_ID}/merge_requests/${mr_iid}" \
    -o mr.json

mr_status=`jq '.["merge_status"]' mr.json`
echo $mr_status
if [[ "$mr_status" != '"can_be_merged"' ]]
then
    echo "check merge_status fail: ${mr_status}"
    exit -1
fi


## approval MR for master

MERGE_DATA='{
    "merge_when_pipeline_succeeds": false,
    "should_remove_source_branch": true
}'

curl -k --request PUT \
    --header "PRIVATE-TOKEN: ${CI_TOKEN}" \
    "${CI_GITLAB}/api/v4/projects/${CI_PROJECT_ID}/merge_requests/${mr_iid}/merge" \
    -o mr_iid.json

merge_commit_sha=`jq '.["merge_commit_sha"]' mr_iid.json`
if [[ "$merge_commit_sha" = "null" ]]
then
    echo "merge to main/master fail: `jq '.["message"]' mr_iid.json`" 

    CLOSE_MR_DATA='{
        "state_event": "close"
    }'
    curl -k --request PUT \
        --header "PRIVATE-TOKEN: ${CI_TOKEN}" \
        --header "Content-Type: application/json" \
        --data "$CLOSE_MR_DATA" \
        "${CI_GITLAB}/api/v4/projects/${CI_PROJECT_ID}/merge_requests/${mr_iid}" 

    exit -1
fi


## tag version

curl -k --request POST \
    --header "PRIVATE-TOKEN: ${CI_TOKEN}" \
    "${CI_GITLAB}/api/v4/projects/${CI_PROJECT_ID}/repository/tags?tag_name=${RELEASE_VERSION}&ref=master" \
    -o tag.json

tag_name=`jq '.["name"]' tag.json`
if [[ "$merge_commit_sha" = "null" ]]
then
    echo "tag ${RELEASE_VERSION} fail: `jq '.["message"]' tag.json`" 
    exit -1
fi


## create MR from master to develop

DEV_MR_DATA='{
    "source_branch": "master",
    "target_branch": "develop",
    "title": "PROD Released"
}'

curl -k --request POST \
    --header "PRIVATE-TOKEN: ${CI_TOKEN}" \
    --header "Content-Type: application/json" \
    --data "$DEV_MR_DATA" \
    "${CI_GITLAB}/api/v4/projects/${CI_PROJECT_ID}/merge_requests"


## create MR from master to QAS

QAS_MR_DATA='{
    "source_branch": "master",
    "target_branch": "QAS",
    "title": "PROD Released"
}'

curl -k --request POST \
    --header "PRIVATE-TOKEN: ${CI_TOKEN}" \
    --header "Content-Type: application/json" \
    --data "$QAS_MR_DATA" \
    "${CI_GITLAB}/api/v4/projects/${CI_PROJECT_ID}/merge_requests"
