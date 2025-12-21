## Notes on Spring, JPA/Hibernate

- Returns 0 or 1 record, for more than 1 throws Exception
  ```java
  Optional<SudokuBoard> findByBoard(String board); 
  ```

- To return 0 or many records
  ```java
  List<SudokuBoard> findAllByName(String name); 
  ```

