# Use Maven to build the app
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY . .
# Run the build (skipping tests to be faster)
RUN mvn clean package -DskipTests

# Use a lightweight Java to run the app
FROM eclipse-temurin:21-jdk
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-Djava.net.preferIPv4Stack=true","-jar","app.jar"]