#!/usr/bin/env node
/**
 * CRUD 脚手架生成器（零依赖）。
 *
 * 用法：
 *   node scripts/generate-crud.mjs --spec scripts/generate-crud.spec.example.json --out tmp/notice
 *
 * 默认只写 --out 指定的目录，不会碰现有源码；确认生成物后再复制到仓库对应位置。
 * 生成的是“可编译的骨架 + 需要人工复核的 SQL/页面”，不是免审代码。
 */
import fs from 'node:fs'
import path from 'node:path'

const TYPES = new Set(['string', 'int', 'long', 'boolean', 'decimal', 'datetime'])
const RESERVED = new Set(['class', 'interface', 'enum', 'record', 'package', 'public', 'private', 'static', 'void'])

function fail(message) {
  console.error(`[generate-crud] ${message}`)
  process.exit(1)
}

function parseArgs(argv) {
  const args = {}
  for (let i = 0; i < argv.length; i += 1) {
    const key = argv[i]
    if (!key.startsWith('--')) continue
    const value = argv[i + 1]
    if (value == null || value.startsWith('--')) fail(`参数 ${key} 缺少值`)
    args[key.slice(2)] = value
    i += 1
  }
  return args
}

function upperFirst(value) {
  return value.charAt(0).toUpperCase() + value.slice(1)
}

function snake(value) {
  return value.replace(/([a-z0-9])([A-Z])/g, '$1_$2').toLowerCase()
}

function readSpec(file) {
  if (!file) fail('缺少 --spec')
  const absolute = path.resolve(file)
  if (!fs.existsSync(absolute)) fail(`spec 不存在: ${absolute}`)
  let spec
  try {
    spec = JSON.parse(fs.readFileSync(absolute, 'utf8'))
  } catch (e) {
    fail(`spec 不是合法 JSON: ${e.message}`)
  }
  return spec
}

function validate(spec) {
  const moduleName = spec.module
  const entity = spec.entity
  const table = spec.table
  if (!/^[a-z][A-Za-z0-9]*$/.test(moduleName || '')) fail('module 必须以小写字母开头，只含字母/数字')
  if (!/^[A-Z][A-Za-z0-9]*$/.test(entity || '')) fail('entity 必须以大写字母开头，只含字母/数字')
  if (!/^[a-z][a-z0-9_]*$/.test(table || '')) fail('table 必须是小写下划线命名')
  if (!/^V[0-9]+\.[0-9]+\.[0-9]+$/.test(spec.version || '')) fail('version 必须形如 V1.0.16')
  if (!Array.isArray(spec.fields) || spec.fields.length === 0) fail('fields 至少一项')
  if (!Number.isInteger(spec.menuParentId) || spec.menuParentId <= 0) fail('menuParentId 必须是正整数')

  const names = new Set()
  for (const field of spec.fields) {
    if (!/^[a-z][A-Za-z0-9]*$/.test(field.name || '') || RESERVED.has(field.name)) {
      fail(`字段名不合法: ${field.name}`)
    }
    if (names.has(field.name)) fail(`字段重复: ${field.name}`)
    names.add(field.name)
    if (!TYPES.has(field.type)) fail(`字段 ${field.name} 的类型必须是 ${[...TYPES].join('/')}`)
    if (!field.label) fail(`字段 ${field.name} 缺少 label`)
    if (field.type === 'string' && field.maxLength != null && (!Number.isInteger(field.maxLength) || field.maxLength < 1 || field.maxLength > 2000)) {
      fail(`字段 ${field.name} 的 maxLength 必须是 1~2000`)
    }
  }
}

function javaType(field) {
  switch (field.type) {
    case 'int':
      return 'Integer'
    case 'long':
      return 'Long'
    case 'boolean':
      return 'Boolean'
    case 'decimal':
      return 'BigDecimal'
    case 'datetime':
      return 'LocalDateTime'
    default:
      return 'String'
  }
}

function sqlType(field) {
  switch (field.type) {
    case 'int':
      return 'int'
    case 'long':
      return 'bigint'
    case 'boolean':
      return 'tinyint(1)'
    case 'decimal':
      return 'decimal(18,2)'
    case 'datetime':
      return 'datetime'
    default:
      return `varchar(${field.maxLength || 255})`
  }
}

function annotation(field) {
  const required = field.required ? 'true' : 'false'
  const list = field.list !== false ? 'true' : 'false'
  return `@Schema(description = "${field.label}", requiredMode = Schema.RequiredMode.${required ? 'REQUIRED' : 'NOT_REQUIRED'}, example = "${field.example || ''}")\n    private ${javaType(field)} ${field.name};`
}

function jacksonType(field) {
  if (field.type === 'datetime') return '@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")'
  return ''
}

function build(spec) {
  const moduleName = spec.module
  const entity = spec.entity
  const base = entity.startsWith('Sys') ? entity.slice(3) : entity
  const table = spec.table
  const prefix = spec.prefix || moduleName
  const pkg = spec.package || 'com.admintem.web'
  const pkgPath = pkg.replaceAll('.', '/')
  const version = spec.version
  const description = spec.description || base
  const fields = spec.fields
  const queryFields = fields.filter((f) => f.query)
  const listFields = fields.filter((f) => f.list !== false)
  const formFields = fields.filter((f) => f.form !== false)
  const fieldDecls = fields.map((f) => `    ${annotation(f)}`).join('\n\n')
  const saveFields = formFields.map((f) => {
    const validators = []
    if (f.required) validators.push('@NotNull(message = "' + f.label + '不能为空")')
    if (f.type === 'string' && f.maxLength) validators.push(`@Size(max = ${f.maxLength}, message = "${f.label}长度不能超过 ${f.maxLength}")`)
    return `    ${validators.join('\n    ')}\n    @Schema(description = "${f.label}")\n    private ${javaType(f)} ${f.name};`
  }).join('\n\n')
  const voFields = fields.map((f) => `    @Schema(description = "${f.label}")\n    private ${javaType(f)} ${f.name};`).join('\n\n')
  const queryFieldsCode = queryFields.map((f) => `    @Schema(description = "${f.label}")\n    private ${javaType(f)} ${f.name};`).join('\n\n')
  const queryConditions = queryFields.map((f) => {
    const getter = 'query.get' + upperFirst(f.name) + '()'
    if (f.type === 'string') return `        wrapper.like(StringUtils.hasText(${getter}), ${entity}::get${upperFirst(f.name)}, ${getter});`
    return `        wrapper.eq(${getter} != null, ${entity}::get${upperFirst(f.name)}, ${getter});`
  }).join('\n')
  const setCreate = formFields.map((f) => `        entity.set${upperFirst(f.name)}(request.get${upperFirst(f.name)}());`).join('\n')
  const setUpdate = formFields.map((f) => `        entity.set${upperFirst(f.name)}(request.get${upperFirst(f.name)}());`).join('\n')
  const toVo = [
    '        vo.setId(entity.getId());',
    ...fields.map((f) => `        vo.set${upperFirst(f.name)}(entity.get${upperFirst(f.name)}());`),
    '        vo.setCreatedBy(entity.getCreatedBy());',
    '        vo.setCreatedTime(entity.getCreatedTime());',
    '        vo.setUpdateBy(entity.getUpdateBy());',
    '        vo.setUpdateTime(entity.getUpdateTime());',
  ].join('\n')

  const backend = path.join(pkgPath, '..')
  const entityPath = `admintem-backend/src/main/java/${pkgPath}/entity/${entity}.java`
  const mapperPath = `admintem-backend/src/main/java/${pkgPath}/mapper/${entity}Mapper.java`
  const pageQueryPath = `admintem-backend/src/main/java/${pkgPath}/entity/dto/${base}PageQuery.java`
  const saveRequestPath = `admintem-backend/src/main/java/${pkgPath}/entity/dto/${base}SaveRequest.java`
  const voPath = `admintem-backend/src/main/java/${pkgPath}/entity/vo/${base}VO.java`
  const servicePath = `admintem-backend/src/main/java/${pkgPath}/service/${base}Service.java`
  const serviceImplPath = `admintem-backend/src/main/java/${pkgPath}/service/impl/${base}ServiceImpl.java`
  const controllerPath = `admintem-backend/src/main/java/${pkgPath}/controller/${base}Controller.java`
  const apiPath = `admintem-vue/src/api/${moduleName}.js`
  const pagePath = `admintem-vue/src/views/${moduleName}/index.vue`
  const sqlPath = `admintem-docs/db/changelog/${version}__add_${table}.sql`
  void backend

  const entityJava = `package ${pkg}.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ${description}实体（${table}）。由 generate-crud 生成，请人工复核。
 */
@Data
@TableName("${table}")
@Schema(description = "${description}")
public class ${entity} implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    @Schema(description = "主键")
    private Long id;

${fieldDecls}

    @Schema(description = "创建人")
    private String createdBy;

    @Schema(description = "创建时间")
    private LocalDateTime createdTime;

    @Schema(description = "更新人")
    private String updateBy;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @JsonIgnore
    @TableLogic(value = "0", delval = "1")
    @Schema(description = "逻辑删除标记", hidden = true)
    private Integer isDeleted;
}
`

  const mapperJava = `package ${pkg}.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import ${pkg}.entity.${entity};
import org.apache.ibatis.annotations.Mapper;

/**
 * ${description} Mapper（${table}）。由 generate-crud 生成。
 */
@Mapper
public interface ${entity}Mapper extends BaseMapper<${entity}> {
}
`

  const pageQueryJava = `package ${pkg}.entity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * ${description}分页查询。由 generate-crud 生成。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "${description}分页查询")
public class ${base}PageQuery extends PageQuery {

${queryFieldsCode || '    // 未配置 query 字段，可按需补充'}
}
`

  const saveRequestJava = `package ${pkg}.entity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ${description}新增/编辑入参。由 generate-crud 生成。
 */
@Data
@Schema(description = "${description}保存入参")
public class ${base}SaveRequest {

    @Schema(description = "主键（编辑时必填）")
    private Long id;

${saveFields}
}
`

  const voJava = `package ${pkg}.entity.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ${description}展示对象。由 generate-crud 生成。
 */
@Data
@Schema(description = "${description}")
public class ${base}VO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主键")
    private Long id;

${voFields}

    @Schema(description = "创建人")
    private String createdBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private LocalDateTime createdTime;

    @Schema(description = "更新人")
    private String updateBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
`

  const serviceJava = `package ${pkg}.service;

import ${pkg}.entity.dto.${base}PageQuery;
import ${pkg}.entity.dto.${base}SaveRequest;
import ${pkg}.entity.vo.${base}VO;
import ${pkg}.entity.vo.PageResult;

/**
 * ${description}服务。由 generate-crud 生成。
 */
public interface ${base}Service {

    PageResult<${base}VO> pageQuery(${base}PageQuery query);

    Long create(${base}SaveRequest request, String operator);

    void update(${base}SaveRequest request, String operator);

    void delete(Long id, String operator);
}
`

  const serviceImplJava = `package ${pkg}.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import ${pkg}.common.ResultCode;
import ${pkg}.entity.${entity};
import ${pkg}.entity.dto.${base}PageQuery;
import ${pkg}.entity.dto.${base}SaveRequest;
import ${pkg}.entity.vo.${base}VO;
import ${pkg}.entity.vo.PageResult;
import ${pkg}.exception.BusinessException;
import ${pkg}.mapper.${entity}Mapper;
import ${pkg}.service.${base}Service;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ${description}服务实现。由 generate-crud 生成，请人工复核业务规则。
 */
@Service
@RequiredArgsConstructor
public class ${base}ServiceImpl implements ${base}Service {

    private final ${entity}Mapper mapper;

    @Override
    public PageResult<${base}VO> pageQuery(${base}PageQuery query) {
        long pageNum = query.getPageNum() == null ? 1L : query.getPageNum();
        long pageSize = query.getPageSize() == null ? 10L : query.getPageSize();
        LambdaQueryWrapper<${entity}> wrapper = new LambdaQueryWrapper<>();
${queryConditions || '        // 未配置 query 字段'}
        wrapper.orderByDesc(${entity}::getId);
        IPage<${entity}> page = mapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        List<${base}VO> list = page.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        return new PageResult<>(list, page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(${base}SaveRequest request, String operator) {
        ${entity} entity = new ${entity}();
${setCreate}
        entity.setCreatedBy(operator);
        entity.setCreatedTime(LocalDateTime.now());
        mapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(${base}SaveRequest request, String operator) {
        if (request.getId() == null || mapper.selectById(request.getId()) == null) {
            throw new BusinessException(ResultCode.DATA_NOT_EXIST, "记录不存在");
        }
        ${entity} entity = new ${entity}();
        entity.setId(request.getId());
${setUpdate}
        entity.setUpdateBy(operator);
        entity.setUpdateTime(LocalDateTime.now());
        mapper.updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id, String operator) {
        if (id == null || mapper.selectById(id) == null) {
            throw new BusinessException(ResultCode.DATA_NOT_EXIST, "记录不存在");
        }
        mapper.deleteById(id);
    }

    private ${base}VO toVO(${entity} entity) {
        ${base}VO vo = new ${base}VO();
${toVo}
        return vo;
    }
}
`

  const controllerJava = `package ${pkg}.controller;

import ${pkg}.aspect.AuditLog;
import ${pkg}.aspect.AuditTargetType;
import ${pkg}.common.Result;
import ${pkg}.entity.dto.${base}PageQuery;
import ${pkg}.entity.dto.${base}SaveRequest;
import ${pkg}.entity.vo.${base}VO;
import ${pkg}.entity.vo.PageResult;
import ${pkg}.security.SecurityUtils;
import ${pkg}.service.${base}Service;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * ${description}控制器。由 generate-crud 生成，请复核权限标识与审计动作。
 */
@RestController
@RequestMapping("/${moduleName}")
@RequiredArgsConstructor
@Tag(name = "${description}")
public class ${base}Controller {

    private final ${base}Service service;

    @PostMapping("/queryPage")
    @PreAuthorize("@rbac.hasPerm('${prefix}:${prefix}')")
    @Operation(summary = "分页查询${description}")
    public Result<PageResult<${base}VO>> queryPage(@Valid @RequestBody ${base}PageQuery query) {
        return Result.ok(service.pageQuery(query));
    }

    @PostMapping("/add")
    @PreAuthorize("@rbac.hasPerm('${prefix}:add')")
    @AuditLog(action = "${moduleName.toUpperCase()}_ADD", name = "新增${description}", target = AuditTargetType.CONFIG)
    @Operation(summary = "新增${description}")
    public Result<Long> add(@Valid @RequestBody ${base}SaveRequest request) {
        return Result.ok(service.create(request, SecurityUtils.currentUserName()));
    }

    @PostMapping("/edit")
    @PreAuthorize("@rbac.hasPerm('${prefix}:edit')")
    @AuditLog(action = "${moduleName.toUpperCase()}_EDIT", name = "编辑${description}", target = AuditTargetType.CONFIG)
    @Operation(summary = "编辑${description}")
    public Result<Void> edit(@Valid @RequestBody ${base}SaveRequest request) {
        service.update(request, SecurityUtils.currentUserName());
        return Result.ok();
    }

    @PostMapping("/deleteById")
    @PreAuthorize("@rbac.hasPerm('${prefix}:delete')")
    @AuditLog(action = "${moduleName.toUpperCase()}_DELETE", name = "删除${description}", target = AuditTargetType.CONFIG)
    @Operation(summary = "删除${description}")
    public Result<Void> delete(@Valid @RequestBody ${base}SaveRequest request) {
        service.delete(request.getId(), SecurityUtils.currentUserName());
        return Result.ok();
    }
}
`

  const apiJs = `/**
 * ${description}接口（generate-crud 生成）。
 */
import request from '@/utils/request'

export function query${base}Page(data) {
  return request({ url: '/${moduleName}/queryPage', method: 'post', data })
}

export function add${base}(data) {
  return request({ url: '/${moduleName}/add', method: 'post', data })
}

export function edit${base}(data) {
  return request({ url: '/${moduleName}/edit', method: 'post', data })
}

export function delete${base}(id) {
  return request({ url: '/${moduleName}/deleteById', method: 'post', data: { id } })
}
`

  const columns = listFields.map((f) => {
    if (f.type === 'int' || f.type === 'boolean') {
      return `        <el-table-column prop="${f.name}" label="${f.label}" width="110" />`
    }
    return `        <el-table-column prop="${f.name}" label="${f.label}" min-width="140" show-overflow-tooltip />`
  }).join('\n')
  const formItems = formFields.map((f) => {
    if (f.type === 'string') {
      return `        <el-form-item label="${f.label}" prop="${f.name}">
          <el-input v-model="form.${f.name}" ${f.textarea ? 'type="textarea" :rows="4"' : ''} maxlength="${f.maxLength || 255}" show-word-limit />
        </el-form-item>`
    }
    if (f.type === 'int' || f.type === 'long') {
      return `        <el-form-item label="${f.label}" prop="${f.name}">
          <el-input-number v-model="form.${f.name}" :min="0" controls-position="right" />
        </el-form-item>`
    }
    if (f.type === 'boolean') {
      return `        <el-form-item label="${f.label}" prop="${f.name}">
          <el-switch v-model="form.${f.name}" />
        </el-form-item>`
    }
    return `        <el-form-item label="${f.label}" prop="${f.name}">
          <el-input v-model="form.${f.name}" />
        </el-form-item>`
  }).join('\n')
  const queryItems = queryFields.map((f) => {
    if (f.type === 'int' || f.type === 'long') {
      return `      <el-form-item label="${f.label}">
        <el-input-number v-model="query.${f.name}" :min="0" controls-position="right" />
      </el-form-item>`
    }
    return `      <el-form-item label="${f.label}">
        <el-input v-model="query.${f.name}" clearable @keyup.enter="handleSearch" />
      </el-form-item>`
  }).join('\n')
  const resetForm = formFields.map((f) => `  form.${f.name} = ${f.type === 'string' ? "''" : f.type === 'boolean' ? 'false' : 'null'}`).join('\n')
  const assignForm = formFields.map((f) => `    form.${f.name} = row.${f.name} ?? ${f.type === 'string' ? "''" : f.type === 'boolean' ? 'false' : 'null'}`).join('\n')

  const pageVue = `<template>
  <div class="page">
    <TableToolbar kicker="${moduleName.toUpperCase()} · CRUD" title="${description}">
      <el-form :inline="true" :model="query" @submit.prevent="handleSearch">
${queryItems}
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
      <template #actions>
        <el-button v-perm="'${prefix}:add'" type="primary" @click="openCreate">新增</el-button>
      </template>
    </TableToolbar>

    <ProTable
      :data="list"
      :loading="loading"
      :total="total"
      :page="query.pageNum"
      :size="query.pageSize"
      @page-change="onPageChange"
    >
      <el-table-column prop="id" label="ID" width="90" />
${columns}
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button v-perm="'${prefix}:edit'" link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button v-perm="'${prefix}:delete'" link type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </ProTable>

    <FormDialog v-model="dialogVisible" :title="dialogTitle" :loading="saving" @confirm="handleSubmit">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="96px">
${formItems}
      </el-form>
    </FormDialog>
  </div>
</template>
`

  const columnsSql = fields.map((f) => `    \`${snake(f.name)}\` ${sqlType(f)} ${f.required ? 'NOT NULL' : 'NULL'} COMMENT '${f.label}',`).join('\n')
  const sql = `-- ${version} 生成 ${description}（${table}）
-- 注意：执行前人工复核表名、字段、权限标识、菜单父ID；生成的菜单默认不自动授权给角色。
CREATE TABLE \`${table}\` (
    \`id\` bigint NOT NULL AUTO_INCREMENT,
${columnsSql}
    \`created_by\` varchar(32) DEFAULT NULL,
    \`created_time\` datetime DEFAULT NULL,
    \`update_by\` varchar(32) DEFAULT NULL,
    \`update_time\` datetime DEFAULT NULL,
    \`is_deleted\` tinyint NOT NULL DEFAULT 0,
    PRIMARY KEY (\`id\`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='${description}';

INSERT INTO \`sys_auth_permission\`
    (\`name\`, \`parent_id\`, \`order_num\`, \`type\`, \`menu_url\`, \`component\`, \`status\`, \`show\`, \`permission_key\`, \`created_time\`, \`update_time\`, \`is_deleted\`)
VALUES
    ('${description}', ${spec.menuParentId}, 99, 0, '/${moduleName}', '${moduleName}/index', 0, 0, '${prefix}:${prefix}', NOW(), NOW(), 0);

SET @menu_id = LAST_INSERT_ID();

INSERT INTO \`sys_auth_permission\`
    (\`name\`, \`parent_id\`, \`order_num\`, \`type\`, \`backend_url\`, \`status\`, \`show\`, \`permission_key\`, \`created_time\`, \`update_time\`, \`is_deleted\`)
VALUES
    ('新增${description}', @menu_id, 1, 1, '/${moduleName}/add', 0, 0, '${prefix}:add', NOW(), NOW(), 0),
    ('编辑${description}', @menu_id, 2, 1, '/${moduleName}/edit', 0, 0, '${prefix}:edit', NOW(), NOW(), 0),
    ('删除${description}', @menu_id, 3, 1, '/${moduleName}/deleteById', 0, 0, '${prefix}:delete', NOW(), NOW(), 0);
`

  return {
    [entityPath]: entityJava,
    [mapperPath]: mapperJava,
    [pageQueryPath]: pageQueryJava,
    [saveRequestPath]: saveRequestJava,
    [voPath]: voJava,
    [servicePath]: serviceJava,
    [serviceImplPath]: serviceImplJava,
    [controllerPath]: controllerJava,
    [apiPath]: apiJs,
    [pagePath]: pageVue,
    [sqlPath]: sql,
  }
}

function main() {
  const args = parseArgs(process.argv.slice(2))
  const spec = readSpec(args.spec)
  validate(spec)
  if (!args.out) fail('缺少 --out（建议先输出到 tmp，不要直接覆盖源码）')
  const out = path.resolve(args.out)
  const files = build(spec)
  const force = args.force === 'true'
  for (const [relative, content] of Object.entries(files)) {
    const target = path.resolve(out, relative)
    if (!target.startsWith(out + path.sep)) fail(`输出路径越界: ${relative}`)
    if (fs.existsSync(target) && !force) fail(`文件已存在，如需覆盖加 --force: ${target}`)
  }
  for (const [relative, content] of Object.entries(files)) {
    const target = path.resolve(out, relative)
    fs.mkdirSync(path.dirname(target), { recursive: true })
    fs.writeFileSync(target, content, 'utf8')
  }
  const manifest = `# 生成清单\n\nspec: ${path.resolve(args.spec)}\n\n` +
    Object.keys(files).map((f) => `- ${f}`).join('\n') + '\n'
  fs.mkdirSync(out, { recursive: true })
  fs.writeFileSync(path.join(out, 'GENERATED.md'), manifest, 'utf8')
  console.log(`[generate-crud] 已生成 ${Object.keys(files).length} 个文件到 ${out}`)
  console.log('[generate-crud] 下一步：人工复核权限标识/菜单父ID/SQL，再把文件移到仓库对应位置')
}

main()
