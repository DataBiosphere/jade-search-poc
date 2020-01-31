#!/bin/bash

alias jc=~/Workspaces/jade-data-repo-cli/build/install/jadecli/bin/jadecli

# start a new file each time this script is run
rm -f file-load/bam-table-load.json

# loop through dataset paths
let ctr=0
while read inputGSPath; do
	let ctr=ctr+1

	# convert the original GS path to the dataset file path
	filePath=$(echo $inputGSPath | sed 's/gs:\/\/genomics\-public\-data\/1000\-genomes\/bam\/\(.*\)/\/bam_files\/\1/')

	# lookup the file details from the dataset file path
	fileDetails=$(jc dataset file lookup --file-path "$filePath" --format json 1000GenomesDataset)

	# parse out the file id and name
	fileId=$(echo $fileDetails | jq .fileId)
	fileName=$(echo $fileDetails | jq .description | sed 's/ BAM file//')

	# write the table row as JSON to a file
	echo "{\"Sample\":$fileName, \"BAM_File_Ref\":$fileId, \"BAM_File_Id\":\"$filePath\"}" >> file-load/bam-table-load.json

	if [ $ctr -gt 4 ]; then
		break
	fi
done <file-load/filepath-list-test.txt

echo "Processed $ctr files"

