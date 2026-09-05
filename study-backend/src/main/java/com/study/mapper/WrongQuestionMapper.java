package com.study.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.study.entity.WrongQuestion;
import org.apache.ibatis.annotations.Mapper;

/**
 * 错题本Mapper
 */
@Mapper
public interface WrongQuestionMapper extends BaseMapper<WrongQuestion> {
}
