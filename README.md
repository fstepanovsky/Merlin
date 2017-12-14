# Merlin
Tool for modifiying ProArc Export for Kramerius with Imageserver

## Usage
### Single title processing
For processing with output as a pack use:

`java -jar merlin.jar -iI /path/to/jp2/export -iK /path/to/k4/export -o /path/to/output`

For processing with direct output into imageserver and kramerius import folder use:

```
java -jar merlin.jar -iI /path/to/jp2/export -iK /path/to/k4/export -oD -oK /path/to/imageserver -oI /path/to/kramerius/ 
```
Note that:
- `/path/to/Kramerius` must Kramerius app see at `/opt/app-root/src/.kramerius4/import/ProArc/`

### Batch title processing
For batch title processing use: (creates directory with timestamp)

`java -jar merlin.jar /path/to/jp2/export /path/to/foxml/export /path/to/output`

## Optional arguments
### Generating aleph csv file

use `-aD /path/to/aleph/dir` for enabling generating csv for updating Aleph record

### Kramerius API import automatic request

use `-kA http://kramerius.address -kL httpBasicAuthCredentials` for enabling import request over Kramerius API after the processing of each title is finished
