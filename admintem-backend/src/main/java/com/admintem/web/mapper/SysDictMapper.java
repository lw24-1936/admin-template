package com.admintem.web.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.admintem.web.entity.SysDict;
import com.admintem.web.entity.dto.DictPageQuery;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface SysDictMapper {

    IPage<SysDict> selectDictPage(IPage<SysDict> page, @Param("q") DictPageQuery q);

    List<SysDict> selectByType(@Param("dictType") String dictType);

    SysDict selectById(@Param("id") Long id);

    long countByTypeValue(@Param("dictType") String dictType, @Param("dictValue") String dictValue,
                          @Param("excludeId") Long excludeId);

    int insert(SysDict dict);

    int updateSelective(SysDict dict);

    int logicDelete(@Param("id") Long id, @Param("updateBy") String updateBy, @Param("now") LocalDateTime now);
}
