# Merlin
Tool for modifiying ProArc Export for Kramerius with Imageserver

For single title processing use: (uses directory defined in -o / must exist)

`java -jar merlin.jar -iI /path/to/jp2/export -iK /path/to/k4/export -o /path/to/output`

For batch title processing use: (creates directory with timestamp)

`java -jar merlin.jar /path/to/jp2/export /path/to/foxml/export /path/to/output`
