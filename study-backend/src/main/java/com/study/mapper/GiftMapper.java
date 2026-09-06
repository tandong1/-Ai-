package com.study.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.study.entity.Gift;
import org.apache.ibatis.annotations.Mapper;

/**
 * 礼物Mapper
 */
@Mapper
public interface GiftMapper extends BaseMapper<Gift> {
}
