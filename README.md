# Access Control Server
This is the server for a simple access control system to restrict access to a particular area. This part of the system
is responsible for managing the access control list and granting access to users. In order to run this system, you need 
to have an MQTT broker running, since this program uses MQTT to communicate with the other parts of the system. This program
is written entirely in Java, and uses the Eclipse Paho MQTT client library to communicate with the MQTT broker. You also
need a MySQL-based database server to store the access control list. This program uses the JDBC API to communicate with
the database server.

## Building & running the program
To build the program, you need to have the following installed:
* Java 17 or later
* Maven 3.6.0 or later
* A MySQL-based database server (tested with MariaDB 10.5.19)
* An MQTT broker (tested with Mosquitto 2.0.11)

Once you have all the prerequisites installed, you can build the program by running the following command:
```shell
mvn clean package
```

This will create a JAR file in the `target` directory. You can then run the program by running the following command:
```shell
java -jar target/AccessControlServer-1.0-SNAPSHOT.jar
```

## Configuration
In order for the program to run correctly, you need to provide some initial configuration. This includes the database
server credentials and the MQTT broker credentials (those are later automatically stored in the database). You can also
put the configuration in a file and then tell the program to load it using the `--config=<filename>` command line argument. The
configuration file should be in the `.properties` format. The following is a list of all the configuration options:
* `db-host`: The hostname of the database server.
* `db-name`: The name of the database to use.
* `db-user`: The username to use to connect to the database.
* `db-password`: The password to use to connect to the database.
* `mqtt-broker-uri`: The URI of the MQTT broker.
* `mqtt-user`: The username to use to connect to the MQTT broker.
* `mqtt-password`: The password to use to connect to the MQTT broker.ä