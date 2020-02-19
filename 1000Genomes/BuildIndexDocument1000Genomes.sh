#!/bin/bash

# This is a script to build an index document for snapshots derived from the 1000Genomes dataset.
# The arguments passed to the script are, in order: root_row_id, snapshot_id, snapshot_name, snapshot_data_project.

# Below is an example command to use this script to generate documents for each root_row_id in a snapshot.
# poc index-snapshot 1000GenomesSnapshotA -b="sh 1000Genomes/BuildIndexDocument1000Genomes.sh" -c=sample -i=testindex -t=sample_info

# check for expected number of arguments
if [ "$#" -ne 4 ]
then
  echo "Bad number of arguments. Expected 4, actual ($#)."
  exit 1 # error
fi
root_row_id=$1
snapshot_id=$2
snapshot_name=$3
snapshot_data_project=$4

# build SQL query to send to BigQuery
sql_query="
  SELECT
    sample, family_id, population_description, Phase1_E_Platform,
    Super_Population, Phase1_LC_Centers, Population, super_population_description, EBV_Coverage
  FROM \`$snapshot_data_project\`.$snapshot_name.sample_info
  WHERE sample='$root_row_id'"

# execute SQL query using BigQuery commmand line tool and store the result in a temporary file
bq query --format=json --use_legacy_sql=false "$sql_query" > tmp_sqloutput.json

# check that only one row was found
num_rows=$(cat tmp_sqloutput.json | jq length)
if [ "$num_rows" -ne 1 ]
then
  echo "Bad number of rows found for root_row_id. Expected 1, actual ($num_rows)."
  exit 1 # error
fi

# extract the one row from the array
single_row=$(cat tmp_sqloutput.json | jq .[0])

# remove the temporary file used to store the BigQuery result
rm -f tmp_sqloutput.json

# current date as yyyy-mm-dd HH:MM:SS
now=$(date '+%Y-%m-%d %H:%M:%S')

# add the current date to the JSON document
index_doc=$(echo $single_row | jq '. += {"date_created":"$now"}')

# print the final JSON document to stdout
echo $index_doc

exit 0 # success
