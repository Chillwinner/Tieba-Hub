package com.aura.mapper;

import com.aura.entity.Comment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

// 评论持久层：CRUD + 按新闻查 + 按父评论查 + 点赞数增量更新
@Mapper
public interface CommentMapper
{
    // 插入评论
    int insert(Comment comment);

    // 按 id 查评论
    Comment findById(@Param("id") Long id);

    // 按新闻 id 查全部评论
    List<Comment> findByNewsId(@Param("newsId") Long newsId);

    // 按父评论 id 查子评论
    List<Comment> findByParentId(@Param("parentId") Long parentId);

    // 更新评论
    int update(Comment comment);

    // 增量更新点赞数
    int updateLikeCount(@Param("id") Long id, @Param("delta") int delta);

    // 按 id 删评论
    int deleteById(@Param("id") Long id);
}
