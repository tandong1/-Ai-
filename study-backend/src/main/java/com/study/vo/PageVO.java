package com.study.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * 分页响应VO
 */
@Data
public class PageVO<T> implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 当前页数据
     */
    private List<T> list;

    /**
     * 总记录数
     */
    private Long total;

    /**
     * 当前页码，从1开始
     */
    private Integer page;

    /**
     * 每页条数
     */
    private Integer pageSize;

    /**
     * 是否还有下一页
     */
    private Boolean hasMore;

    public PageVO() {
    }

    /**
     * 构建分页结果
     */
    public static <T> PageVO<T> of(List<T> list, long total, int page, int pageSize) {
        PageVO<T> vo = new PageVO<>();
        vo.setList(list);
        vo.setTotal(total);
        vo.setPage(page);
        vo.setPageSize(pageSize);
        vo.setHasMore((long) page * pageSize < total);
        return vo;
    }

    /**
     * 空分页结果
     */
    public static <T> PageVO<T> empty(int page, int pageSize) {
        return of(Collections.emptyList(), 0L, page, pageSize);
    }
}
