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

# Migrating and/or Duplicating a dataset among multiple DataStore servers
This operation comprises several steps, detailed below.

## Physically copying N5/Zarr files into another DataStore server
All image data are persistently held on a drive by means of (many) standard folders and regular files (and they are [organized as N5 (or Zarr) format](DESCRIPTION.md#storage-architecture) dictates), no special files such as links are used. It is easy to copy these files to another location using standard tools of your operating system. Note that the DataStore is also using a small database where the parameters of datasets are held to answer queries faster. The dataset in principle contains redundant/duplicate information, only stored more conveniently performance-wise.

In particular, one can copy the dataset files into a root folder (possibly even on another computer) that is served by another DataStore server. In this case, however, make sure the full tree rooted with the UUID-named folder, which is the top-level folder of a dataset, is copied. The database file shall not be copied -- think of it as that it does not belong to a dataset, it belongs to a DataStore server, and we're moving a dataset here not the full server.

## Notifying the DataStore of a newly added dataset
If a UUID-named folder, which holds a dataset, is placed (possibly among other datasets) into a folder served by another DataStore, the server needs to be notified that a new dataset has just become available. This can be done ATM via command line:

```
curl -X POST http://localhost:9080/datasets/73c0f152-48ba-4936-833b-2ad97d9751cc
```

where `localhost:9080` is the URL where the notified server is working, and the UUID `73c0f152-48ba-4936-833b-2ad97d9751cc` is the name of the newly being added dataset. The notified server will now rescan the relevant folder structure (yes, the one that has been just copied in), will read the dataset's parameters, will update its own database file, and will start serving this dataset too immediately.

This way, cloning of a dataset can be achieved.

## Deleting a dataset from DataStore sever
To turn the cloning operation into a move operation, it remains only to delete the dataset in the source DataStore server. This can be conveniently achieved from Fiji, using DataStore menu options `Plugins -> HPC DataStore -> Delete -> Delete dataset or its version`, choose "whole dataset" option. The dialog will ask the server to remove the dataset completely, which means deleting relevant entries in its database and deleting also the files and folders on a drive -- this operation has no undo.

# Serving one and the same dataset from multiple DataStore servers
If multiple servers are desired to be running and serving the same image data, it is mandatory that all the servers' processes can reach the same dataset UUID-named root folder. This can be achieved when the dataset is stored on a commonly shared filesystem within your institute. One then aside creates folders that each DataStore server will be serving, creates symbolic links in these folders that point to the source dataset UUID-named root folder, and starts the relevant servers and [notifies them one by one as explained above](HOWTO.md#notifying-the-datastore-of-a-newly-added-dataset). It basically pretends the data has been copied in, but instead a link is used to reach the data.

All servers in this way are serving (reading and writing) the same data with changes to the data propagating immediately. It is, however, not advisable to change the geometry of the dataset served in this way, only pixel data is okay to change. Also, avoid removing the dataset from any server, it would delete the source dataset.

-------------------
More manuals on how to achieve various things will be appearing here.
