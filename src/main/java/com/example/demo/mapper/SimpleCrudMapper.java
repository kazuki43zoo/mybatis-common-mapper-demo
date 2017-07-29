package com.example.demo.mapper;

import com.google.common.base.CaseFormat;
import org.apache.ibatis.annotations.DeleteProvider;
import org.apache.ibatis.annotations.InsertProvider;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.SelectProvider;
import org.apache.ibatis.annotations.UpdateProvider;
import org.apache.ibatis.builder.annotation.ProviderContext;
import org.apache.ibatis.jdbc.SQL;
import org.apache.ibatis.session.RowBounds;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

interface SimpleCrudMapper<Entity, Criteria> {

  @InsertProvider(type = CreateSqlProvider.class, method = "sql")
  @Options(useGeneratedKeys = true)
  void create(Entity entity);

  @UpdateProvider(type = UpdateSqlProvider.class, method = "sql")
  boolean update(Entity entity);

  @DeleteProvider(type = DeleteSqlProvider.class, method = "sql")
  boolean delete(Integer id);

  @SelectProvider(type = FindOneSqlProvider.class, method = "sql")
  Entity findOne(Integer id);

  default Page<Entity> findPageByCriteria(Criteria criteria, Pageable pageable) {
    long total = __countByCriteria(criteria);
    if (total == 0) {
      return new PageImpl<>(Collections.emptyList(), pageable, total);
    }
    List<Entity> content = __selectByCriteria(criteria, new RowBounds(pageable.getOffset(), pageable.getPageSize()));
    return new PageImpl<>(content, pageable, total);
  }

  @SelectProvider(type = CountByCriteriaSqlProvider.class, method = "sql")
  long __countByCriteria(Criteria criteria);

  @SelectProvider(type = SelectByCriteriaSqlProvider.class, method = "sql")
  List<Entity> __selectByCriteria(Criteria criteria, RowBounds rowBounds);

  class CreateSqlProvider extends SqlProviderSupport {
    public String sql(ProviderContext context) {
      Class<?> entityType = entityType(context);
      Field[] fields = entityType.getDeclaredFields();
      return new SQL()
        .INSERT_INTO(tableName(entityType))
        .INTO_COLUMNS(columns(fields))
        .INTO_VALUES(Stream.of(fields).map(this::bindParameter).toArray(String[]::new))
        .toString();
    }
  }

  class UpdateSqlProvider extends SqlProviderSupport {
    public String sql(ProviderContext context) {
      Class<?> entityType = entityType(context);
      return new SQL()
        .UPDATE(tableName(entityType))
        .SET(Stream.of(entityType.getDeclaredFields())
          .map(field -> columnName(field) + " = " + bindParameter(field)).toArray(String[]::new))
        .WHERE("id = #{id}")
        .toString();
    }
  }

  class DeleteSqlProvider extends SqlProviderSupport {
    public String sql(ProviderContext context) {
      Class<?> entityType = entityType(context);
      return new SQL()
        .DELETE_FROM(tableName(entityType))
        .WHERE("id = #{id}")
        .toString();
    }
  }

  class FindOneSqlProvider extends SqlProviderSupport {
    public String sql(ProviderContext context) {
      Class<?> entityType = entityType(context);
      return new SQL()
        .SELECT(columns(entityType.getDeclaredFields()))
        .FROM(tableName(entityType))
        .WHERE("id = #{id}")
        .toString();
    }
  }

  class SelectByCriteriaSqlProvider extends SqlProviderSupport {
    private static final Field[] EMPTY_FIELDS = new Field[0];

    public String sql(Object criteria, ProviderContext context) {
      Class<?> entityType = entityType(context);
      return new SQL()
        .SELECT(columns(entityType.getDeclaredFields()))
        .FROM(tableName(entityType))
        .WHERE(Stream.of(criteria == null ? EMPTY_FIELDS : criteria.getClass().getDeclaredFields())
          .filter(field -> value(criteria, field) != null)
          .map(field -> columnName(field) + " = " + bindParameter(field)).toArray(String[]::new))
        .toString();
    }
  }

  class CountByCriteriaSqlProvider extends SqlProviderSupport {
    private static final Field[] EMPTY_FIELDS = new Field[0];

    public String sql(Object criteria, ProviderContext context) {
      Class<?> entityType = entityType(context);
      return new SQL()
        .SELECT("count(*)")
        .FROM(tableName(entityType))
        .WHERE(Stream.of(criteria == null ? EMPTY_FIELDS : criteria.getClass().getDeclaredFields())
          .filter(field -> value(criteria, field) != null)
          .map(field -> columnName(field) + " = " + bindParameter(field)).toArray(String[]::new))
        .toString();
    }
  }


  abstract class SqlProviderSupport {

    protected Class<?> entityType(ProviderContext context) {
      return Stream.of(context.getMapperType().getGenericInterfaces())
        .filter(ParameterizedType.class::isInstance).map(ParameterizedType.class::cast)
        .filter(type -> type.getRawType() == SimpleCrudMapper.class)
        .findFirst()
        .map(type -> type.getActualTypeArguments()[0])
        .filter(Class.class::isInstance).map(Class.class::cast)
        .orElseThrow(() -> new IllegalStateException("The SimpleCrudMapper does not found in " + context.getMapperType().getName() + "."));
    }

    protected String tableName(Class<?> entityType) {
      return entityType.getSimpleName().toLowerCase();
    }

    protected String[] columns(Field[] fields) {
      return Stream.of(fields).map(this::columnName).toArray(String[]::new);
    }

    protected String columnName(Field field) {
      return CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, field.getName());
    }

    protected String bindParameter(Field field) {
      return "#{" + field.getName() + "}";
    }

    protected Object value(Object bean, Field field) {
      try {
        field.setAccessible(true);
        return field.get(bean);
      } catch (IllegalAccessException e) {
        throw new IllegalStateException(e);
      } finally {
        field.setAccessible(false);
      }
    }

  }

}
