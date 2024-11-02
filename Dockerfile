## Use an official Maven image to build the app
#FROM maven:3.8.4-openjdk-17 AS build
#WORKDIR /app
#
## Copy the pom.xml and download the dependencies
#COPY pom.xml .
#RUN mvn dependency:go-offline
#
## Copy the rest of the project files
#COPY src ./src
#
## Build the project
#RUN mvn clean package -DskipTests
#
## Use an official OpenJDK runtime as a parent image
#FROM openjdk:17-jdk-slim
#
## Set the working directory
#WORKDIR /app
#
## Copy the jar from the build image
#COPY --from=build /app/target/*.jar app.jar
#
## Expose the port on which your application will run
#EXPOSE 8080
#
## Set the entry point for the container
#ENTRYPOINT ["java", "-jar", "app.jar"]
