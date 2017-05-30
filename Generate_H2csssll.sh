#!/bin/bash

buckets=$1
bucket=$2

java -jar RinSimExt.jar g H2csssll $buckets $bucket false 16 1800000 4 0.8 l
