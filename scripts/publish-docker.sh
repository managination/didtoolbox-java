#!/bin/bash

# Docker Hub Publish Script for did-server (numaquest)
# Publishes to: micharoon/numaquest:0.0.1

set -ek

# Configuration
DOCKER_USERNAME="micharoon"
IMAGE_NAME="numaquest"
VERSION="0.0.1"
FULL_IMAGE_NAME="${DOCKER_USERNAME}/${IMAGE_NAME}:${VERSION}"

echo "=========================================="
echo "Publishing Docker Image to Docker Hub"
echo "=========================================="
echo ""
echo "Image: ${FULL_IMAGE_NAME}"
echo ""

# Check if Docker is installed
if ! command -v docker &> /dev/null; then
    echo "ERROR: Docker is not installed or not in PATH"
    exit 1
fi

# Check if user is logged into Docker Hub
echo "Checking Docker Hub login status..."
if ! docker info &> /dev/null; then
    echo "ERROR: Docker daemon is not running"
    exit 1
fi

if ! docker system info 2>/dev/null | grep -q "Username"; then
    echo "WARNING: You may not be logged into Docker Hub"
    echo "Please login first with: docker login"
    echo ""
    read -p "Do you want to login now? (y/n) " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        docker login
    else
        echo "Exiting..."
        exit 1
    fi
fi

# Navigate to project root (script is in scripts/ directory)
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
cd "$PROJECT_ROOT"

echo ""
echo "Building Docker image..."
echo "Build context: $(pwd)"
echo ""

# Build the Docker image
docker build -t "${FULL_IMAGE_NAME}" -f Dockerfile .

if [ $? -ne 0 ]; then
    echo "ERROR: Docker build failed"
    exit 1
fi

echo ""
echo "=========================================="
echo "Build successful!"
echo "=========================================="
echo ""
echo "Image built: ${FULL_IMAGE_NAME}"
echo ""

# Push to Docker Hub
echo "Pushing image to Docker Hub..."
echo ""
docker push "${FULL_IMAGE_NAME}"

if [ $? -ne 0 ]; then
    echo "ERROR: Docker push failed"
    echo ""
    echo "Possible reasons:"
    echo "  - Not logged into Docker Hub (run: docker login)"
    echo "  - Network connectivity issues"
    echo "  - Repository does not exist on Docker Hub"
    echo ""
    exit 1
fi

echo ""
echo "=========================================="
echo "Successfully published!"
echo "=========================================="
echo ""
echo "Image URL: docker.io/${FULL_IMAGE_NAME}"
echo ""
echo "To pull this image:"
echo "  docker pull ${FULL_IMAGE_NAME}"
echo ""
echo "To run this image:"
echo "  docker run -p 8080:8080 ${FULL_IMAGE_NAME}"
echo ""
