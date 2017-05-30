#!/bin/bash

buckets=$1
bucket=$2

java -jar  RinSimExt.jar e h2cssshl $buckets $bucket local c
