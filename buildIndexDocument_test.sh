#!/bin/bash

# This is an example script to build an index document, given the snapshot_id and root_row_id.
# The snapshot_id and root_row_id are the first and second arguments, respectively, passed to the script.
# Below is an example command to use this script to generate documents for each root_row_id in a snapshot.
# poc index-snapshot 1000GenomesSnapshotA -t=sample_info -c=sample -i=testindex -b="sh buildIndexDocument_test.sh"

# current date as yyyy-mm-dd HH:MM:SS
now=$(date '+%Y-%m-%d %H:%M:%S')

echo "{\"date_created\":\"$now\", \"snapshot_id\":\"$1\", \"root_row_id\":\"$2\"}"

