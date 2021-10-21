# History

The ability of the [BigDataViewer (BDV)](https://imagej.net/plugins/bdv),
and of all the other software that is built around it,
to display large images is achieved in great deal because of using [chunks](DESCRIPTION.md#chunks)
and [resolution pyramids](DESCRIPTION.md#resolution-pyramids).
Technically, this is achieved by storing the images, and their lower resolution copies, using
the HDF5 file format. This brings a few advantages such as ability to use royalty-free and performant
HDF5 library, data compression, and economical representation w.r.t. number of files in the file system
by ``baking'' everything into the HDF5 container. But the compression and everything-inside principle
makes it practically good only for create-once, read-only-forever type of access. With this setting,
replacing a content, or adding another channel, cannot be done efficiently in general.

The N5 project, was designed not to use the everything-inside principle while using the chunks
and resolution pyramids. And it offers, therefore, a smooth ability to replace content of chunks,
or modify dataset ``geometry'' like adding another channel. User code controls N5 through its Java API,
plus there is a few plugins in Fiji itself to load/store full images.

The DataStore is essentially following the N5 in the core functionality, it adds more on the client side
by providing ImageJ2/Fiji plugins (and ImageJ macro commands consequently because any ImageJ2 plugin is callable
from ImageJ macro). Additionally, it [features client-server paradigm](DESCRIPTION.md#connection-scheme) in a
hope that it would make [DataStore data accessible in more programming languages](APPLICATIONS.md#a-storage-independent-image-fileformat)
than just Java and even in a rather simple way.
