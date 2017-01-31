# Short Description
The Information Exchange Hub (IExHub) API is a connector for Consent2Share to perform HIE operations.

# Full Description

# Supported Tags and Respective `Dockerfile` Links

[`0.6.0 (latest)`](https://github.com/bhits/iexhub/releases/tag/0.6.0), [`0.3.0`](https://github.com/bhits/iexhub/releases/tag/0.3.0)

[`Current Dockerfile`](https://github.com/bhits/iexhub/blob/master/iexhub/src/main/docker/Dockerfile)

For more information about this image, the source code, and its history, please see the [GitHub repository](https://github.com/bhits/iexhub).

# What is IExHub?

The Information Exchange Hub (IExHub) consists of connectors, services, and transformation components intended to allow applications to interoperate with standards-based Health Information Exchange (HIE) organizations.

For more information and related downloads for Consent2Share, please visit [Consent2Share](https://bhits.github.io/consent2share/).

# How to use this image

## Start a IExHub instance

Be sure to familiarize yourself with the repository's [README.md](https://github.com/bhits/iexhub) file before starting the instance.

`docker run --name iexhub -d bhits/iexhub:latest`

*NOTE: In order for this API to fully function as a microservice in the Consent2Share application, it is required to setup the dependency microservices and the support level infrastructure. Please refer to the [Consent2Share Deployment Guide](https://github.com/bhits/consent2share/releases/download/2.0.0/c2s-deployment-guide.pdf) for instructions to setup the Consent2Share infrastructure.*


## Configure

This API runs with a [default configuration](https://github.com/bhits/iexhub/blob/master/iexhub/src/main/resources/IExHub.properties) that is primarily targeted for the development environment. As of now, this entire file **MUST** be provided externally to the IExHub.

### Using a custom configuration file

To use custom `IExHub.properties`, mount the file to the docker container under `/java/iexhub/config/IExHub.properties`.

`docker run -v "/path/on/dockerhost/IExHub.properties:/java/iexhub/config/IExHub.properties" -d bhits/iexhub:latest`

## Environment Variables

When you start the IExHub image, you can edit the configuration of the IExHub instance by passing one or more environment variables on the command line. 

### CATALINA_OPTS 

This environment variable is used to setup a JVM argument, such as memory configuration and logger configuration.

An example to setup the logger level: 

`docker run --name iexhub -e CATALINA_OPTS="-Dlog4j.debug" -d bhits/iexhub:latest`

#### iexhub.logging.file

The existing `log4j.properties` in classpath depends on this variable, so it **MUST** be provided for saving the log file somewhere on the file system. If a `log4j.configuration` is provided and the `log4j.properties` file that is loaded from this location has an absolute log file location specified in `log4j.appender.LOG_FILE.File` property, `iexhub.logging.file` property is omitted.

Example:

`docker run -d -e CATALINA_OPTS="-Diexhub.logging.file=/java/iexhub/logs/InfoExchangeHub.log" bhits/iexhub:latest`

#### iexhub.config.location

The folder location for the external configuration files. Defaults to `/java/iexhub/config`.

Example:

`docker run -d -e CATALINA_OPTS="-Diexhub.config.location=/path/to/iexhub/config/folder" bhits/iexhub:latest`

#### iexhub.config.filename

The file name for the properties file that will be loaded from `iexhub.config.location`. Defaults to `IExHub.properties`.

Example:

`docker run -d -e CATALINA_OPTS="-Diexhub.config.filename=Config.properties" bhits/iexhub:latest`

# Supported Docker versions

This image is officially supported on Docker version 1.12.1.

Support for older versions (down to 1.6) is provided on a best-effort basis.

Please see the [Docker installation documentation](https://docs.docker.com/engine/installation/) for details on how to upgrade your Docker daemon.

# License

View [license](https://github.com/bhits/iexhub/blob/master/LICENSE) information for the software contained in this image.

# User Feedback

## Documentation 

Documentation for this image is stored in the [bhits/iexhub](https://github.com/bhits/iexhub) GitHub repository. Be sure to familiarize yourself with the repository's README.md file before attempting a pull request.

## Issues

If you have any problems with or questions about this image, please contact us through a [GitHub issue](https://github.com/bhits/iexhub/issues).