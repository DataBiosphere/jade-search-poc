#!/bin/bash

# current date as yyyy-mm-dd HH:MM:SS
now=$(date '+%Y-%m-%d %H:%M:%S')

echo "{\"date_created\":\"$now\", \"snapshot_id\":\"$1\", \"root_row_id\":\"$2\"}"

