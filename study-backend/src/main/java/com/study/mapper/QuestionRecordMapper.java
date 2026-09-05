package com.study.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.study.entity.QuestionRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 答题记录Mapper
 */
@Mapper
public interface QuestionRecordMapper extends BaseMapper<QuestionRecord> {
}
