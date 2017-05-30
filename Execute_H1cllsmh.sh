#!/bin/bash

buckets=$1
bucket=$2

java -jar  RinSimExt.jar e h1cllsmh $buckets $bucket local c
