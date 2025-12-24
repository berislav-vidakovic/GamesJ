## Notes on Java, Spring, JPA/Hibernate

- Returns 0 or 1 record, for more than 1 throws Exception
  ```java
  Optional<SudokuBoard> findByBoard(String board); 
  ```

- To return 0 or many records
  ```java
  List<SudokuBoard> findAllByName(String name); 
  ```

- Immutable list - no add, remove
  ```java
  List<String> techstack = List.of(
      baseUrl + "/images/java.png",
      baseUrl + "/images/spring.png",
      baseUrl + "/images/mysql.png"
  );
  ```

- Mutable list
  ```java
  this.techstack = new ArrayList<>(techstack);
  ```
