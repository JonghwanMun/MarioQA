#! /bin/bash

if [ -d 'target' ]; then
	echo '===> Remove the existing target folder'
	rm -r target
fi
mvn install
