# Use an official Maven image to build the project
FROM maven:3.8.6-openjdk-11 AS build

# Set the working directory in the container
WORKDIR /app

# Copy the pom.xml file and install dependencies
COPY pom.xml /app/

# Download all dependencies into the Maven cache
#RUN mvn dependency:go-offline

# Copy the rest of the application source code
COPY /src/ /app/src

COPY /lib/ /app/lib


# Build the application
RUN mvn clean package -DskipTests

# Use a new minimal JDK image to run the application
FROM openjdk:11-jre-slim

# Set the working directory in the container
WORKDIR /app

# Copy the built jar from the Maven image
COPY --from=build /app/target/switch-backoffice-jar-with-dependencies.jar /app/switch-backoffice-jar-with-dependencies.jar

# Expose the port the application will run on
EXPOSE 8080

# Set the entry point to run the jar file
ENTRYPOINT ["java", "-jar", "switch-backoffice-jar-with-dependencies.jar"]
