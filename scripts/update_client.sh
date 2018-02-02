#!/bin/bash

# To be run from the Apollo's root directory

# Update LSAA client
if [ -d plugins/ApolloLsaa/client/LSAA ]; then
	rm -rf jbrowse-download/plugins/LSAA
	cp -r plugins/ApolloLsaa/client/LSAA jbrowse-download/plugins/LSAA

	if [ -f web-app/jbrowse/index.html ]; then 
			rm -rf web-app/jbrowse/plugins/LSAA
			cp -r jbrowse-download/plugins/LSAA web-app/jbrowse/plugins/LSAA
	    echo "LSAA client installed"
	else
	    echo "JBrowse not installed; cannot install LSAA client"
	fi
else
	echo "cannot find source for LSAA client"
fi

# Update PairedReadViewer client
if [ -d plugins/ApolloLsaa/client/PairedReadViewer ]; then
	rm -rf jbrowse-download/plugins/PairedReadViewer
	cp -r plugins/ApolloLsaa/client/PairedReadViewer jbrowse-download/plugins/PairedReadViewer

	if [ -f web-app/jbrowse/index.html ]; then 
			rm -rf web-app/jbrowse/plugins/PairedReadViewer
			cp -r jbrowse-download/plugins/PairedReadViewer web-app/jbrowse/plugins/PairedReadViewer
	    echo "PairedReadViewer client installed"
	else
	    echo "JBrowse not installed; cannot install PairedReadViewer client"
	fi
else
	echo "cannot find source for PairedReadViewer client"
fi

echo "Client updated successfully"