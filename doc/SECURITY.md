Working bunch of parameters for running from IDE:

-Dquarkus.http.limits.max-body-size=200M 
-Dquarkus.datasource.jdbc.url="jdbc:h2:<path-to-datastore>/myDb;MODE=PostgreSQL;INIT=CREATE SCHEMA IF NOT EXISTS datastore" 
-Ddatastore.path=<path-to-datastore> 
-Dquarkus.https.host=localhost 
-Dquarkus.https.port=8443 -Xverify:none 
-Dquarkus.http.ssl.certificate.key-store-file=META-INF/resources/server.keystore 
-Dcz.it4i.fiji.datastore.security.servers=google,sub,https://accounts.google.com/o/oauth2/v2/auth,https://www.googleapis.com/oauth2/v3/userinfo,https://www.googleapis.com/oauth2/v4/token,<client-id>,<client-secret> 
-Dcz.it4i.fiji.datastore.security.users=google:<user-id-in-google>:<local-user-id>:write



You need to edit the property:
-quarkus.datasource.jdbc.url a datastore.path - set path-to-datastore

-cz.it4i.fiji.datastore.security.servers - here you have to set client-id, client-secret, the meaning of other tokens you should see in the code
-cz.it4i.fiji.datastore.security.users - this does not need to be set for authentication, it is only important during authorization and defines access rights for individual users, these are currently very simple

