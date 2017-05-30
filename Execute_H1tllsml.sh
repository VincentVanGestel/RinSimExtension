#!/bin/bash

buckets=$1
bucket=$2

java -jar -Xmx3G RinSimExt.jar e h1tllsml $buckets $bucket local t
