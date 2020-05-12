#### Run "sh ./ingest_dataset.sh to see usage."
#### Sections below: Constants, Functions, Main

#### Constants
FILE_DATA_LOCATION="gs://jade-testdata/tcga/files/"
TABULAR_DATA_LOCATION="gs://jade-testdata/tcga/"
TABULAR_DATA_FORMAT="json"
DR_PROFILE_NAME="TCGAProfile"
DR_BILLING_ACCT="00708C-45D19D-27AAFA"
DR_DATASET_NAME="TCGADataset"
JADECLI_EXECUTABLE="~/Workspaces/jade-data-repo-cli/build/install/jadecli/bin/jadecli"
TABLE_NAMES_WITHOUT_FILEREFS=("biospecimen" "clinical" "case" "file")
FILE_NAMES=()

alias jadecli=$JADECLI_EXECUTABLE


#### Functions
##   0. Print help
function print_help {
	echo
	echo "This script provides commands to ingest a dataset and create a snapshot of it."
	echo "Later steps may depend on previous ones."
	echo
	echo "The structure of the script is intended to be applicable to various different datasets."
	echo "Each version of the script should only apply to a single dataset (e.g. EpiLIMS, 1000Genomes, TCGA)."
	echo
	echo "Please specify the name of the step to run (e.g. sh ./ingest_dataset.sh create-dataset)"
	echo "    0. [help] Print help"
	echo "    1. [list-source-data] List the source data files and where they live"
	echo "    2. [create-profile] Create the profile"
	echo "    3. [create-dataset] Create the dataset"
	echo "    4. [upload-tables] Upload tabular data"
	echo "    5. [upload-files] Upload file data"
	echo "    6. [upload-filerefs] Add file references to tabular data (in separate table)"
	echo "    7. [delete-dataset] Delete the dataset"
	echo "    8. [delete-profile] Delete the profile"
	echo
}

##   1. List the source data files and where they live
function list_source_data {
	echo
	echo "Tabular data is stored in: $TABULAR_DATA_LOCATION"
	gsutil ls -l $TABULAR_DATA_LOCATION
	echo
	echo "File data is stored in: $FILE_DATA_LOCATION"
	gsutil ls -l $FILE_DATA_LOCATION
	echo
}

##   2. Create the profile
function create_profile {
	echo
	echo "Creating profile: $DR_PROFILE_NAME"
	jadecli profile create --name $DR_PROFILE_NAME --account $DR_BILLING_ACCT
	echo
}

##   3. Create the dataset
function create_dataset {
	echo
	echo "Creating dataset: $DR_DATASET_NAME"
	jadecli dataset create --input-json ./create-dataset.json --profile $DR_PROFILE_NAME --name $DR_DATASET_NAME
	echo
}

##   4. Upload tabular data
function upload_tables {
	echo
	for TABLE_NAME in ${TABLE_NAMES_WITHOUT_FILEREFS[@]}; do
		echo "Ingesting data into table: $TABLE_NAME"
		jadecli dataset table load --table $TABLE_NAME --input-gspath ${TABULAR_DATA_LOCATION}${TABLE_NAME}.$TABULAR_DATA_FORMAT --input-format $TABULAR_DATA_FORMAT $DR_DATASET_NAME
	done
}

##   5. Upload file data
function upload_files {
	echo
	for FILE_NAME in ${FILE_NAMES[@]}; do
		echo "Ingesting file: $FILE_NAME"
		jadecli dataset file load --profile $DR_PROFILE_NAME --input-gspath ${FILE_DATA_LOCATION}${FILE_NAME} --target-path /$FILE_NAME --mime-type "application/octet-stream" --description "$FILE_NAME" --format json $DR_DATASET_NAME
	done
}

##   6. Add file references to tabular data (in separate table)
function upload_filerefs {
	echo
	echo "No file references for TCGA dataset"
}

##   7. [delete-dataset] Delete the dataset
function delete_dataset {
	echo
	echo "Deleting the dataset"
	jadecli dataset delete $DR_DATASET_NAME
}

##   8. [delete-profile] Delete the profile
function delete_profile {
	echo
	echo "Deleting the profile"
	jadecli profile delete $DR_PROFILE_NAME
}


#### Main
case "$1" in
	"help")
	    print_help
	    ;;
	"list-source-data")
	    list_source_data
	    ;;
	"create-profile")
	    create_profile
	    ;;
	"create-dataset")
	    create_dataset
	    ;;
	"upload-tables")
	    upload_tables
	    ;;
	"upload-files")
	    upload_files
	    ;;
	"upload-filerefs")
	    upload_filerefs
	    ;;
	"delete-dataset")
	    delete_dataset
	    ;;
	"delete-profile")
	    delete_profile
	    ;;
    *)
        print_help
esac
