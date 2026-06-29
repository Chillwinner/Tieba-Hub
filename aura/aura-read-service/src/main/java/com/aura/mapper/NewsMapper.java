package com.aura.mapper;

import com.aura.entity.News;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

// 新闻持久层：CRUD + 按作者查 + 分页 + 点赞数增量更新
@Mapper
public interface NewsMapper
{
    // 插入新闻
    int insert(News news);

    // 按 id 查新闻
    News findById(@Param("id") Long id);

    // 按作者 id 查全部新闻
    List<News> findByAuthorId(@Param("authorId") Long authorId);

    // 查全部新闻
    List<News> findAll();

    // 分页查询
    List<News> findPage(@Param("offset") int offset, @Param("limit") int limit);

    // 更新新闻
    int update(News news);

    // 增量更新点赞数
    int updateLikeCount(@Param("id") Long id, @Param("delta") int delta);

    // 按 id 删新闻
    int deleteById(@Param("id") Long id);
}
