In general, one needs to [have *HPC-DataStore* update site installed in your Fiji](imgs/datastore-fiji-update-site.png)
beforehand.

# Creating from an Existing HDF5/XML
Use Fiji menu: *Plugins* -> *BigDataViewer* -> *Export SPIM data as remote XML/N5*
![Example of creating a dataset from HDF5/XML](imgs/create_from_hdf5xml.png)

# Converting Between TIFFs and DataStore
There are [demo ImageJ macros](https://github.com/fiji-hpc/hpc-datastore-fiji/tree/master/src/main/ijm)
to serve both directions. [More details are also given here.](DESCRIPTION.md#gui-enhanced-download-macro)

# Adding a New Channel
In Fiji, go to Plugins -> HPC DataStore -> Modify -> Add channel to dataset. A dialog window comes out
asking you to provide the server URL and UUID of the dataset. It then adds *one* (empty) channel to that dataset,
and should respond "OK" to the console window (Fiji -> Window -> Console).

[Or, you can manage from command line.](https://github.com/fiji-hpc/hpc-datastore/blob/master/src/test/bash/rest-add-channel)
(...and replace `locahost` for the proper address, etc.)

[Check the new dataset configuration afterwards.](FEATURES.md#querying-the-datastore-server)
And if you had a local `.xml` file pointing to that dataset, you need to update it, e.g., by downloading
a fresh version of it in Fiji -> Plugins -> HPC DataStore -> BigDataViewer -> Save XML (DataStore).

# Viewing DataStore content with BigDataViewer...
... or with anything that can open BDV's `.xml` files (like Labkit, Mastodon, BigStitcher, MoBIE, CellSketch...).

The communication between the DataStore server and BDV client can happen using two different communication channels,
either using the emulation of the BigDataServer protocol, or using our own protocol. Unlike the former, the latter
requires that HPC DataStore Fiji update site to be enabled and installed because it introduces an implementation module
to the BDV so that the BDV can speak and thus display images using our protocol. The legacy way does not require anything
extra to be installed.

## Legacy BigDataServer and BDV
To directly start a BDV on a particular dataset (UUID of which needs to be known in advance), one can open Fiji
(which always comes with BDV inside), and go to Plugins -> HPC DataStore -> BigDataViewer -> Open in BDV (legacy BDS).
This opens the BDV remote server browsing window, user chooses available version, and then the usual BDV window opens directly.

Another option is to download the BDV typical `.xml` file (Fiji -> Plugins -> HPC DataStore -> BigDataViewer -> Save XML (legacy BDS)),
and open it in the BDV (e.g., Fiji -> Plugins -> BigDataViewer -> Open XML/HDF5). Since this file acts as a handle to the particular dataset,
one can share it with a friend (over email) or provide it to some other application (like Labkit, Mastodon, etc.).

## DataStore and BDV
This is principally the same as the above, except that the new communication protocol is used instead. The versions of our datasets
appear as another channels in the BDV, including the special version `mixedLatest`. And BDV clients need to have `.jar` files from
our Fiji update site in their class paths.

-------------------
More manuals on how to achieve various things should appear here. Very sorry for now.
