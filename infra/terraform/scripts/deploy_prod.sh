#!/bin/sh

VERSION=production

DOCKER_HUB=216862985054.dkr.ecr.eu-central-1.amazonaws.com/
SERVICE_IMAGE_NAME=sama-service
WEBSERVER_IMAGE_NAME=sama-webserver

eval "$(aws ecr get-login --region eu-central-1 --no-include-email)"

docker network create sama
docker run -d \
      --name sama-service \
      --network sama \
      -e X_JAVA_OPTS="-Dspring.profiles.active=$ENV" \
      -p 3001:3000 \
      $DOCKER_HUB$SERVICE_IMAGE_NAME:$VERSION

docker run -d \
      --name sama-webserver \
      --network sama \
      -p 3000:8080 \
      $DOCKER_HUB$WEBSERVER_IMAGE_NAME:$VERSION
