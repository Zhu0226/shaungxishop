package com.seckill.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.seckill.domain.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {
}
