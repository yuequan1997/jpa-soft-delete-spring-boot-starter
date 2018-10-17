## What does this do?

A simple JPA spring-boot-starter to add conventions for flagging records as discarded.



## Usage

Add a maven dependency to your `pom.xml`

```xml
<dependency>
  <groupId>org.yuequan</groupId>
  <artifactId>jpa-soft-delete-spring-boot-starter</artifactId>
  <version>1.0.0.RELEASE</version>
</dependency>
```



Add  `@EnableJpaSoftDeleteRepositories` annotation to your  in  application class

```java
@SpringBootApplication
@EnableJpaSoftDeleteRepositories
public class JpaSoftDeleteSpringBootStarterApplication {
    public static void main(String[] args){
        SpringApplication.run(JpaSoftDeleteSpringBootStarterApplication.class, args);
    }
}
```

Add  `@SoftDelete`  annotation to your  in repository class

```java
@SoftDelete
public interface UserRepository extends JpaRepository<T, ID> {
    
}
```

Add `removed_at` column to your  table and add `removedAt` to your entity, will support customization in the next release

```java
userRepository.delete(user);   
// update users set removed_at=? where id=1

userRepository.findById(user.getId())
// select .... from users user0_ where user0_.id=.. and (user0_.removed_at is null)
    
userRepository.findAll(PageRequest.of(0, count))
// select .... from users user0_ where user0_.removed_at is null limit ?
```

