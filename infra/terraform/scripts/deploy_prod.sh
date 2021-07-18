#!/bin/sh

ENV=prod
VERSION=production
IMAGE_NAME=216862985054.dkr.ecr.eu-central-1.amazonaws.com/sama-service

eval "$(aws ecr get-login --region eu-central-1 --no-include-email)"
docker run -d \
      --name sama-service \
      -e X_JAVA_OPTS="-Dspring.profiles.active=$ENV" \
      -p 3000:3000 \
      $IMAGE_NAME:$VERSION
asd