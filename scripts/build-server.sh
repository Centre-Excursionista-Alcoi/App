#!/bin/sh

echo "Building Docker image for development..."

docker build -t arnyminerz/cea-app:development -f server/Dockerfile . || exit 1

echo "Build complete."
echo "Push to Docker Hub with: docker push arnyminerz/cea-app:development"
echo "Run with: docker run -p 3000:3000 arnyminerz/cea-app:development"
