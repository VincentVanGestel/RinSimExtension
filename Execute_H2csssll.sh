#!/bin/bash

buckets=$1
bucket=$2

java -jar  RinSimExt.jar e h2csssll $buckets $bucket local c
