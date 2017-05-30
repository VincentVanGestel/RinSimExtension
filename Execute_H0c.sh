#!/bin/bash

buckets=$1
bucket=$2

java -jar  RinSimExt.jar e h0c $buckets $bucket local c
