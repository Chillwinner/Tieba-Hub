package com.aura.mapper;

import com.aura.entity.UserFollow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

// 关注关系持久层接口
@Mapper
public interface UserFollowMapper
{
    // 插入关注关系
    int insert(UserFollow userFollow);

    // 按用户和作者查询关注关系
    UserFollow findByUserAndAuthor(@Param("userId") Long userId, @Param("authorId") Long authorId);

    // 查某用户关注的全部人
    List<UserFollow> findByUserId(@Param("userId") Long userId);

    // 查某作者的全部粉丝
    List<UserFollow> findByAuthorId(@Param("authorId") Long authorId);

    // 删除关注关系
    int delete(@Param("userId") Long userId, @Param("authorId") Long authorId);
}
