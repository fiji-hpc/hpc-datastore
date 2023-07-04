#/usr/bin/env bash

if [ ! -d "target" ]; then
	echo "must be operated from the root folder of the hpc-datastore git repo"
	exit 1
fi

echo "Creating (zipping) a file 'server_jars.zip'."
zip -r server_jars.zip start-server target/hpc-datastore-*.jar target/quarkus-app > /dev/null
echo "Created a file 'server_jars.zip'."

echo
echo "On target host, create a folder into which unzip this file,"
echo "then execute the script 'start-server' from that folder..."
echo "It will tell what parameters to provide to make it run."
