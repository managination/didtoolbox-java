#!/bin/bash

# Docker Hub Publish Script for did-server (numaquest)
# Publishes multi-architecture images to Docker Hub

set -e

# Configuration
DOCKER_USERNAME="micharoon"
IMAGE_NAME="webvh-store"
VERSION="latest"
FULL_IMAGE_NAME="${DOCKER_USERNAME}/${IMAGE_NAME}:${VERSION}"

# Target platforms for multi-arch build
PLATFORMS="linux/amd64,linux/arm64"

echo "=========================================="
echo "Publishing Multi-Arch Docker Image"
echo "=========================================="
echo ""
echo "Image: ${FULL_IMAGE_NAME}"
echo "Platforms: ${PLATFORMS}"
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

# Setup buildx for multi-arch builds
echo ""
echo "Setting up Docker buildx for multi-architecture builds..."

# Create a new builder instance if it doesn't exist
if ! docker buildx ls | grep -q "multiarch-builder"; then
    echo "Creating new buildx builder: multiarch-builder"
    docker buildx create --name multiarch-builder --driver docker-container --bootstrap
else
    echo "Using existing buildx builder: multiarch-builder"
fi

# Switch to the builder
docker buildx use multiarch-builder

# Inspect the builder to ensure it's ready
docker buildx inspect --bootstrap

echo ""
echo "Builder ready for platforms: ${PLATFORMS}"
echo ""

# Navigate to project root (script is in scripts/ directory)
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
cd "$PROJECT_ROOT"

echo "Building and pushing multi-architecture Docker image..."
echo "Build context: $(pwd)"
echo ""

# Build and push the multi-arch image
docker buildx build \
    --platform "${PLATFORMS}" \
    --tag "${FULL_IMAGE_NAME}" \
    --file Dockerfile \
    --push \
    .

if [ $? -ne 0 ]; then
    echo "ERROR: Docker buildx build/push failed"
    echo ""
    echo "Possible reasons:"
    echo "  - Not logged into Docker Hub (run: docker login)"
    echo "  - Network connectivity issues"
    echo "  - Repository does not exist on Docker Hub"
    echo "  - Buildx builder not properly configured"
    echo ""
    exit 1
fi

echo ""
echo "=========================================="
echo "Successfully published multi-arch image!"
echo "=========================================="
echo ""
echo "Image URL: docker.io/${FULL_IMAGE_NAME}"
echo "Platforms: ${PLATFORMS}"
echo ""
echo "To verify the multi-arch manifest:"
echo "  docker buildx imagetools inspect ${FULL_IMAGE_NAME}"
echo ""
echo "To pull this image (Docker will auto-select correct platform):"
echo "  docker pull ${FULL_IMAGE_NAME}"
echo ""
echo "To run this image:"
echo "  docker run -p 8080:8080 ${FULL_IMAGE_NAME}"
echo ""
echo "To build for a specific platform locally:"
echo "  docker buildx build --platform linux/amd64 -t ${IMAGE_NAME} ."
echo "  docker buildx build --platform linux/arm64 -t ${IMAGE_NAME} ."
echo ""
