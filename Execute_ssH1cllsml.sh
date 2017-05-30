#!/bin/bash

buckets=$1
bucket=$2
local=$3

java -jar -Xmx3G RinSimExt.jar e ssh1cllsml $buckets $bucket $local
