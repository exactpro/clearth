ClearTH
==========

This is the official ClearTH project repository.

## Introduction

ClearTH is a test automation tool whose primary purpose is testing of Clearing, Settlement and Back-Office Systems.

It is able to simultaneously execute multiple end-to-end test scenarios in batches. Test scenarios can be executed within a schedule, thus providing fully autonomous test execution capabilities.

ClearTH typically interacts with the system under test via its gateways / APIs, but can be easily extended with new protocols and data formats to work with.

ClearTH Core provides basic functionality to execute test scenarios, some automation actions and connectivity support. ClearTH modules extend this functionality with new data formats support and other features.

Applications that use ClearTH are built on top of ClearTH Core, optionally including its modules.

## Tutorials

Here are the links to the tutorial videos introducing you to the main features of ClearTH and helping you get set up.

  - Getting Started:
    https://youtu.be/pAGXzRbOE24
  - Project Overview:
    https://youtu.be/DbBpV-rWAVE
  - Automation Script:
    https://youtu.be/HPN64NgWriU
  - Automation Example (Part 1):
    https://youtu.be/gOYakNgS5c4
  - Automation Example (Part 2):
    https://youtu.be/7iI43EMenrQ
  - Custom Actions (Part 1):
    https://youtu.be/l1gl26aEg1g
  - Custom Actions (Part 2):
    https://youtu.be/VjC7GCWw_B8
  - Adding a Module for XML Messages
    https://youtu.be/Mv3WxHgjmSQ
  - Automation Example with Messages
    https://youtu.be/uFbp8RpGrCw
  - Built-in Tools
    https://youtu.be/akayR4N3dhk



## How to build

Build and publish ClearTH Core and modules to a local repository, i.e. "shared" directory in the repository root by executing the following command from the repository root:
```
$ ./gradlew clean build publish
```

Create a new project that will use ClearTH Core and its GUI module by executing the following command from the repository root:
```
$ ./gradlew createProject -PnewProjectDir=../PROJECTDIR -PnewProjectName=PROJECTNAME
```

The new project will be created in the directory adjacent to the repository root.


## Starting ClearTH

Navigate to the directory with your new project and execute the following command to start ClearTH within the Jetty server:
```
$ ./gradlew jettyRun
```

Alternatively, you can use the following command to explicitly build the WAR file with your project and deploy it to the Jetty server:
```
$ ./gradlew runClearTH
```

ClearTH GUI will be available at http://localhost:8080/clearth

You can log-in by using the following login/password:
```
admin/admin
```


## ClearTH in Docker

Navigate to the directory with your new project and create the WAR file with your project:
```
$ ./gradlew copyWar
```

Build Docker image:
```
$ docker build -t NAME:TAG -f docker/Dockerfile .
```

Create a directory for persistent data, give it write permissions:
```
$ mkdir docker-volume-data && chmod o+wx docker-volume-data/
```

Now you can start ClearTH from the image you built:
```
$ docker run -v ./docker-volume-data:/clearth/persistentFiles -p 8080:8080 -it NAME:TAG
```

ClearTH GUI will be available at http://localhost:8080/clearth

You can log-in by using the following login/password:
```
admin/admin
```