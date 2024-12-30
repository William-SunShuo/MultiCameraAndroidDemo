#!/bin/bash

# Clone the repository without checking out files
git clone --no-checkout git@github.com:XbotTech/MultiCamera-SDK.git sdk

# Navigate into the cloned repository directory
cd sdk || exit 1

# Initialize sparse checkout
git sparse-checkout init

# Set the paths to include in the sparse checkout
#git sparse-checkout set /sdk/* /wrapper/*
echo "/src/*" > .git/info/sparse-checkout

# Check out the main branch
git checkout monitor
