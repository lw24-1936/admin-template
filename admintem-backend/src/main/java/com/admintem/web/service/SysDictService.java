package com.admintem.web.service;

import com.admintem.web.entity.dto.DictPageQuery;
import com.admintem.web.entity.dto.DictSaveRequest;
import com.admintem.web.entity.vo.DictVO;
import com.admintem.web.entity.vo.PageResult;

import java.util.List;

public interface SysDictService {

    PageResult<DictVO> pageQuery(DictPageQuery query);

    List<DictVO> items(String dictType);

    Long create(DictSaveRequest request, String operator);

    void update(DictSaveRequest request, String operator);

    void delete(Long id, String operator);
}
