In general, one needs to [have *HPC-DataStore* update site installed in your Fiji](imgs/datastore-fiji-update-site.png)
beforehand.

# Creating from an Existing HDF5/XML
Use Fiji menu: *Plugins* -> *BigDataViewer* -> *Export SPIM data as remote XML/N5*
![Example of creating a dataset from HDF5/XML](imgs/create_from_hdf5xml.png)

# Converting Between TIFFs and DataStore
There are [demo ImageJ macros](https://github.com/fiji-hpc/hpc-datastore-fiji/tree/master/src/main/ijm)
to serve both directions. [More details are also given here.](DESCRIPTION.md#gui-enhanced-download-macro)

# Adding a New Channel
The corresponding GUI (or ImageJ macro) client code is missing now.
[One has to manage from command line.](https://github.com/fiji-hpc/hpc-datastore/blob/master/src/test/bash/rest-add-channel)
(...and replace `locahost` for the proper address, etc.)

[Check the new dataset configuration afterwards.](FEATURES.md#querying-the-datastore-server)

-------------------
More manuals on how to achieve various things should appear here. Very sorry for now.
