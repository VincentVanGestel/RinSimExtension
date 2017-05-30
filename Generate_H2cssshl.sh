#!/bin/bash

buckets=$1
bucket=$2

java -jar RinSimExt.jar g H2cssshl $buckets $bucket false 16 1800000 4 0.2 l
