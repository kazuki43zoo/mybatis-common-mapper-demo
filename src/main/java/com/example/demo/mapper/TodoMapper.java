package com.example.demo.mapper;

import com.example.demo.domain.Todo;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TodoMapper extends BasicCrudMapper<Todo, Todo> {
}
