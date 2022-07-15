### Prerequisites

- This application uses [Spring Boot Web Starter](https://spring.io/guides/gs/spring-boot/)
- This application uses Java 1.8
- This application uses Maven for building and dependency management

### Application Structure

- Files must be located under `/src/main/java` to be picked up by Spring
- The demo source code is located in `src/main/java/nl/kraaknet/countbot` with various folders inside.
- The project assumes `nl.kraaknet.countbot.CountBot` is the Spring Boot Application. To change this, remove the `@SpringBootApplication` annotation from `DemoApplication` and add it to another application class.

### Running the app

Press the "Run" button

-- OR -- 

From the Terminal in repl.it, run:

```
mvn clean package

mvn spring-boot:run
```


### Troubleshooting

- If the terminal window gets stuck in a loop while trying to run a `java` command, refresh the repl.it browser window for this project.