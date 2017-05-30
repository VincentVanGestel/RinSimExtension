#!/bin/bash

buckets=$1
bucket=$2

java -jar RinSimExt.jar g H1tllsml $buckets $bucket true 32 7200000 4 0.5 l
