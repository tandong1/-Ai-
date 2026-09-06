package com.study.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.study.entity.Gift;
import com.study.mapper.GiftMapper;
import com.study.service.GiftService;
import com.study.vo.GiftVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 礼物服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GiftServiceImpl implements GiftService {

    private final GiftMapper giftMapper;

    @Override
    public List<GiftVO> getAvailableGifts() {
        log.info("获取可用礼物列表");

        LambdaQueryWrapper<Gift> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Gift::getIsActive, true)
               .orderByAsc(Gift::getSortOrder)
               .orderByAsc(Gift::getPointsRequired);

        List<Gift> gifts = giftMapper.selectList(wrapper);

        return gifts.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    private GiftVO convertToVO(Gift gift) {
        GiftVO vo = new GiftVO();
        vo.setId(gift.getId());
        vo.setName(gift.getName());
        vo.setDescription(gift.getDescription());
        vo.setPoints(gift.getPointsRequired());
        vo.setImages(gift.getImages());
        vo.setStock(gift.getStock());
        return vo;
    }
}
