#!/bin/bash

# To be run from the Apollo's root directory

# Remove compiled code
if [ -d target/work ]; then
	rm -rf target/work
else
	echo "cannot find any compiled targets in target/ folder"
fi

# Compile ApolloLsaa
cd plugins/ApolloLsaa
echo "Compiling ApolloLsaa"
grails compile && grails maven-install
cd ../../

echo "Plugin compiled successfully"