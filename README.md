# elasticsearch-poc

### Ingest the 1000 Genomes tabular data
The below script for testing the POC assumes that the 1000 Genomes tabular data has already been ingested and two Snapshots defined.
Instructures for ingesting the 1000 Genomes tabular data is in the 1000Genomes/ingest-notes.txt file.

### Compile and install the POC
Run both commands from the top-level directory.

    ./gradlew :installDist
    alias poc=./build/install/jade-search-poc/bin/jade-search-poc

### Create a new index
    poc create-index sampleindex -s='
    {
     "properties": {
       "sample": {
         "type": "keyword"
       },
       "family_id": {
         "type": "keyword"
       },
       "population_description": {
         "type": "text"
       },
       "Phase1_E_Platform": {
         "type": "keyword"
       },
       "Super_Population": {
         "type": "keyword"
       },
       "Phase1_LC_Centers": {
         "type": "keyword"
       },
       "Population": {
         "type": "keyword"
       },
       "super_population_description": {
         "type": "text"
       },
       "EBV_Coverage": {
         "type": "keyword"
       }
     }
    }
    '

Expected output:

    Index created successfully.

### Show the index metadata
    poc show-index sampleindex

Expected output:

    {
      "properties" : {
        "family_id" : {
          "type" : "keyword"
        },
        "Phase1_E_Platform" : {
          "type" : "keyword"
        },
        "population_description" : {
          "type" : "text"
        },
        "EBV_Coverage" : {
          "type" : "keyword"
        },
        "datarepo_rootRowId" : {
          "type" : "keyword"
        },
        "super_population_description" : {
          "type" : "text"
        },
        "Population" : {
          "type" : "keyword"
        },
        "Super_Population" : {
          "type" : "keyword"
        },
        "Phase1_LC_Centers" : {
          "type" : "keyword"
        },
        "sample" : {
          "type" : "keyword"
        },
        "datarepo_snapshotId" : {
          "type" : "keyword"
        }
      }
    }
    [ ]

Notice that the metadata includes two added fields: datarepo_rootRowId, datarepo_snapshotId.
These will be used to filter the documents on search.

### Index two snapshots
1000GenomesSnapshotA has a single reader: `mmdevverily@gmail.com`.

    poc index-snapshot 1000GenomesSnapshotA -b="sh 1000Genomes/BuildIndexDocument1000Genomes.sh" -c=sample -i=sampleindex -t=sample_info
    
Expected output:

    19:51:18.181 [main] INFO jadesearchpoc.Indexer - indexing snapshot (name): 1000GenomesSnapshotA
    19:51:19.421 [main] DEBUG jadesearchpoc.Indexer - SELECT sample AS root_column FROM `broad-jade-mm-data.1000GenomesSnapshotA.sample_info` WHERE sample IS NOT NULL ORDER BY sample ASC
    19:51:21.553 [main] DEBUG jadesearchpoc.Indexer - processing root_row_id: HG00096
    19:51:21.557 [main] INFO jadesearchpoc.utils.ElasticSearchUtils - searching for root_row_id: HG00096 in index: sampleindex
    19:51:22.746 [main] DEBUG jadesearchpoc.Indexer - documentIdExists: null
    19:51:22.754 [main] DEBUG jadesearchpoc.utils.ProcessUtils - started process: sh 1000Genomes/BuildIndexDocument1000Genomes.sh HG00096 504eb790-a858-479f-b9b6-0e28656458e1 1000GenomesSnapshotA broad-jade-mm-data
    19:51:25.120 [main] DEBUG jadesearchpoc.Indexer - { "EBV_Coverage": "20.31", "Phase1_E_Platform": "ILLUMINA", "Phase1_LC_Centers": "WUGSC", "Population": "GBR", "Super_Population": "EUR", "family_id": "HG00096", "population_description": "British in England and Scotland", "sample": "HG00096", "super_population_description": "European", "date_created": "$now" }
    19:51:25.754 [main] DEBUG jadesearchpoc.utils.ElasticSearchUtils - index: sampleindex, id: qlzgnXABZyY_x0meQ2df
    19:51:25.754 [main] DEBUG jadesearchpoc.utils.ElasticSearchUtils - new index document created
    19:51:25.754 [main] DEBUG jadesearchpoc.Indexer - processing root_row_id: HG00097
    19:51:25.754 [main] INFO jadesearchpoc.utils.ElasticSearchUtils - searching for root_row_id: HG00097 in index: sampleindex
    19:51:25.805 [main] DEBUG jadesearchpoc.Indexer - documentIdExists: null
    19:51:25.809 [main] DEBUG jadesearchpoc.utils.ProcessUtils - started process: sh 1000Genomes/BuildIndexDocument1000Genomes.sh HG00097 504eb790-a858-479f-b9b6-0e28656458e1 1000GenomesSnapshotA broad-jade-mm-data
    19:51:27.883 [main] DEBUG jadesearchpoc.Indexer - { "EBV_Coverage": "169.49", "Phase1_E_Platform": "ABI_SOLID", "Phase1_LC_Centers": "BCM", "Population": "GBR", "Super_Population": "EUR", "family_id": "HG00097", "population_description": "British in England and Scotland", "sample": "HG00097", "super_population_description": "European", "date_created": "$now" }
    19:51:27.952 [main] DEBUG jadesearchpoc.utils.ElasticSearchUtils - index: sampleindex, id: q1zgnXABZyY_x0meTmch
    19:51:27.952 [main] DEBUG jadesearchpoc.utils.ElasticSearchUtils - new index document created
    19:51:27.952 [main] DEBUG jadesearchpoc.Indexer - processing root_row_id: HG00099
    19:51:27.952 [main] INFO jadesearchpoc.utils.ElasticSearchUtils - searching for root_row_id: HG00099 in index: sampleindex
    19:51:28.007 [main] DEBUG jadesearchpoc.Indexer - documentIdExists: null
    19:51:28.012 [main] DEBUG jadesearchpoc.utils.ProcessUtils - started process: sh 1000Genomes/BuildIndexDocument1000Genomes.sh HG00099 504eb790-a858-479f-b9b6-0e28656458e1 1000GenomesSnapshotA broad-jade-mm-data
    19:51:30.133 [main] DEBUG jadesearchpoc.Indexer - { "EBV_Coverage": "23.04", "Phase1_E_Platform": "ABI_SOLID", "Phase1_LC_Centers": "BCM", "Population": "GBR", "Super_Population": "EUR", "family_id": "HG00099", "population_description": "British in England and Scotland", "sample": "HG00099", "super_population_description": "European", "date_created": "$now" }
    19:51:30.211 [main] DEBUG jadesearchpoc.utils.ElasticSearchUtils - index: sampleindex, id: rFzgnXABZyY_x0meVmfv
    19:51:30.211 [main] DEBUG jadesearchpoc.utils.ElasticSearchUtils - new index document created
    19:51:30.211 [main] DEBUG jadesearchpoc.Indexer - processing root_row_id: HG00100
    19:51:30.212 [main] INFO jadesearchpoc.utils.ElasticSearchUtils - searching for root_row_id: HG00100 in index: sampleindex
    19:51:30.262 [main] DEBUG jadesearchpoc.Indexer - documentIdExists: null
    19:51:30.266 [main] DEBUG jadesearchpoc.utils.ProcessUtils - started process: sh 1000Genomes/BuildIndexDocument1000Genomes.sh HG00100 504eb790-a858-479f-b9b6-0e28656458e1 1000GenomesSnapshotA broad-jade-mm-data
    19:51:32.491 [main] DEBUG jadesearchpoc.Indexer - { "EBV_Coverage": "116.22", "Phase1_E_Platform": "ILLUMINA", "Phase1_LC_Centers": "SC", "Population": "GBR", "Super_Population": "EUR", "family_id": "HG00100", "population_description": "British in England and Scotland", "sample": "HG00100", "super_population_description": "European", "date_created": "$now" }
    19:51:32.568 [main] DEBUG jadesearchpoc.utils.ElasticSearchUtils - index: sampleindex, id: rVzgnXABZyY_x0meYGco
    19:51:32.569 [main] DEBUG jadesearchpoc.utils.ElasticSearchUtils - new index document created

1000GenomesSnapshotB has two readers: `mmdevverily@gmail.com` and `mmdevverily2@gmail.com`.

    poc index-snapshot 1000GenomesSnapshotB -b="sh 1000Genomes/BuildIndexDocument1000Genomes.sh" -c=sample -i=sampleindex -t=sample_info
    
Expected output:

    19:54:22.820 [main] INFO jadesearchpoc.Indexer - indexing snapshot (name): 1000GenomesSnapshotB
    19:54:27.354 [main] INFO jadesearchpoc.utils.ElasticSearchUtils - searching for root_row_id: HG00101 in index: sampleindex
    19:54:31.871 [main] INFO jadesearchpoc.utils.ElasticSearchUtils - searching for root_row_id: HG00102 in index: sampleindex
    19:54:35.325 [main] INFO jadesearchpoc.utils.ElasticSearchUtils - searching for root_row_id: HG00103 in index: sampleindex
    19:54:38.762 [main] INFO jadesearchpoc.utils.ElasticSearchUtils - searching for root_row_id: HG00105 in index: sampleindex

### Display all the documents in the index
    poc dump-index sampleindex

Expected output is all the documents contained in the index as a JSON-formatted string.

### Search for all documents in the index as each user
Delete the credential file first, to force login again. Login as `mmdevverily2@gmail.com`. The search should return 4 documents.

Delete the credential file again, to force login again. Login as `mmdevverily@gmail.com`. The search should now return all 8 documents.

    rm /Users/marikomedlock/.jadecli/creds/StoredCredential
    
    poc search-index sampleindex -q='
    {
        "query": {
            "match_all": { "boost" : 1.2 }
        }
    }
    '

