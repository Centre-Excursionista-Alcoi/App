#!/bin/sh

echo "Building Docker image for development..."

docker buildx build \
  --network=host \
  --file server/Dockerfile \
  --tag arnyminerz/cea-app:development \
  . \
  --push

echo "Build complete."
echo "Push to Docker Hub with: docker push arnyminerz/cea-app:development"
echo "Run with: docker run -p 3000:3000 arnyminerz/cea-app:development"
