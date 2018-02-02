#!/bin/bash

PORT=$1
# Include Apollo/JBrowse scripts in PATH
flatfile-to-json.pl 1> /dev/null 2> /dev/null
if [ $? -ne 2 ]; then
	export PATH=$PATH:`pwd`/bin
fi

# To be run from the Apollo's root directory
./plugins/ApolloLsaa/scripts/update_server.sh && ./plugins/ApolloLsaa/scripts/update_client.sh && ./apollo run-local ${PORT}
