#!/bin/sh
set -eu

BUCKET_NAME="${SECUREFILE_S3_BUCKET:-securefile-statements}"

awslocal s3api create-bucket --bucket "${BUCKET_NAME}" >/dev/null 2>&1 || true

echo "LocalStack S3 bucket ready: ${BUCKET_NAME}"
