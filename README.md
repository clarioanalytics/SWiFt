_SWiFt_ is a mini-framework for using Amazon SWF based off of the Amazon SDK for Java.

## Building

To do a clean and rebuild:
    mvn clean install

## Config Examples
To run the examples in `com.clario.swift.example` create the file `src/main/resources/config.properties` with the following information:

    amazon.aws.id=<your id>
    amazon.aws.key=<your secret key>

    # number of threads to create for example activity workers and decision workers
    activity.threads=5
    decision.threads=2