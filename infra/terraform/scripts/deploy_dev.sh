#!/bin/sh

ENV=dev
VERSION=production
IMAGE_NAME=216862985054.dkr.ecr.eu-central-1.amazonaws.com/sama-service
INSTANCE_ID=$(ec2metadata --instance-id)

eval "$(aws ecr get-login --region eu-central-1 --no-include-email)"
docker run -d \
      --name sama-service \
      -e X_JAVA_OPTS="-Dspring.profiles.active=$ENV" \
      -p 3000:3000 \
      --log-driver=awslogs \
      --log-opt awslogs-group=$ENV \
      --log-opt awslogs-stream=sama-service-$VERSION-"$INSTANCE_ID" \
      --log-opt awslogs-create-group=true \
      --log-opt awslogs-datetime-format='%Y-%m-%d %H:%M:%S%L' \
      $IMAGE_NAME:$VERSION
