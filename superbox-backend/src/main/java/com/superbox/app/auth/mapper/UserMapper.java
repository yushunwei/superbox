package com.superbox.app.auth.mapper;

import com.superbox.app.auth.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserMapper {

    @Select("SELECT * FROM sys_user WHERE username = #{username}")
    User findByUsername(String username);

    @Select("SELECT * FROM sys_user WHERE id = #{id}")
    User findById(Long id);

    @Select("SELECT * FROM sys_user WHERE wx_openid = #{openid}")
    User findByWxOpenid(String openid);

    @Update("UPDATE sys_user SET wx_openid = #{wxOpenid}, updated_at = NOW() WHERE id = #{id}")
    int bindWxOpenid(Long id, String wxOpenid);

    @Update("UPDATE sys_user SET preferred_language = #{lang}, updated_at = NOW() WHERE id = #{id}")
    int updateLanguage(Long id, String lang);

    @org.apache.ibatis.annotations.Insert("INSERT INTO sys_user (username, password_hash, display_name, preferred_language) " +
            "VALUES (#{username}, #{passwordHash}, #{displayName}, #{preferredLanguage})")
    @org.apache.ibatis.annotations.Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(User user);
}
