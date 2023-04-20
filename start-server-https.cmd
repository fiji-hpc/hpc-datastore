set myVar="C:\Users\ValMe\Documents\GitHub\hpc-datastore"
set ip="localhost"
set port=8443
set pports="35000,35100"
set clientID="554143119581-1kmphimgugsrfm45rd65bdja8h3bh27k.apps.googleusercontent.com"
set clientSecret="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"

java ^
-Xmx1G -Dquarkus.datasource.jdbc.url="jdbc:h2:%myVar%/myDb;MODE=PostgreSQL;INIT=CREATE SCHEMA IF NOT EXISTS datastore" ^
-Ddatastore.path=%myVar% -Dquarkus.http.host=%ip% -Dquarkus.http.port=%port% -Ddatastore.ports=%pports% -Dquarkus.http.ssl-port=%port% ^
-Dquarkus.http.ssl.certificate.key-store-file=META-INF/resources/server.keystore ^
-Dcz.it4i.fiji.datastore.security.servers=google,sub,https://accounts.google.com/o/oauth2/v2/auth,https://www.googleapis.com/oauth2/v3/userinfo,https://www.googleapis.com/oauth2/v4/token,%clientID%,%clientSecret% ^
-Dquarkus.http.limits.max-body-size=200M ^
-Dcz.it4i.fiji.datastore.security.users=google:100198484073624931665:1:write ^
-Dcz.it4i.fiji.datastore.security.able="YES" ^
-Xverify:none ^
-jar target/quarkus-app/quarkus-run.jar
