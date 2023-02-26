### Stream Producer Application

Simple Spring Boot 3 application producing Kafka Events

## Install 

The application needs a ecr url set in local or global gradle.properties
```shell
ecr-repository-root=[YOUR_AWS_ACCOUNT].dkr.ecr.[YOUR_REGION].amazonaws.com
```
The gradle deploy task will push to ecr and execute the helm chart
```shell
./gradlew deploy
```

See build.gradle.kts for details