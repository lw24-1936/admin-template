package com.admintem.web.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.admintem.web.common.ResultCode;
import com.admintem.web.entity.SysDict;
import com.admintem.web.entity.dto.DictPageQuery;
import com.admintem.web.entity.dto.DictSaveRequest;
import com.admintem.web.entity.vo.DictVO;
import com.admintem.web.entity.vo.PageResult;
import com.admintem.web.exception.BusinessException;
import com.admintem.web.mapper.SysDictMapper;
import com.admintem.web.service.SysDictService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SysDictServiceImpl implements SysDictService {

    private final SysDictMapper dictMapper;

    @Override
    public PageResult<DictVO> pageQuery(DictPageQuery query) {
        long pageNum = query.getPageNum() == null ? 1L : query.getPageNum();
        long pageSize = query.getPageSize() == null ? 10L : query.getPageSize();
        IPage<SysDict> page = dictMapper.selectDictPage(new Page<>(pageNum, pageSize), query);
        List<DictVO> list = page.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        return new PageResult<>(list, page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public List<DictVO> items(String dictType) {
        return dictMapper.selectByType(dictType).stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public Long create(DictSaveRequest request, String operator) {
        if (dictMapper.countByTypeValue(request.getDictType(), request.getDictValue(), null) > 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "同一字典类型下字典值已存在");
        }
        SysDict dict = new SysDict();
        fill(dict, request);
        dict.setCreatedBy(operator);
        dict.setCreatedTime(LocalDateTime.now());
        try {
            dictMapper.insert(dict);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "同一字典类型下字典值已存在");
        }
        return dict.getId();
    }

    @Override
    @Transactional
    public void update(DictSaveRequest request, String operator) {
        SysDict dict = dictMapper.selectById(request.getId());
        if (dict == null) {
            throw new BusinessException(ResultCode.DATA_NOT_EXIST, "字典项不存在");
        }
        if (dictMapper.countByTypeValue(request.getDictType(), request.getDictValue(), request.getId()) > 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "同一字典类型下字典值已存在");
        }
        fill(dict, request);
        dict.setUpdateBy(operator);
        dict.setUpdateTime(LocalDateTime.now());
        try {
            dictMapper.updateSelective(dict);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "同一字典类型下字典值已存在");
        }
    }

    @Override
    @Transactional
    public void delete(Long id, String operator) {
        if (dictMapper.selectById(id) == null) {
            throw new BusinessException(ResultCode.DATA_NOT_EXIST, "字典项不存在");
        }
        dictMapper.logicDelete(id, operator, LocalDateTime.now());
    }

    private void fill(SysDict dict, DictSaveRequest request) {
        dict.setDictType(request.getDictType());
        dict.setDictLabel(request.getDictLabel());
        dict.setDictValue(request.getDictValue());
        dict.setSortNum(request.getSortNum() == null ? 0 : request.getSortNum());
        dict.setStatus(request.getStatus() == null ? 0 : request.getStatus());
        dict.setRemark(request.getRemark());
    }

    private DictVO toVO(SysDict dict) {
        DictVO vo = new DictVO();
        vo.setId(dict.getId());
        vo.setDictType(dict.getDictType());
        vo.setDictLabel(dict.getDictLabel());
        vo.setDictValue(dict.getDictValue());
        vo.setSortNum(dict.getSortNum());
        vo.setStatus(dict.getStatus());
        vo.setRemark(dict.getRemark());
        vo.setCreatedBy(dict.getCreatedBy());
        vo.setCreatedTime(dict.getCreatedTime());
        vo.setUpdateBy(dict.getUpdateBy());
        vo.setUpdateTime(dict.getUpdateTime());
        return vo;
    }
}
