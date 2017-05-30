#!/bin/bash

buckets=$1
bucket=$2

java -jar  RinSimExt.jar e h1cllsml $buckets $bucket local c
