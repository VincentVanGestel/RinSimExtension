#!/bin/bash

buckets=$1
bucket=$2

java -jar -Xmx3G RinSimExt.jar e h2tsssll $buckets $bucket local t
