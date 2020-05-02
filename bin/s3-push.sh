#!/usr/bin/env bash

if [[ ! -z "$STAGE" ]]; then
  STAGE="-${STAGE}"
fi

LOBASE=./resources/public
S3BASE=s3://spartan-harriers/qrv2${STAGE:-}
echo "S3BASE: ${S3BASE}"

#aws s3 sync ${LOBASE} ${S3BASE} \
#	--dryrun \
#	--exclude '*cljs-out*dev*' \
#	--exclude '*cljs-out*min*' \
#	--exclude "*.pem" \
#	--exclude "*test*" \
#	--exclude "*.py" \
#	--include '*dev-main.js' \
#	--include '*index.html'

echo synching cljs-out

aws s3 sync $LOBASE/cljs-out $S3BASE/cljs-out --exclude '*' --include "dev-main.js" --acl public-read

echo synching css
aws s3 sync $LOBASE/css $S3BASE/css --acl public-read

echo synching favicon
aws s3 sync $LOBASE/favicon $S3BASE/favicon --acl public-read

echo synching index.html
aws s3 sync $LOBASE $S3BASE --exclude '*' --include "index.html" --acl public-read

echo synching aws.edn
aws s3 sync $LOBASE $S3BASE --exclude '*' --include "aws.edn" --acl public-read

echo synching service-worker.js
aws s3 sync $LOBASE $S3BASE --exclude '*' --include 'service-worker.js' --acl public-read 

echo synching site.webmanifest
aws s3 sync $LOBASE $S3BASE --exclude '*' --include 'site.webmanifest' --acl public-read

