#!/bin/bash

# This is an example script to build an index document.
# The arguments passed to the script are, in order: root_row_id, snapshot_id, snapshot_name, snapshot_data_project.

# Below is an example command to use this script to generate documents for each root_row_id in a snapshot.
# poc index-snapshot 1000GenomesSnapshotA -t=sample_info -c=sample -i=testindex -b="sh BuildIndexDocumentTest.sh"

# check for expected number of arguments
if [ "$#" -ne 4 ]
then
  echo "Bad number of arguments. Expected 4, actual ($#)."
  exit 1 # error
fi

# current date as yyyy-mm-dd HH:MM:SS
now=$(date '+%Y-%m-%d %H:%M:%S')

echo "{\"date_created\":\"$now\", \"root_row_id\":\"$1\", \"snapshot_id\":\"$2\",
\"snapshot_name\":\"$3\", \"snapshot_data_project\":\"$4\"}"

exit 0 # success
