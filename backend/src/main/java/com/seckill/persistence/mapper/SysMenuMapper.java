package com.seckill.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.seckill.domain.entity.SysMenu;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SysMenuMapper extends BaseMapper<SysMenu> {

    @Select("SELECT DISTINCT m.perms FROM sys_menu m " +
            "INNER JOIN sys_role_menu rm ON m.id = rm.menu_id " +
            "INNER JOIN sys_user_role ur ON rm.role_id = ur.role_id " +
            "WHERE ur.user_id = #{userId} AND m.perms IS NOT NULL")
    List<String> selectPermsByUserId(@Param("userId") Long userId);
}
