# HTTP Server in Java

This is a toy project to understand the lower level details involved in HTTP Server.
Validation of implementation is done through 8th Light's [cob_spec](https://github.com/8thlight/cob_spec).

## Prerequisites/ Notes
* JDK 8
* If using [asdf](https://github.com/asdf-vm/asdf), `.tool-versions` specifies tested JDK version. 
* For Windows, replace "./gradlew" with "./gradlew.bat".

## Run Server
Run server and output logs to console:
```console
./gradlew run --args='-p 8080 -d <directory>' -PlogAppender=Console -q
```
where
* `-p`: port
* `-d`: public serving and writing directory
* `-PlogAppender`: appender selection for logs (`File`/`Console`); sets the JVM Argument `-DlogAppender`

Run server and output logs to `<directory>/logs` :
```console
./gradlew run --args='-p 8080 -d <directory>' -q
```

## Testing
Run all tests:
```console
./gradlew clean check
```

Cob Spec:
1. Ensure java is JDK 8:
    ```console
    java -version
    ```
2. Follow [Getting Started](https://github.com/8thlight/cob_spec#getting-started) instructions to build Cob Spec.
3. Follow [Starting Fitnesse Server](https://github.com/8thlight/cob_spec#starting-fitnesse-server) to start Spec server
and navigate to Spec page.
4. Build HTTP Server distribution start scripts:
    ```console
    ./gradlew clean installDist
    ```
5. Follow [Configuring Cob Spec](https://github.com/8thlight/cob_spec#configuring-cob-spec), noting the following:
    *  `SERVER_START_COMMAND` should be set to `<path-to-http-server-java>/build/install/http-server-java/bin/http-server-java`
6. Follow [Running Tests](https://github.com/8thlight/cob_spec#running-tests) to run all tests.
