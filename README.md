# HPC DataStore
This is the server-side of the HPC DataStore project.

The DataStore is shortest described as [BigDataServer](https://imagej.net/plugins/bdv/server)
that can also accept images besides serving them. In other words, it is a REST HTTP-based
client-server for downloading/uploading chunks from images from/to a remote dataset.
The server is storing image data using the [N5](https://github.com/saalfeldlab/n5) on disk
in a [BigDataViewer](https://imagej.net/plugins/bdv) XML/N5 dialect. The server communicates
with its clients using [our own simple protocol.](https://docs.google.com/document/d/1ZeLc83dyNE9USBuvSCLEVGK-zQzUKFb7VGhOlVIRBvU/edit)

[Motivation, reasoning, application examples and more
details in general can be found here.](doc/OUTLINE.md)

## Notes that shall not be overlooked...
Folks, this is a young project. Still quite a few things are "under heavy development".
Also the functionality of the [reference Fiji client](https://github.com/fiji-hpc/hpc-datastore-fiji)
is often a bit lacking behind what the server currently can do. But the future is bright :-)

At this moment, this is not [Zarr](https://zarr-specs.readthedocs.io/en/core-protocol-v3.0-dev/)
compatible. Sadly! We, however, wish and plan to be. It requires to switch to N5
[Zarr backend](https://github.com/saalfeldlab/n5-zarr), which we will do once the
BigDataViewer will be able to read Zarr files directly on disk (where we also plan
to be active and help). Everything needs its time, you know.

## Technicalities
This repository hosts implementation of the DatasetsRegisterService
and DatasetServer, as [discussed here](docs/DESCRIPTION.md#connection-scheme).

The repository recognizes three types of branches:
- `production` branch hosts the last release version of the code,
- `master` branch hosts the current development, the `production` branch adjoins `master` from time to time,
- topic branch(es) host particular piece of development, it spins-off from the `master` and merges back


# Starting the server
## From command line
The easiest way to get the server up and running is to execute the following:
```
git clone https://github.com/fiji-hpc/hpc-datastore.git
cd hpc-datastore
./mvnw -Dmaven.test.skip=true clean package
(or mvnw.cmd -Dmaven.test.skip=true clean package on Windows)
./start-server
```

This downloads the recent sources, compiles them (also downloads dependencies which
will take some time) and runs the starting script. The starting script will only
print out help and ask you to provide path to where datasets shall be stored, hostname
and port to which the server shall bind to (where it should listen at).

The server is for real started with the following command:
```
./start-server PATH HOST PORT
```
One can (re)start the server on a folder with some datasets already inside,
the server will be serving it too.

One probably want to execute that inside a `screen` or `tmux` environment.


## From IDE
Run the class _cz.it4i.fiji.datastore.App_. It accepts the following properties:

- datastore.path
- quarkus.http.port
- quarkus.http.host

You can pass them as a parameter to the JVM with the -D<property.name> construct.


# Testing server
alfeios.fi.muni.cz, port 9080
CTC training dataset
