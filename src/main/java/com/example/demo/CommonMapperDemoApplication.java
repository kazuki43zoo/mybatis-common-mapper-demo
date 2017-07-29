package com.example.demo;

import com.example.demo.domain.Todo;
import com.example.demo.mapper.TodoMapper;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

@SpringBootApplication
public class CommonMapperDemoApplication {

  public static void main(String[] args) {
    SpringApplication.run(CommonMapperDemoApplication.class, args);
  }

  @Bean
  CommandLineRunner demo(TodoMapper todoMapper) {
    return args -> {

      Todo newTodo = new Todo();
      newTodo.setTitle("test");
      newTodo.setFinished(Boolean.FALSE);
      newTodo.setCreatedAt(LocalDateTime.now());
      newTodo.setDeadline(LocalDate.now().plusDays(7));
      todoMapper.create(newTodo);

      Todo todo = todoMapper.findOne(newTodo.getId());
      dump(todo);

      todo.setTitle(todo.getTitle() + " Edit");
      todo.setFinished(Boolean.TRUE);
      todo.setFinishedAt(LocalDateTime.now());
      if (todoMapper.update(todo)) {
        Todo todoCriteria = new Todo();
        todoCriteria.setTitle("test Edit");
        todoMapper.findPageByCriteria(todoCriteria, new PageRequest(0, 10))
          .forEach(this::dump);
      }

      if (todoMapper.delete(todo.getId())) {
        Optional.ofNullable(todoMapper.findOne(todo.getId()))
          .ifPresent(this::dump);
      }

    };
  }

  private void dump(Todo todo) {
    System.out.println("---dump start----");
    System.out.println(todo.getId());
    System.out.println(todo.getTitle());
    System.out.println(todo.getCreatedAt());
    System.out.println(todo.getDeadline());
    System.out.println(todo.getFinished());
    System.out.println(todo.getFinishedAt());
    System.out.println("---dump end----");
  }
}
