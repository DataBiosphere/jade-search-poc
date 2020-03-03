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
    19:51:21.557 [main] INFO jadesearchpoc.utils.ElasticSearchUtils - searching for root_row_id: HG00096 in index: sampleindex
    19:51:25.754 [main] INFO jadesearchpoc.utils.ElasticSearchUtils - searching for root_row_id: HG00097 in index: sampleindex
    19:51:27.952 [main] INFO jadesearchpoc.utils.ElasticSearchUtils - searching for root_row_id: HG00099 in index: sampleindex
    19:51:30.212 [main] INFO jadesearchpoc.utils.ElasticSearchUtils - searching for root_row_id: HG00100 in index: sampleindex

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

