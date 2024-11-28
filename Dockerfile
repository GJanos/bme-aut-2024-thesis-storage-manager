# Use Maven for building the application
FROM maven:3.8.4-openjdk-17 AS build
WORKDIR /app

# Copy the Maven build files and pre-fetch dependencies
COPY pom.xml .
RUN mvn dependency:go-offline  # Downloads dependencies to reduce build time for subsequent runs.

# Copy source code and build the project
COPY src ./src
COPY sonar-project.properties .

RUN mvn clean package -DskipTests  # Builds the app into a JAR file without running tests.

# Use an OpenJDK runtime for running the application
FROM openjdk:17-jdk-slim
WORKDIR /app

# Copy the built JAR file from the "build" stage
COPY --from=build /app/target/*.jar app.jar

# Expose the application port
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]

