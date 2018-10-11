## What does this do?

A simple JPA spring-boot-starter to add conventions for flagging records as discarded.



## Usage

Add  `@EnableJpaSoftDeleteRepositories` Annotation to your  on Application class

```java
@SpringBootApplication
@EnableJpaSoftDeleteRepositories
public class XXXApplication {
    public static void main(String[] args){
        SpringApplication.run(JpaSoftDeleteSpringBootStarterApplication.class, args);
    }
}
```

Add  `@SoftDelete`  Annotation to your  on Repository class

```java
@SoftDelete
public interface XXXRepository extends JpaRepository<T, ID> {
    
}
```



Still writing.........................................................