package cz.it4i.fiji.datastore.security;

public enum PermissionType {
    R("r"), // Read
    W("w"), // Write
    C("c"), // Create
    RW("rw"); // Read-Write

    private String value;

    PermissionType(String value) {
        this.value = value;
    }

    public static PermissionType fromString(String permissionString) {
        for (PermissionType permissionType : PermissionType.values()) {
            if (permissionType.value.equalsIgnoreCase(permissionString)) {
                return permissionType;
            }
        }
        throw new IllegalArgumentException("Invalid permission string: " + permissionString);
    }
}