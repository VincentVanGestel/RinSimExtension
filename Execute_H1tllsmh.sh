#!/bin/bash

buckets=$1
bucket=$2

java -jar -Xmx3G RinSimExt.jar e h1tllsmh $buckets $bucket local t
