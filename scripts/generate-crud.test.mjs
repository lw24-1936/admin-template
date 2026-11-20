import assert from 'node:assert/strict'
import { execFileSync } from 'node:child_process'
import fs from 'node:fs'
import os from 'node:os'
import path from 'node:path'
import test from 'node:test'
import { fileURLToPath } from 'node:url'

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
const generator = path.join(root, 'scripts', 'generate-crud.mjs')
const spec = path.join(root, 'scripts', 'generate-crud.spec.example.json')

test('generates backend, frontend, migration and manifest', () => {
  const out = fs.mkdtempSync(path.join(os.tmpdir(), 'admintem-crud-'))
  try {
    const stdout = execFileSync(process.execPath, [generator, '--spec', spec, '--out', out], {
      encoding: 'utf8',
    })
    assert.match(stdout, /已生成 11 个文件/)

    const entity = fs.readFileSync(
      path.join(out, 'admintem-backend/src/main/java/com/admintem/web/entity/SysNotice.java'),
      'utf8',
    )
    assert.match(entity, /@TableName\("sys_notice"\)/)
    assert.match(entity, /private String title;/)

    const controller = fs.readFileSync(
      path.join(out, 'admintem-backend/src/main/java/com/admintem/web/controller/NoticeController.java'),
      'utf8',
    )
    assert.match(controller, /@PreAuthorize\("@rbac\.hasPerm\('sysNotice:sysNotice'\)"\)/)

    const page = fs.readFileSync(
      path.join(out, 'admintem-vue/src/views/notice/index.vue'),
      'utf8',
    )
    assert.match(page, /<TableToolbar/)
    assert.match(page, /<ProTable/)
    assert.match(page, /@page-change="onPageChange"/)

    const sql = fs.readFileSync(
      path.join(out, 'admintem-docs/db/changelog/V1.0.16__add_sys_notice.sql'),
      'utf8',
    )
    assert.match(sql, /CREATE TABLE `sys_notice`/)
    assert.match(sql, /sysNotice:add/)

    assert.ok(fs.existsSync(path.join(out, 'GENERATED.md')))
  } finally {
    fs.rmSync(out, { recursive: true, force: true })
  }
})

test('refuses to overwrite by default', () => {
  const out = fs.mkdtempSync(path.join(os.tmpdir(), 'admintem-crud-'))
  try {
    execFileSync(process.execPath, [generator, '--spec', spec, '--out', out], { encoding: 'utf8' })
    assert.throws(
      () => execFileSync(process.execPath, [generator, '--spec', spec, '--out', out], { encoding: 'utf8' }),
      /文件已存在/,
    )
  } finally {
    fs.rmSync(out, { recursive: true, force: true })
  }
})
