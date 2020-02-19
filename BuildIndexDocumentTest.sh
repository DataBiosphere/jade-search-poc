#!/bin/bash

# This is an example script to build an index document, given the snapshot_id and root_row_id.
# The arguments passed to the script are, in order: root_row_id, snapshot_id, snapshot_name, snapshot_data_project.
# Below is an example command to use this script to generate documents for each root_row_id in a snapshot.
# poc index-snapshot 1000GenomesSnapshotA -t=sample_info -c=sample -i=testindex -b="sh BuildIndexDocumentTest.sh"

# current date as yyyy-mm-dd HH:MM:SS
now=$(date '+%Y-%m-%d %H:%M:%S')

echo "{\"date_created\":\"$now\", \"root_row_id\":\"$1\", \"snapshot_id\":\"$2\",
\"snapshot_name\":\"$3\", \"snapshot_data_project\":\"$4\"}"

